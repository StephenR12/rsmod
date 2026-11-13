package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import net.rsprot.protocol.game.outgoing.interfaces.IfSetModel
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.combat.commons.npc.engagePlayer
import org.rsmod.api.player.HallowedLockpick
import org.rsmod.api.player.events.SpadeDigDestination
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.privateLocChange
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val cryptNames = listOf("ahrim", "dharok", "guthan", "karil", "torag", "verac")

internal object BarrowsBits : VarBitReferences() {
    val kills = cryptNames.map { find("barrows_killed_$it") }
    val potential = find("barrows_killed_monster")
    val count = find("barrows_killed_count")
    val entrance = find("barrows_old_entrance")
    val doors = ('a'..'p').map { find("barrows_door_$it") }
    val corners = listOf("a", "c", "g", "i").map { find("barrows_old_chamber_$it") }
    val ladder = find("barrows_ladder_visible")
    val collapse = find("barrows_old_collapsing")
    val chest = find("barrows_chest_open")
    val puzzle = find("barrows_old_puzzle")
}

internal object BarrowsCounters : org.rsmod.api.type.refs.varp.VarpReferences() {
    val chests = find("total_barrows_chests")
}

internal object TunnelLocs : LocReferences() {
    val stairs = cryptNames.map { find("barrows_stairs_$it") }
    val sarcophagi = cryptNames.map { find("barrow_${it}_sarcophagus") }
    val visibleLadder = find("barrows_ladder")
    val ladders = listOf("a", "c", "g", "i").map { find("barrows_ladder_$it") }
    val pairs =
        ('a'..'p').map { listOf(find("barrows_door_${it}_l"), find("barrows_door_${it}_r")) }
}

internal object BarrowsTimers : org.rsmod.api.type.refs.timer.TimerReferences() {
    val crypt = find("barrows_crypt_watch")
}

internal object TunnelUI : InterfaceReferences() {
    val overlay = find("barrows_overlay")
    val puzzle = find("barrows_puzzle")
}

internal object PuzzleButtons : ComponentReferences() {
    val sequence = (1..3).map { find("barrows_puzzle:$it") }
    val choices = listOf("a", "b", "c").map { find("barrows_puzzle:$it") }
    val pictures = listOf("a", "b", "c").map { find("barrows_puzzle:pic_$it") }
}

