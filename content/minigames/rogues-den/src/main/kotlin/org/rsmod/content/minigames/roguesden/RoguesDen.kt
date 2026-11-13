package org.rsmod.content.minigames.roguesden

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@jakarta.inject.Singleton
class RoguesDen
@Inject
constructor(
    private val types: ObjTypeList,
    private val random: GameRandom,
    private val locations: LocRepository,
    private val npcs: NpcRepository,
    private val rays: RayCastValidator,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private data class Run(
        var last: CoordGrid,
        var agility: Int = 0,
        var thieving: Int = 0,
        var progress: Int = 0,
        var puzzle: BoundLocInfo? = null,
        val disarmed: MutableMap<CoordGrid, Int> = mutableMapOf(),
    )

    private val runs = WeakHashMap<Player, Run>()
    private val blinded = WeakHashMap<Npc, Int>()
    private val start = CoordGrid(3056, 4992, 1)
    private val lobby = CoordGrid(3056, 4990, 1)

    override fun ScriptContext.startup() {
        onOpNpc1(DenNpcs.brian) {
            if (vars[DenVars.intro] == 0) {
                vars[DenVars.intro] = 1
                mes(
                    "Enter the doorway with empty pockets and no equipment. Your mystic jewel will bring you out of the maze."
                )
            } else mes("Make your way past the traps to the wall safes in the centre.")
        }
        onOpLoc1(DenLocs.entrance) { enter(this) }
        onOpHeld1(DenObjs.gem) { finish(player, false) }
        onOpHeld5(DenObjs.gem) { mes("You don't want to lose that in here!") }
        onOpLoc1(DenLocs.safe) { safe(this, it.loc) }
        for (trap in DenLocs.traps) onOpLoc5(trap) { disarm(player, it.loc) }
        onOpLoc1(DenLocs.bars) { bars(this, it.loc) }
        for (ledge in listOf(DenLocs.rail, DenLocs.ledge)) onOpLoc1(ledge) { ledge(this, it.loc) }
        onOpLoc1(DenLocs.passage) { passage(this, it.loc) }
        onOpLoc1(DenLocs.up) {
            if (player in runs) telejump(it.loc.coords.translate(-stepX(it.loc), -stepZ(it.loc), 1))
        }
        onOpLoc1(DenLocs.down) { finish(player, false) }
        onOpLoc2(DenLocs.down) {
            if (player in runs) telejump(it.loc.coords.translate(stepX(it.loc), stepZ(it.loc), -1))
        }
        onOpLoc1(DenLocs.gallery) { door(this, it.loc) }
        onOpLoc1(DenLocs.doors) { door(this, it.loc) }
        onOpLoc1(DenLocs.lock) {
            if (player.thievingLvl >= 80) door(this, it.loc, true)
            else mes("You need level 80 Thieving to pick this lock.")
        }
        onOpLoc1(DenLocs.mosaic) {
            runs[player]?.let { run ->
                run.puzzle = it.loc
                ifOpenMainModal(DenInterfaces.mosaic)
            }
        }
        for ((i, component) in DenComponents.tiles.withIndex()) onIfModalButton(component) {
            val run = runs[player] ?: return@onIfModalButton
            val loc = run.puzzle ?: return@onIfModalButton
            if (
                i == 0 &&
                    player.coords.chebyshevDistance(loc.coords) <= 2 &&
                    invDel(inv, DenObjs.tiles[i], 1).success
            ) {
                run.puzzle = null
                ifClose()
                door(this, loc, true)
            } else mes("That tile doesn't fit the mosaic.")
        }
        for (guard in DenNpcs.guards) {
            onOpNpcU(guard, DenObjs.powder) { blind(this, it.npc) }
            onApNpcU(guard, DenObjs.powder) { if (isWithinApRange(it.npc, 6)) blind(this, it.npc) }
        }
        onPlayerSoftTimer(DenTimer.maze) { pulse(player) }
        onPlayerLogout { if (player in runs) finish(player, false) }
        onPlayerLogin { if (DenObjs.gem in player.inv && player !in runs) recover(player) }
        onEvent<PlayerTeleportEvent.Start> { if (player in runs) finish(player, false, false) }
        onOpHeld1(DenObjs.crate) { choose(this, it.slot, DenObjs.crate, DenObjs.prizes) }
        onOpHeld1(DenObjs.kit) {
            choose(
                this,
                it.slot,
                DenObjs.kit,
                listOf(objs.chisel, objs.rope, objs.tinderbox, objs.spade),
            )
        }
    }

    internal fun enter(access: ProtectedAccess): Boolean =
        with(access) {
            if (player in runs || !player.members || vars[DenVars.intro] == 0) {
                mes("Speak to Brian O'Richard before entering.")
                return false
            }
            if (player.statBase(stats.agility) < 50 || player.statBase(stats.thieving) < 50) {
                mes("You need level 50 Agility and Thieving.")
                return false
            }
            if (vars[DenBits.follower] != 0) {
                mes("Leave your follower outside the maze first.")
                return false
            }
            if (inv.any { it != null } || worn.any { it != null }) {
                mes("Bank everything you are carrying or wearing first.")
                return false
            }
            if (invAdd(inv, DenObjs.gem).failure) return false
            statRestore(stats.agility)
            statRestore(stats.thieving)
            statSub(stats.prayer, 255, 0)
            runs[player] = Run(start)
            telejump(start)
            softTimer(DenTimer.maze, 1)
            true
        }

    private fun blind(a: ProtectedAccess, npc: Npc): Unit =
        with(a) {
            if (
                player in runs &&
                    player.coords.chebyshevDistance(npc.coords) <= 6 &&
                    invDel(inv, DenObjs.powder, 1).success
            ) {
                blinded[npc] = player.currentMapClock + 10
                npc.abortRoute()
                npc.spotanim(DenSpots.stun)
                mes("The guard is temporarily blinded.")
            }
        }

    private fun drain(player: Player, stat: StatType, amount: Int): Boolean {
        val run = runs[player] ?: return false
        if (player.stat(stat) < amount) {
            finish(player, false)
            return false
        }
        player.statSub(stat, amount, 0)
        if (stat == stats.agility) run.agility += amount else run.thieving += amount
        return true
    }

    internal fun disarm(player: Player, loc: BoundLocInfo): Boolean {
        val run = runs[player] ?: return false
        if (!drain(player, stats.thieving, 1)) return false
        run.disarmed[loc.coords] = player.currentMapClock + 6
        player.anim(DenSeqs.disarm)
        return true
    }

    private fun stepX(loc: BoundLocInfo) = listOf(-1, 0, 1, 0)[loc.angleId]

    private fun stepZ(loc: BoundLocInfo) = listOf(0, 1, 0, -1)[loc.angleId]

    private fun door(a: ProtectedAccess, l: BoundLocInfo, twoWay: Boolean = false): Unit =
        with(a) {
            if (player !in runs) return
            val other = l.coords.translate(stepX(l), stepZ(l))
            val onOtherSide =
                (player.coords.x - l.x) * stepX(l) + (player.coords.z - l.z) * stepZ(l) > 0
            if (!twoWay && onOtherSide) {
                mes("The grill will only open one way.")
                return
            }
            telejump(if (onOtherSide) l.coords else other)
        }

    private fun passage(a: ProtectedAccess, l: BoundLocInfo): Unit =
        with(a) { if (player in runs) telejump(l.coords.translate(-3 * stepX(l), -3 * stepZ(l))) }

    private fun bars(a: ProtectedAccess, l: BoundLocInfo): Unit =
        with(a) {
            if (!drain(player, stats.agility, 5)) return
            anim(DenSeqs.bars)
            val dest =
                if (l.angleId % 2 == 1)
                    CoordGrid(if (player.coords.x > l.x) l.x - 1 else l.x + 1, l.z, 1)
                else CoordGrid(l.x, if (player.coords.z > l.z) l.z - 1 else l.z + 1, 1)
            telejump(dest)
        }

    private fun ledge(a: ProtectedAccess, l: BoundLocInfo): Unit =
        with(a) {
            if (l.id == DenLocs.rail.id) {
                if (drain(player, stats.agility, 4))
                    telejump(CoordGrid(2958, if (player.coords.z <= 5032) 5035 else 5030, 1))
                return
            }
            val long = l.z >= 5087
            val west = if (long) 2982 else 2988
            val east = if (long) 2991 else 2994
            if (drain(player, stats.agility, if (long) 7 else 4))
                telejump(
                    CoordGrid(if (player.coords.x <= (west + east) / 2) east else west, l.z, 1)
                )
        }

    internal fun pulse(p: Player) {
        val r = runs[p] ?: return
        val c = p.coords
        if (c.level !in 1..2 || c.x !in 2944..3071 || c.z !in 4988..5119 || DenObjs.gem !in p.inv) {
            finish(p, false)
            return
        }
        // Drained skill levels stay fixed even when a normal world stat timer fires during the run.
        for ((s, spent) in listOf(stats.agility to r.agility, stats.thieving to r.thieving)) {
            val extra = p.stat(s) - (p.statBase(s) - spent)
            if (extra > 0) p.statSub(s, extra, 0)
        }
        val now = p.currentMapClock
        if (
            c.level == 1 &&
                npcs.findAll(ZoneKey.from(c), 1).any { n ->
                    DenNpcs.guards.any { it.id == n.id } &&
                        (blinded[n] ?: Int.MIN_VALUE) <= now &&
                        n.coords.chebyshevDistance(c) <= 2 &&
                        rays.hasLineOfSight(n.coords, c)
                }
        ) {
            finish(p, false)
            return
        }
        if (c == r.last) return
        r.last = c
        if (c.level != 1) return
        val stage =
            when {
                c.z >= 5044 && c.x in 3008..3029 -> 5
                c.z >= 5050 -> 4
                c.x <= 2975 -> 3
                c.x <= 3007 -> 2
                c.x <= 3035 -> 1
                else -> 0
            }
        r.progress = maxOf(r.progress, stage)
        r.disarmed.entries.removeIf { it.value < now }
        val traps = locations.findAll(c).toList()
        if (c !in r.disarmed && traps.any { it.id in setOf(7227, 7230, 7245, 7250, 7224) }) {
            finish(p, false)
            return
        }
        if (traps.any { it.id == 7241 || it.id == 7252 }) {
            if (!drain(p, stats.agility, 3)) return
        }
        for ((x, z) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val tile = c.translate(x, z)
            if (
                tile !in r.disarmed &&
                    locations.findAll(tile).any { loc ->
                        loc.id in setOf(7228, 7229, 7249) &&
                            loc.angleId ==
                                listOf(-1 to 0, 0 to 1, 1 to 0, 0 to -1).indexOf(-x to -z)
                    }
            ) {
                finish(p, false)
                return
            }
        }
    }

    internal fun safe(access: ProtectedAccess, l: BoundLocInfo): Boolean =
        with(access) {
            if (player !in runs || l.x !in 3013..3018 || l.z !in 5045..5050 || l.level != 1)
                return false
            val succeeds =
                if (random.of(200) <= player.thievingLvl) true
                else !random.randomBoolean() && drain(player, stats.thieving, 1)
            anim(DenSeqs.crack)
            if (!succeeds) {
                finish(player, false)
                return false
            }
            val item = if (random.of(8) < 3) DenObjs.kit else DenObjs.crate
            finish(player, true)
            invAdd(inv, item)
            true
        }

    internal fun finish(p: Player, success: Boolean, move: Boolean = true) {
        val run = runs.remove(p) ?: return
        val rate = if (success) 12 else run.progress * 2
        if (run.agility > 0 && rate > 0)
            p.statAdvance(stats.agility, (rate * run.agility).toDouble())
        if (run.thieving > 0 && rate > 0)
            p.statAdvance(stats.thieving, (rate * run.thieving).toDouble())
        recover(p, move)
    }

    private fun recover(p: Player, move: Boolean = true) {
        for (i in p.inv.indices) {
            val held = p.inv[i] ?: continue
            if (
                held.isType(DenObjs.gem) ||
                    held.isType(DenObjs.powder) ||
                    DenObjs.tiles.any { held.isType(it) }
            )
                p.inv[i] = null
        }
        p.statRestoreAll(listOf(stats.agility, stats.thieving, stats.prayer))
        p.clearSoftTimer(DenTimer.maze)
        if (move) PathingEntityCommon.telejump(p, collision, lobby)
    }

    private suspend fun choose(
        a: ProtectedAccess,
        slot: Int,
        source: ObjType,
        choices: List<ObjType>,
    ): Unit =
        with(a) {
            val reward =
                choices.getOrNull(menu("Choose your reward", true, choices.map { types[it].name }))
                    ?: return
            if (!inv[slot].isType(source)) return
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, source.id, 1, slot = slot)
                    add(bag, reward.id, 1)
                }
            if (!tx.success) mes("Make room in your inventory.")
        }
}
