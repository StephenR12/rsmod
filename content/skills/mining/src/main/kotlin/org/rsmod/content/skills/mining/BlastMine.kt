package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class LitDynamite(val owner: Player, val cavity: BoundLocInfo, val drop: CoordGrid)

/** Stored on the physical blasted ore, so dropping, picking up or relogging cannot reroll it. */
internal object BlastedOre {
    private const val TIME_MASK = 0x1ffffff

    fun pack(epoch: Long, first: Int, extra: Int?): Int =
        ((epoch.toInt() and TIME_MASK) shl 6) or (first + 1) or (((extra ?: -1) + 1) shl 3)

    fun fresh(value: Int, epoch: Long): Boolean =
        value and 7 in 1..5 && ((epoch.toInt() - (value ushr 6)) and TIME_MASK) < 180

    fun yields(value: Int) =
        listOf((value and 7) - 1, ((value ushr 3) and 7) - 1).filter { it in 0..4 }
}

public class BlastMine
@Inject
constructor(
    private val locs: LocRepository,
    private val ground: ObjRepository,
    private val controllers: ControllerRepository,
    private val types: LocTypeList,
    private val npcs: NpcTypeList,
    private val items: ObjTypeList,
    private val seqs: SeqTypeList,
    private val rng: GameRandom,
    private val xp: XpModifiers,
    private val celestial: CelestialMiningRing,
    private val shops: Shops,
    private val events: EventBus,
) : PluginScript() {
    private val ores =
        listOf(objs.coal, objs.gold_ore, objs.mithril_ore, objs.adamantite_ore, objs.runite_ore)
    private val rewardXp = listOf(33, 66, 120, 190, 260)
    private val levels = listOf(20, 30, 45, 60, 75)
    private val lit = mutableMapOf<Controller, LitDynamite>()

    private fun now() = Instant.now().epochSecond

    override fun ScriptContext.startup() {
        for (id in 28579..28580) {
            val rock = types.getValue(id)
            val cavity = types.getValue(id + 2)
            val loaded = types.getValue(id + 4)
            onOpLoc1(rock) { excavate(it.loc) }
            onOpLocU(rock, objs.chisel) { excavate(it.loc) }
            onOpLoc1(cavity) { load(it.loc) }
            onOpLocU(cavity, BlastItems.dynamite) { load(it.loc) }
            onOpLoc1(loaded) { light(it.loc) }
            onOpLocU(loaded, objs.tinderbox) { light(it.loc) }
        }
        onAiConTimer(BlastFuse.fuse) {
            lit.remove(controller)?.let(::explode)
            controllers.del(controller)
        }
        onOpObj3(BlastItems.ore) {
            val drop = it.obj
            if (!player.members || !BlastedOre.fresh(drop.vars, now())) {
                if (ground.del(drop)) mes("The blasted ore disintegrates.")
            } else {
                val take = invAdd(inv, BlastItems.ore, vars = drop.vars, autoCommit = false)
                if (take.failure) mes("You need a free inventory slot for the blasted ore.")
                else if (ground.del(drop)) {
                    take.commitAll()
                    player.softTimer(BlastTimers.ore, 1)
                }
            }
        }
        onPlayerSoftTimer(BlastTimers.ore) { expire(player) }
        onPlayerLogin {
            if (BlastItems.ore in player.inv) {
                expire(player)
                if (BlastItems.ore in player.inv) player.softTimer(BlastTimers.ore, 1)
            }
            syncClaim(player)
            if (player.members && inMine(player.coords)) player.ifOpenOverlay(BlastUi.hud, events)
        }
        onArea(BlastAreas.mine) {
            if (player.members && BlastUi.hud !in player.ui)
                player.ifOpenOverlay(BlastUi.hud, events)
        }
        onAreaExit(BlastAreas.mine) {
            if (BlastUi.hud in player.ui) player.ifCloseOverlay(BlastUi.hud, events)
        }
        val sack = types.getValue(28592)
        onOpLoc1(sack) { deposit() }
        onOpLocU(sack, BlastItems.ore) { deposit() }
        onOpNpc3(npcs.getValue(7072)) { collect() }
        onOpNpc1(npcs.getValue(7072)) {
            if (choice2("Collect my cleaned ore.", true, "Tell me about the mine.", false))
                collect()
            else explain()
        }
        onOpNpc1(npcs.getValue(7073)) { explain() }
        for (id in 8546..8547) {
            val merchant = npcs.getValue(id)
            onOpNpc3(merchant) { trade() }
            onOpNpc1(merchant) {
                if (choice2("Buy dynamite.", true, "Claim my diary allowance.", false)) trade()
                else claim()
            }
        }
        onOpNpc4(npcs.getValue(8547)) { claim() }
        for (id in 53248..53249) onOpLoc1(types.getValue(id)) {
            if (!player.members) return@onOpLoc1
            val pos = it.loc.coords
            val side = if (id == 53248) 1 else -1
            // The lower approach and upper platform are on opposite sides of the ladder.
            val (dx, dz) = listOf(1 to 0, 0 to -1, -1 to 0, 0 to 1)[it.loc.angleId]
            telejump(pos.translate(dx * side, dz * side, side))
        }
    }

    private fun inMine(pos: CoordGrid) =
        pos.level <= 1 && pos.x in 1464..1512 && pos.z in 3848..3895

    private fun ProtectedAccess.explain() {
        mes(
            "Excavate a rock face, load dynamite and light the fuse. Put the blasted ore in a sack before it crumbles, then collect your cleaned ore from the operator."
        )
    }

    private fun ProtectedAccess.canWork(): Boolean {
        if (!player.members || player.miningLvl < 43) {
            mes("You need level 43 Mining on a members' world to work here.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.excavate(rock: BoundLocInfo) {
        if (!canWork()) return
        if (BlastBits.ore.any { vars[it] >= 900 }) {
            mes("Collect your cleaned ore before excavating more rock.")
            return
        }
        if (objs.chisel !in inv) {
            mes("You need a chisel to excavate this rock.")
            return
        }
        anim(seqs.getValue(7201))
        delay(3)
        if (objs.chisel !in inv || locs.findExact(rock.coords, types[rock]) == null) return
        locs.change(rock, types.getValue(rock.id + 2), 100)
        statAdvance(stats.mining, 20.0 * xp.get(player, stats.mining))
    }

    private fun ProtectedAccess.load(rock: BoundLocInfo) {
        if (!canWork() || locs.findExact(rock.coords, types[rock]) == null) return
        if (!invDel(inv, BlastItems.dynamite).success) {
            mes("You need a pot of dynamite.")
            return
        }
        locs.change(rock, types.getValue(rock.id + 2), 100)
    }

    private fun ProtectedAccess.light(rock: BoundLocInfo) {
        if (!canWork() || locs.findExact(rock.coords, types[rock]) == null) return
        if (objs.tinderbox !in inv) {
            mes("You need a tinderbox to light the fuse.")
            return
        }
        locs.change(rock, types.getValue(rock.id + 2), 40)
        val fuse = Controller(BlastFuse.fuse, rock.coords)
        lit[fuse] = LitDynamite(player, rock, coords)
        controllers.add(fuse, Int.MAX_VALUE)
        fuse.aiTimer(5)
        statAdvance(stats.firemaking, 50.0 * xp.get(player, stats.firemaking))
        mes("You light the fuse.")
    }

    private fun explode(fuse: LitDynamite) {
        val (owner, cavity, drop) = fuse
        val burning = locs.findExact(cavity.coords, types.getValue(cavity.id + 2)) ?: return
        locs.change(burning, types.getValue(cavity.id + 4), 40)
        if (!owner.isSlotAssigned) return
        if (owner.coords.level == drop.level && owner.coords.chebyshevDistance(cavity.coords) < 2)
            owner.queueHit(1, HitType.Typeless, 15)
        val ore = rollOre(owner)
        val bonus = if (rng.of(3) < 2) rollOre(owner) else null
        ground.add(
            InvObj(BlastItems.ore, vars = BlastedOre.pack(now(), ore, bonus)),
            drop,
            300,
            owner,
            reveal = 300,
        )
    }

    // Boosts at detonation select the ore. The invisible boost only affects weights.
    private fun rollOre(player: Player): Int {
        val skill = player.miningLvl
        val effective = skill + celestial.invisibleBoost(player)
        val elite = player.vars[varbits.kourend_diary_elite_complete] != 0
        val roll = rng.of(1000)
        var threshold = 0
        for (i in 4 downTo 1) {
            if (skill < levels[i]) continue
            val weight =
                when (i) {
                    4 -> 40 + (effective - 75) * 4
                    3 -> 110 + (effective - 60) * 3
                    2 -> 170 + (effective - 45) * 2
                    else -> 210
                }
            threshold += if (elite) weight * 11 / 10 else weight
            if (roll < threshold) return i
        }
        return 0
    }

    private fun expire(player: Player) {
        val time = now()
        var crumbled = false
        for (slot in player.inv.indices) {
            val ore = player.inv[slot] ?: continue
            if (ore.id == BlastItems.ore.id && !BlastedOre.fresh(ore.vars, time)) {
                player.inv[slot] = null
                crumbled = true
            }
        }
        if (crumbled) player.mes("Your blasted ore has disintegrated.")
        if (BlastItems.ore !in player.inv) player.clearSoftTimer(BlastTimers.ore)
    }

    private fun ProtectedAccess.deposit() {
        if (!player.members) return
        expire(player)
        for (slot in inv.indices) {
            val ore = inv[slot] ?: continue
            if (ore.id != BlastItems.ore.id) continue
            val yields = BlastedOre.yields(ore.vars).groupingBy { it }.eachCount()
            if (yields.any { (i, count) -> vars[BlastBits.ore[i]] + count > 900 }) {
                mes("Collect the cleaned ore to make room in the sack.")
                continue
            }
            inv[slot] = null
            for ((i, count) in yields) vars[BlastBits.ore[i]] += count
        }
    }

    private fun ProtectedAccess.collect() {
        if (!player.members) return
        var xpEarned = 0
        for ((i, ore) in ores.withIndex()) {
            val count = vars[BlastBits.ore[i]]
            if (count > 0 && invAdd(inv, ore, count, cert = true).success) {
                vars[BlastBits.ore[i]] = 0
                xpEarned += rewardXp[i] * count
            }
        }
        if (xpEarned > 0) statAdvance(stats.mining, xpEarned * xp.get(player, stats.mining))
        else if (BlastBits.ore.any { vars[it] > 0 }) mes("Make room for your noted ore.")
        else mes("There is no cleaned ore to collect.")
    }

    private fun ProtectedAccess.trade() {
        if (player.members)
            shops.open(
                player,
                "Thirus Urkar's Fine Dynamite Store",
                BlastInv.shop,
                5.0,
                1150.0,
                1.0,
            )
    }

    private fun syncClaim(p: Player) {
        VarPlayerIntMapSetter.set(
            p,
            BlastBits.claimed,
            if (p.vars[BlastVars.claimDay] == (now() / 86400).toInt()) 1 else 0,
        )
    }

    private fun ProtectedAccess.claim() {
        if (!player.members) return
        val count =
            when {
                vars[varbits.kourend_diary_elite_complete] != 0 -> 80
                vars[varbits.kourend_diary_hard_complete] != 0 -> 40
                vars[varbits.kourend_diary_medium_complete] != 0 -> 20
                else -> 0
            }
        syncClaim(player)
        if (count == 0) {
            mes("Complete the medium Kourend & Kebos Diary to claim an allowance.")
            return
        }
        if (vars[BlastBits.claimed] != 0) {
            mes("You have already claimed today's dynamite.")
            return
        }
        if (!invAdd(inv, BlastItems.dynamite, count, cert = true).success) {
            mes("Make room for the dynamite.")
            return
        }
        vars[BlastVars.claimDay] = (now() / 86400).toInt()
        vars[BlastBits.claimed] = 1
        mes("Thirus gives you $count dynamite.")
    }
}
