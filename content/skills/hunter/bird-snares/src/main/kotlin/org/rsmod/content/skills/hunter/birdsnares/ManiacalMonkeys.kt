package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.invtx.*
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.player.events.HunterCapture
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ManiacalMonkeys
@Inject
constructor(
    private val locs: LocRepository,
    private val controllers: ControllerRepository,
    private val limits: PortableHunterTraps,
    private val items: ObjTypeList,
    private val npcs: NpcTypeList,
) : PluginScript() {
    private data class Trap(
        val source: BoundLocInfo,
        val con: Controller,
        val owner: Player,
        var phase: Int = 0,
        var variant: Int = 0,
    )

    private val traps = mutableMapOf<CoordGrid, Trap>()
    private val regrowth = WeakHashMap<Npc, Int>()
    private var Controller.ownerSlot: Int by intVarCon(DeadfallVarCons.owner)

    override fun ScriptContext.startup() {
        limits.register(ManiacalController.trap, DeadfallVarCons.owner)
        onOpNpc1(ManiacalNpcs.zooknock) {
            if (!completed()) return@onOpNpc1
            if (choice2("Can you replace my large ninja greegree?", true, "No thanks.", false)) {
                if (invAdd(inv, ManiacalObjs.greegree, 1).success)
                    mes("Zooknock gives you a Kruk monkey greegree.")
            }
        }
        onPlayerLogin {
            if (
                player.vars[ManiacalQuest.progress] >= 195 &&
                    player.vars[ManiacalQuest.tracking] == 0
            )
                org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(
                    player,
                    ManiacalQuest.tracking,
                    1,
                )
        }
        onOpLoc1(ManiacalLocs.grass) { if (completed()) telejump(CoordGrid(2509, 9173, 1)) }
        onOpLoc1(ManiacalLocs.ladder) { telejump(CoordGrid(2714, 2787)) }
        onOpLoc1(ManiacalLocs.southBars) { if (asKruk()) telejump(CoordGrid(2504, 9204, 1)) }
        onOpLoc1(ManiacalLocs.northBars) { if (asKruk()) telejump(CoordGrid(2504, 9189, 1)) }
        onOpLoc1(ManiacalLocs.passage) { if (asKruk()) telejump(CoordGrid(2911, 9095)) }
        onOpLoc1(ManiacalLocs.exit) {
            if (!laboratory(coords)) return@onOpLoc1
            if (player.transmog?.id == ManiacalNpcs.mounted.id)
                mes("Use a holding area to dismount your gorilla first.")
            else telejump(CoordGrid(2509, 9213, 1))
        }
        onOpNpc1(ManiacalNpcs.mount) {
            if (!laboratory(coords) || !asKruk()) return@onOpNpc1
            transmog(ManiacalNpcs.mounted)
            timer(ManiacalTimer.form, 1)
            mes("You climb onto the gorilla's back.")
        }
        onOpLoc1(ManiacalLocs.dismount) {
            if (laboratory(coords) && player.transmog?.id == ManiacalNpcs.mounted.id) {
                transmog(ManiacalNpcs.kruk)
                player.clearTimer(ManiacalTimer.form)
                mes("You climb down from the gorilla.")
            }
        }
        onPlayerTimer(ManiacalTimer.form) {
            if (!laboratory(coords) || ManiacalObjs.greegree !in worn) {
                if (player.transmog?.id == ManiacalNpcs.mounted.id) resetTransmog()
                player.clearTimer(ManiacalTimer.form)
            }
        }
        onOpLoc1(ManiacalLocs.unset) { setTrap(it.loc) }
        onOpLoc1(ManiacalLocs.baited) {
            traps[it.loc.coords]?.takeIf { t -> t.owner === player }?.let { t -> remove(t) }
        }
        onNpcAccessEvent<AiLocEvents.Op1>(ManiacalLocs.baited.id) {
            val t = traps[it.loc.coords]
            if (
                t == null ||
                    t.phase != 1 ||
                    npc.id != ManiacalNpcs.monkey.id ||
                    regrowth.containsKey(npc)
            ) {
                npc.resetMode()
                return@onNpcAccessEvent
            }
            val level = t.owner.stat(stats.hunter).coerceIn(1, 99)
            val chance = -79 + (level - 1) * 306 / 98
            val caught = random.of(256) <= chance
            t.phase = if (caught) 2 else 4
            t.variant = random.of(2)
            locs.change(
                t.source,
                if (caught) ManiacalLocs.catching[t.variant] else ManiacalLocs.failed,
                Int.MAX_VALUE,
            )
            t.con.aiTimer(3)
            if (caught) {
                regrowth[npc] = 0
                changeType(npcs[ManiacalNpcs.growing[0]], Int.MAX_VALUE)
                npc.timer(ManiacalTimer.form, 12)
            }
            npc.resetMode()
        }
        onNpcTimer(ManiacalTimer.form) {
            val next = (regrowth[npc] ?: return@onNpcTimer) + 1
            if (next == 3) {
                npc.resetTransmog()
                npc.clearTimer(ManiacalTimer.form)
                regrowth.remove(npc)
            } else {
                regrowth[npc] = next
                changeType(npcs[ManiacalNpcs.growing[next]], Int.MAX_VALUE)
            }
        }
        onAiConTimer(ManiacalController.trap) {
            val t =
                traps[controller.coords]?.takeIf { it.con === controller } ?: return@onAiConTimer
            when (t.phase) {
                0 -> {
                    t.phase = 1
                    locs.change(t.source, ManiacalLocs.baited, Int.MAX_VALUE)
                    controller.aiTimer(150)
                }
                2 -> {
                    t.phase = 3
                    locs.change(t.source, ManiacalLocs.full[t.variant], Int.MAX_VALUE)
                    controller.aiTimer(150)
                }
                else -> remove(t, true)
            }
        }
        for (full in ManiacalLocs.full) onOpLoc1(full) {
            val t = traps[it.loc.coords] ?: return@onOpLoc1
            if (t.owner !== player || t.phase != 3 || !riding()) return@onOpLoc1
            if (inv.isFull()) {
                mes("You need an empty inventory slot to take the tail.")
                return@onOpLoc1
            }
            val tail = items.getValue(if (random.of(5000) == 0) 19610 else 19665)
            if (invAdd(inv, tail, 1).failure) return@onOpLoc1
            statAdvance(stats.hunter, 1000.0)
            publish(HunterCapture(player, ManiacalNpcs.monkey.internalNameValue))
            mes("You take a monkey tail from the trap.")
            remove(t)
        }
        onOpHeldU(objs.knife, items.getValue(19665)) { e ->
            if (!player.members || stat(stats.crafting) < 10) {
                mes("You need level 10 Crafting to make sinew.")
                return@onOpHeldU
            }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, 19665, 1, e.secondSlot)
                    add(bag, 9436, 1)
                }
            if (tx.success) statAdvance(stats.crafting, 15.0)
        }
        onPlayerLogout {
            traps.values.filter { it.owner === player }.toList().forEach { remove(it) }
        }
    }

    private fun ProtectedAccess.setTrap(loc: BoundLocInfo) {
        if (!laboratory(coords) || !riding()) return
        if (stat(stats.hunter) < 60) {
            mes("You need level 60 Hunter to set this trap.")
            return
        }
        if (
            loc.coords in traps ||
                traps.values.any { it.owner === player } ||
                limits.count(player) >= minOf(5, stat(stats.hunter) / 20 + 1)
        ) {
            mes("You already have the maximum number of traps.")
            return
        }
        if (invDel(inv, objs.banana, 1).failure) {
            mes("You need a banana to bait the trap.")
            return
        }
        val controller = Controller(ManiacalController.trap, loc.coords)
        controller.ownerSlot = player.slotId
        controllers.add(controller, Int.MAX_VALUE)
        traps[loc.coords] = Trap(loc, controller, player)
        locs.change(loc, ManiacalLocs.setting, Int.MAX_VALUE)
        controller.aiTimer(3)
    }

    private fun remove(t: Trap, deferred: Boolean = false) {
        traps.remove(t.source.coords)
        locs.change(t.source, ManiacalLocs.unset, Int.MAX_VALUE)
        if (deferred) t.con.duration(0) else controllers.del(t.con)
    }

    private fun ProtectedAccess.completed(): Boolean {
        if (player.members && vars[ManiacalQuest.progress] >= 195) return true
        mes("You need to complete Monkey Madness II first.")
        return false
    }

    private fun ProtectedAccess.asKruk(): Boolean {
        if (!completed()) return false
        if (ManiacalObjs.greegree in worn) return true
        mes("You need to wield Kruk's monkey greegree.")
        return false
    }

    private fun ProtectedAccess.riding(): Boolean {
        if (!asKruk()) return false
        if (player.transmog?.id == ManiacalNpcs.mounted.id) return true
        mes("You need to be riding a stunted demonic gorilla.")
        return false
    }

    private fun laboratory(tile: CoordGrid): Boolean = tile.x in 2880..2943 && tile.z in 9088..9151
}
