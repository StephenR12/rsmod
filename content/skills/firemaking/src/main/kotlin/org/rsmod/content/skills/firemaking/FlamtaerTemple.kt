package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onZone
import org.rsmod.api.script.advanced.onZoneExit
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TempleController : ControllerBuilder() {
    val temple = build("flamtaer_temple")
}

internal object TempleState : VarpReferences() {
    val repair = find("temple_repaired_p")
    val supplies = find("temple_resources_p")
    val holy = find("temple_sanctity_p")
    val fine = find("flamtaer_sanctity_tenths")
}

internal object TempleFineState : VarpBuilder() {
    init {
        build("flamtaer_sanctity_tenths") { permanent = false }
    }
}

internal object TempleBits : VarBitReferences() {
    val stored = find("temple_resources")
}

internal object TempleUi : InterfaceReferences() {
    val hud = find("flamtaer_status")
}

internal object TempleTimer : TimerReferences() {
    val drain = find("flamtaer_sanctity")
}

internal object TempleQueue : QueueReferences() {
    val build = find("flamtaer_repair")
}

public class FlamtaerTemple
@Inject
constructor(
    private val objects: ObjTypeList,
    private val types: LocTypeList,
    private val seqs: SeqTypeList,
    private val locs: LocRepository,
    private val controllers: ControllerRepository,
    private val npcs: NpcRepository,
    private val events: EventBus,
    private val xp: FiremakingXp,
) : PluginScript() {
    private val altar = CoordGrid(3506, 3316)
    private val area = (437..439).flatMap { x -> (413..415).map { z -> ZoneKey(x, z, 0) } }.toSet()
    private val visitors = mutableSetOf<Player>()
    private var condition = 0
    private var lit = false
    private var clockRunning = false

    override fun ScriptContext.startup() {
        for (zone in area) {
            onZone(zone) { enterArea(player) }
            onZoneExit(zone) { if (ZoneKey.from(player.coords) !in area) leaveArea(player) }
        }
        onPlayerLogin {
            setSanctity(player, 0)
            if (ZoneKey.from(player.coords) in area) enterArea(player)
        }
        onPlayerLogout {
            leaveArea(player)
            setSanctity(player, 0)
        }
        for (id in 4068..4089) onOpLoc1(types.getValue(id)) {
            clearWeakQueue(TempleQueue.build)
            build(it.loc.coords)
        }
        onPlayerQueueWithArgs<CoordGrid>(TempleQueue.build) { build(it.args) }
        onAiConTimer(TempleController.temple) {
            val attackers =
                npcs.findAll(ZoneKey.from(altar), 2).count {
                    it.id in 1276..1277 &&
                        it.coords.chebyshevDistance(altar) <= 10 &&
                        it.hitpoints > 0 &&
                        it.interaction == null
                }
            if (attackers > 0) change(condition - attackers)
            if (condition == 0) {
                controllers.del(controller)
                clockRunning = false
            } else controller.aiTimer(15)
        }
        onPlayerSoftTimer(TempleTimer.drain) {
            setSanctity(player, player.vars[TempleState.fine] - 10)
            if (player.vars[TempleState.fine] > 0) player.softTimer(TempleTimer.drain, 100)
        }
        onEvent<NpcKillEvents.Credited> {
            if (
                player.members &&
                    npc.id in 1276..1277 &&
                    npc.coords.level == 0 &&
                    npc.coords.chebyshevDistance(altar) <= 12
            )
                gainSanctity(player, 20)
        }
        onOpLoc1(types.getValue(4091)) { light(it.loc.coords) }
        onOpLocU(types.getValue(4091), objs.tinderbox) { light(it.loc.coords) }
        onOpLoc1(types.getValue(4090)) {
            if (it.loc.coords == altar) {
                val slot = inv.indexOfFirst { item -> item != null && dose(item.id) != null }
                if (slot < 0) mes("Use some olive oil or Serum 207 on the fire.") else bless(slot)
            }
        }
        for (id in listOf(3422, 3424, 3426, 3428, 3408, 3410, 3412, 3414)) onOpLocU(
            types.getValue(4090),
            objects.getValue(id),
        ) {
            if (it.loc.coords == altar) bless(it.invSlot)
        }
    }

    private fun enterArea(p: Player) {
        if (!p.members) return
        visitors += p
        set(p, TempleState.repair, condition)
        set(p, TempleState.supplies, p.vars[TempleBits.stored])
        set(p, TempleState.holy, p.vars[TempleState.fine] / 10)
        if (TempleUi.hud !in p.ui) p.ifOpenOverlay(TempleUi.hud, events)
    }

    private fun leaveArea(p: Player) {
        visitors.remove(p)
        if (TempleUi.hud in p.ui) p.ifCloseOverlay(TempleUi.hud, events)
    }

    private fun change(target: Int) {
        val before = condition
        condition = target.coerceIn(0, 100)
        if (before / 10 != condition / 10) {
            for (x in 3504..3508) for (z in 3314..3318) {
                val wall =
                    locs.findAll(CoordGrid(x, z)).firstOrNull { it.id in 4068..4089 } ?: continue
                val base = if (wall.id < 4079) 4068 else 4079
                locs.change(wall, types.getValue(base + condition / 10), Int.MAX_VALUE)
            }
        }
        if (condition == 100 || condition < 80) {
            if (condition < 80) lit = false
            val id = if (condition < 80) 4092 else if (lit) 4090 else 4091
            val fire = locs.findAll(altar).firstOrNull { it.id in 4090..4092 }
            if (fire != null && fire.id != id) locs.change(fire, types.getValue(id), Int.MAX_VALUE)
        }
        visitors.forEach { set(it, TempleState.repair, condition) }
    }

    private fun ProtectedAccess.build(pos: CoordGrid) {
        if (!player.members || vars[varps.morttonquest] < PyreLogPreparation.MIN_QUEST_STATE) return
        if (stat(stats.crafting) < 20) {
            mes("You need level 20 Crafting to repair the wall.")
            return
        }
        if (
            pos.level != 0 ||
                pos.x !in 3504..3508 ||
                pos.z !in 3314..3318 ||
                player.coords.chebyshevDistance(pos) > 1 ||
                locs.findAll(pos).none { it.id in 4068..4089 }
        )
            return
        val special = objects.getValue(3678) in inv
        val imcando =
            listOf(25644, 29775).any {
                objects.getValue(it) in inv || objects.getValue(it) in player.worn
            }
        if (!special && !imcando && objs.hammer !in inv) {
            mes("You need a hammer to repair the wall.")
            return
        }
        enterArea(player)
        if (vars[TempleBits.stored] == 0) {
            if (!supply()) {
                mes("You need a timber beam or plank, a limestone brick and five swamp paste.")
                return
            }
            vars[TempleBits.stored] = 5
        }
        anim(seqs.getValue(if (imcando) 8915 else 1755))
        val skill = stat(stats.crafting) + if (special) 40 else 0
        if (random.of(160) < skill + 20) {
            val bracelet = player.worn.indexOfFirst { it?.id == 21180 }
            if (bracelet >= 0) {
                val item = requireNotNull(player.worn[bracelet])
                val used = item.vars + 1
                player.worn[bracelet] = if (used >= 80) null else InvObj(objects[item], 1, used)
            }
            vars[TempleBits.stored] -= 1
            set(player, TempleState.supplies, vars[TempleBits.stored])
            gainSanctity(player, if (bracelet >= 0) 88 else 50)
            change(condition + if (bracelet >= 0) 5 else 2)
            statAdvance(stats.crafting, if (special) 15.0 else 10.0)
            if (!clockRunning) {
                val con = Controller(TempleController.temple, altar)
                controllers.add(con, Int.MAX_VALUE)
                con.aiTimer(15)
                clockRunning = true
            }
        }
        weakQueue(TempleQueue.build, 3, pos)
    }

    private fun ProtectedAccess.supply(): Boolean {
        val bagHeld = FlamtaerGoods.bag in inv
        val needed = intArrayOf(1, 1, 5)
        val fromBag =
            needed.indices.map { i ->
                if (bagHeld) minOf(needed[i], vars[FlamtaerBag.values[i]]) else 0
            }
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                for ((i, item) in FlamtaerGoods.materials.withIndex()) {
                    val count = needed[i] - fromBag[i]
                    val input = if (i == 0 && item !in inv) objs.plank else item
                    if (count > 0) delete(bag, input.id, count)
                }
            }
        if (!tx.success) return false
        for ((i, used) in fromBag.withIndex()) if (used > 0) vars[FlamtaerBag.values[i]] -= used
        return true
    }

    private fun setSanctity(p: Player, n: Int) {
        val value = n.coerceIn(0, 1000)
        set(p, TempleState.fine, value)
        set(p, TempleState.holy, value / 10)
    }

    private fun gainSanctity(p: Player, n: Int) {
        if (p.vars[TempleState.fine] == 0) p.softTimer(TempleTimer.drain, 100)
        setSanctity(p, p.vars[TempleState.fine] + n)
    }

    private fun ProtectedAccess.light(pos: CoordGrid) {
        if (
            pos != altar ||
                !player.members ||
                vars[varps.morttonquest] < PyreLogPreparation.MIN_QUEST_STATE ||
                lit ||
                condition < 80
        )
            return
        if (objs.tinderbox !in inv) {
            mes("You need a tinderbox to light the altar.")
            return
        }
        val fire = locs.findExact(altar, types.getValue(4091)) ?: return
        locs.change(fire, types.getValue(4090), Int.MAX_VALUE)
        lit = true
        xp.advance(this, 100.0)
    }

    private fun dose(id: Int): Int? =
        when (id) {
            3422,
            3408 -> 4
            3424,
            3410 -> 3
            3426,
            3412 -> 2
            3428,
            3414 -> 1
            else -> null
        }

    private fun ProtectedAccess.bless(slot: Int) {
        if (
            !player.members || !lit || vars[varps.morttonquest] < PyreLogPreparation.MIN_QUEST_STATE
        )
            return
        val item = inv[slot] ?: return
        val doses = dose(item.id) ?: return
        val oil = item.id >= 3422
        if (!oil && stat(stats.herblore) < 15) {
            mes("You need level 15 Herblore to sanctify the serum.")
            return
        }
        if (vars[TempleState.fine] < if (oil) 100 else 200) {
            mes("You haven't earned enough sanctity.")
            return
        }
        inv[slot] = InvObj(objects.getValue(if (oil) item.id + 8 else 3420 - doses))
        setSanctity(player, vars[TempleState.fine] - doses * (if (oil) 9 else 50))
        mes(if (oil) "You make some sacred oil." else "You make some Serum 208.")
    }
}