@Singleton
public class BarrowsTunnels
@Inject
constructor(
    private val controller: BarrowsController,
    private val random: GameRandom,
    private val hunt: org.rsmod.api.hunt.Hunt,
    private val events: EventBus,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val interactions: AiPlayerInteractions,
) : PluginScript() {
    private val mounds =
        listOf(
            CoordGrid(3566, 3289),
            CoordGrid(3575, 3298),
            CoordGrid(3577, 3283),
            CoordGrid(3566, 3275),
            CoordGrid(3553, 3283),
            CoordGrid(3557, 3298),
        )
    private val stairs =
        listOf(
            CoordGrid(3557, 9703, 3),
            CoordGrid(3556, 9718, 3),
            CoordGrid(3534, 9704, 3),
            CoordGrid(3546, 9684, 3),
            CoordGrid(3566, 9683, 3),
            CoordGrid(3577, 9703, 3),
        )
    private val corners =
        listOf(
            CoordGrid(3534, 9711),
            CoordGrid(3568, 9711),
            CoordGrid(3534, 9677),
            CoordGrid(3568, 9677),
        )
    // Nine rooms in a 3x3 grid, plus the four outer corridors linking the corner rooms.
    private val edges =
        listOf(
            0 to 2,
            0 to 6,
            0 to 3,
            0 to 1,
            1 to 4,
            1 to 2,
            2 to 5,
            2 to 8,
            3 to 4,
            4 to 5,
            3 to 6,
            4 to 7,
            5 to 8,
            6 to 7,
            7 to 8,
            6 to 8,
        )

    private data class Puzzle(val origin: CoordGrid, val destination: CoordGrid, val answer: Int)

    private val puzzles = WeakHashMap<Player, Puzzle>()
    private val opponents = WeakHashMap<Player, Npc>()

    private val drainAt = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (inCrypt(player.coords)) {
                showCrypt(player)
                syncLadder(player)
            }
        }
        onPlayerTimer(BarrowsTimers.crypt) {
            if (!inCrypt(coords)) {
                hideCrypt(player)
                return@onPlayerTimer
            }
            if (mapClock < (drainAt[player] ?: mapClock)) return@onPlayerTimer
            drainAt[player] = mapClock + 30
            if (
                player.lefthand?.id in
                    setOf(25928, 25930, 25932, 25934, 25936, 27550, 27551, 27552, 27553)
            )
                return@onPlayerTimer
            statSub(stats.prayer, 8 + controller.defeatedCount(player), 0)
        }
        onEvent<SpadeDigDestination> {
            if (player.members && tile.level == 0) {
                val i = mounds.indices.firstOrNull { tile.chebyshevDistance(mounds[it]) <= 2 }
                if (i != null) {
                    prepare(player)
                    showCrypt(player)
                    destination = stairs[i]
                }
            }
        }
        for ((i, type) in TunnelLocs.stairs.withIndex()) onOpLoc1(type) {
            dismiss(player)
            hideCrypt(player)
            telejump(mounds[i])
        }
        for ((i, type) in TunnelLocs.sarcophagi.withIndex()) onOpLoc1(type) {
            if (!player.members) return@onOpLoc1
            prepare(player)
            if (vars[BarrowsBits.entrance] == i + 1) {
                if (choice2("Enter the hidden tunnel.", true, "Stay here.", false)) {
                    dismiss(player)
                    val corner = BarrowsBits.corners.indexOfFirst { vars[it] != 0 }.coerceAtLeast(0)
                    syncLadder(player)
                    telejump(corners[corner])
                }
            } else summon(this, i)
        }
        for ((i, type) in TunnelLocs.ladders.withIndex()) onOpLoc1(type) { leaveTunnel(i) }
        onOpLoc1(TunnelLocs.visibleLadder) {
            val i = corners.indexOfFirst { corner -> corner.translate(0, 1) == it.loc.coords }
            if (i >= 0) leaveTunnel(i)
        }
        for ((i, pair) in TunnelLocs.pairs.withIndex()) for (door in pair) {
            onOpLoc1(door) { through(it.loc, i, null) }
            for (pick in listOf(HallowedLockpick.full, HallowedLockpick.used)) onOpLocU(
                door,
                pick,
            ) {
                through(it.loc, i, it.invSlot)
            }
        }
        for ((i, button) in PuzzleButtons.choices.withIndex()) onIfModalButton(button) {
            val puzzle = puzzles.remove(player) ?: return@onIfModalButton
            ifCloseSub(TunnelUI.puzzle)
            if (coords.chebyshevDistance(puzzle.origin) > 1) return@onIfModalButton
            if (i == puzzle.answer) {
                vars[BarrowsBits.puzzle] = 1
                cross(this, puzzle.destination)
            } else {
                layout(player)
                mes("You hear the doors and ladders shifting in the darkness.")
            }
        }
        onIfClose(TunnelUI.puzzle) { puzzles.remove(player) }
        onPlayerLogout {
            puzzles.remove(player)
            drainAt.remove(player)
            dismiss(player)
        }
        for (type in listOf(HallowedLockpick.full, HallowedLockpick.used)) onOpHeld4(type) {
            mes(
                "Your strange old lockpick has ${HallowedLockpick.charges(inv[it.slot])} charges remaining."
            )
        }
    }

    private fun inCrypt(tile: CoordGrid) =
        tile.level in listOf(0, 3) && tile.x in 3520..3583 && tile.z in 9664..9727

    private fun showCrypt(player: Player) {
        player.ifOpenOverlay(TunnelUI.overlay, events)
        player.timer(BarrowsTimers.crypt, 1)
        drainAt[player] = player.currentMapClock + 30
    }

    private fun hideCrypt(player: Player) {
        drainAt.remove(player)
        player.clearTimer(BarrowsTimers.crypt)
        player.ifCloseOverlay(TunnelUI.overlay, events)
    }

    private fun syncLadder(player: Player) {
        // All four native placeholders share one visibility bit. Reveal the physical ladder
        // exclusively in this player's chosen corner and restore the three invisible placeholders.
        VarPlayerIntMapSetter.set(player, BarrowsBits.ladder, 0)
        for ((i, tile) in corners.withIndex()) {
            val type =
                if (player.vars[BarrowsBits.corners[i]] != 0) TunnelLocs.visibleLadder
                else TunnelLocs.ladders[i]
            player.privateLocChange(LocInfo(2, tile.translate(0, 1), LocEntity(type.id, 10, 0)))
        }
    }

    private fun ProtectedAccess.leaveTunnel(i: Int) {
        if (vars[BarrowsBits.corners[i]] == 0) return
        val mound = (vars[BarrowsBits.entrance] - 1).coerceIn(0, 5)
        dismiss(player)
        hideCrypt(player)
        telejump(mounds[mound])
        if (vars[BarrowsBits.collapse] != 0) reset(player)
    }

    private fun prepare(player: Player) {
        if (player.vars[BarrowsBits.entrance] in 1..6 && player.vars[BarrowsBits.collapse] == 0)
            return
        reset(player)
        VarPlayerIntMapSetter.set(player, BarrowsBits.entrance, random.of(6) + 1)
        layout(player)
    }

    private fun reset(player: Player) {
        for (bit in
            BarrowsBits.kills +
                BarrowsBits.corners +
                BarrowsBits.doors +
                listOf(
                    BarrowsBits.potential,
                    BarrowsBits.count,
                    BarrowsBits.entrance,
                    BarrowsBits.ladder,
                    BarrowsBits.chest,
                    BarrowsBits.collapse,
                    BarrowsBits.puzzle,
                )) VarPlayerIntMapSetter.set(player, bit, 0)
    }

    private fun layout(player: Player) {
        val room = IntArray(9) { it }
        fun root(n: Int): Int {
            var node = n
            while (node != room[node]) node = room[node]
            return node
        }
        val ordering = edges.indices.toMutableList()
        for (i in ordering.lastIndex downTo 1) {
            val j = random.of(i + 1)
            val temp = ordering[i]
            ordering[i] = ordering[j]
            ordering[j] = temp
        }
        var centerConnected = false
        for (i in ordering) {
            val (a, b) = edges[i]
            val ra = root(a)
            val rb = root(b)
            val center = a == 4 || b == 4
            val open = ra != rb && (!center || !centerConnected)
            if (open) {
                room[ra] = rb
                if (center) centerConnected = true
            }
            // Native form zero is the passable door and form one is the locked door.
            VarPlayerIntMapSetter.set(player, BarrowsBits.doors[i], if (open) 0 else 1)
        }
        val landing = random.of(4)
        for ((i, bit) in BarrowsBits.corners.withIndex()) VarPlayerIntMapSetter.set(
            player,
            bit,
            if (i == landing) 1 else 0,
        )
        VarPlayerIntMapSetter.set(player, BarrowsBits.puzzle, 0)
        syncLadder(player)
    }

    private suspend fun ProtectedAccess.through(loc: BoundLocInfo, i: Int, pick: Int?) {
        if (!player.members || vars[BarrowsBits.entrance] !in 1..6 || coords.level != 0) return
        val dest =
            when (loc.angleId) {
                0 -> CoordGrid(if (coords.x < loc.x) loc.x else loc.x - 1, loc.z)
                2 -> CoordGrid(if (coords.x > loc.x) loc.x else loc.x + 1, loc.z)
                1 -> CoordGrid(loc.x, if (coords.z > loc.z) loc.z else loc.z + 1)
                else -> CoordGrid(loc.x, if (coords.z < loc.z) loc.z else loc.z - 1)
            }
        val locked = vars[BarrowsBits.doors[i]] != 0
        if (locked) {
            if (pick == null) {
                mes("The door is locked.")
                return
            }
            if (!HallowedLockpick.consume(inv, pick)) return
            cross(this, dest)
            return
        }
        val enteringCenter = dest.x in 3546..3557 && dest.z in 9689..9700
        if (
            enteringCenter &&
                (edges[i].first == 4 || edges[i].second == 4) &&
                vars[BarrowsBits.puzzle] == 0 &&
                vars[BarrowsBits.collapse] == 0
        ) {
            val answer = random.of(3)
            val model = 6713 + 6 * random.of(4)
            puzzles[player] = Puzzle(coords, dest, answer)
            ifOpenMainModal(TunnelUI.puzzle)
            for (k in 0..2) {
                player.client.write(IfSetModel(PuzzleButtons.sequence[k].packed, model + 3 + k))
                player.client.write(
                    IfSetModel(PuzzleButtons.pictures[k].packed, model + (k - answer + 3) % 3)
                )
                ifSetEvents(PuzzleButtons.choices[k], -1..0, IfEvent.Op1)
            }
            return
        }
        cross(this, dest)
    }

    private fun cross(a: ProtectedAccess, to: CoordGrid) =
        with(a) {
            telejump(to)
            if (vars[BarrowsBits.collapse] != 0) return@with
            // An opening only spawns an encounter when there is room for another combatant.
            val column = ((to.x - 3534 + 8) / 17).coerceIn(0, 2)
            val row = ((to.z - 9678 + 8) / 17).coerceIn(0, 2)
            val center = CoordGrid(3534 + column * 17, 9678 + row * 17)
            if (
                hunt.findNpcs(center, 8, org.rsmod.game.type.hunt.HuntVis.Off).take(11).count() >=
                    11
            )
                return@with
            val roll = random.of(128)
            val left = BarrowsBits.kills.indices.filter { vars[BarrowsBits.kills[it]] == 0 }
            if (roll < 12 && left.isNotEmpty() && opponents[player]?.isSlotAssigned != true)
                summon(a, left[random.of(left.size)])
            else {
                val npc =
                    Npc(
                        types.getValue(
                            when (roll) {
                                in 0..63 -> 1685
                                in 64..95 -> 1678
                                else -> 1679
                            }
                        ),
                        to,
                    )
                npcs.add(npc, 120)
                npc.engagePlayer(player, interactions)
            }
        }

    internal fun chest(a: ProtectedAccess) =
        with(a) {
            if (vars[BarrowsBits.collapse] != 0) {
                mes("You have already searched this chest.")
                return@with
            }
            if (vars[BarrowsBits.chest] == 0) {
                vars[BarrowsBits.chest] = 1
                val missing = vars[BarrowsBits.entrance] - 1
                if (missing in 0..5) summon(a, missing)
                return@with
            }
            controller.claim(a)
            if (vars[BarrowsBits.collapse] != 0) layout(player)
        }

    private fun summon(a: ProtectedAccess, i: Int) =
        with(a) {
            if (vars[BarrowsBits.kills[i]] != 0) {
                mes("You find nothing.")
                return@with
            }
            val previous = opponents[player]
            if (previous?.isSlotAssigned == true) {
                if (
                    previous.coords.level == coords.level &&
                        previous.coords.chebyshevDistance(coords) < 20
                )
                    return@with
                dismiss(player)
            }
            val brother = BarrowsBrothers.all[i]
            val npc = Npc(types[brother.npc], coords)
            configureBrother(npc, brother)
            npcs.add(npc, 500)
            opponents[player] = npc
            npc.engagePlayer(player, interactions)
        }

    internal fun credited(player: Player, npc: Npc): Boolean {
        if (npc.id !in 1672..1688 || npc.coords.x !in 3520..3583 || npc.coords.z !in 9664..9727)
            return false
        return if (npc.id in 1672..1677) opponents[player] === npc else npc.coords.level == 0
    }

    private fun dismiss(player: Player) {
        val npc = opponents.remove(player)
        if (npc?.isSlotAssigned == true) npcs.del(npc, Int.MAX_VALUE)
    }
}
