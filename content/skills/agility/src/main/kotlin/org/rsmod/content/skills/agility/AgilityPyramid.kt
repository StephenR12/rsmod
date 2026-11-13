package org.rsmod.content.skills.agility

import java.util.WeakHashMap
import kotlin.math.abs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PyramidLocs : LocReferences() {
    val cliffs = listOf(find("ntk_agility_climbing_rocks_1"), find("ntk_agility_climbing_rocks_2"))
    val up = find("agility_pyramid_steps1")
    val down = find("agility_pyramid_steps_top_hotspot")
    val summit = find("agility_pyramid_wall_rocks")
    val doors =
        listOf(find("agility_pyramid_door_hotspot"), find("agility_pyramid_door_hotspot_mirror"))
    val obstacles =
        listOf(
                "jump_hotspot",
                "ledge_hotspot",
                "wallhang_start_hotspot_ground",
                "wallhang_end_hotspot_ground",
                "wallhang_start_hotspot",
                "wallhang_end_hotspot",
                "low_wall",
                "plank_end",
                "plank_start",
                "wallhang_start_south_hotspot",
                "wallhang_end_south_hotspot",
                "wallhang_start_west_hotspot",
                "wallhang_end_west_hotspot",
                "ledgebalance_start_south_hotspot",
                "ledgebalance_end_south_hotspot",
                "ledgebalance_start_west_hotspot",
                "ledgebalance_end_west_hotspot",
            )
            .map { find("agility_pyramid_$it") }
}

internal object PyramidBits : VarBitReferences() {
    val level = find("agility_pyramid_level")
    val taken = find("agility_pyramid_top")
    val traps = (1..5).map { find("agility_pyramid_tilt_$it") }
}

internal object PyramidItems : ObjReferences() {
    val gold = find("agility_pyramid_gold_pyramid")
}

internal object PyramidVars : VarpReferences() {
    val visited = find("agility_pyramid_obstacles")
}

internal object PyramidVarBuilder : VarpBuilder() {
    init {
        build("agility_pyramid_obstacles") { transmitNever = true }
    }
}

internal object PyramidTimers : TimerReferences() {
    val traps = find("agility_pyramid_traps")
}

private enum class PyramidMove(
    val xp: Double,
    val ticks: Int,
    val damage: Int,
    val animation: SeqType,
) {
    Wall(8.0, 3, 8, GnomeSequences.climb),
    Ledge(52.0, 5, 8, GnomeSequences.balance),
    Plank(56.4, 5, 10, GnomeSequences.balance),
    Hang(56.4, 6, 8, ArenaMoves.bars),
    Jump(22.0, 3, 8, ArenaMoves.step),
}

private data class PyramidCrossing(
    val floor: Int,
    val kind: PyramidMove,
    val start: CoordGrid,
    val end: CoordGrid,
    val triggers: Set<CoordGrid>,
)

private fun cross(
    floor: Int,
    kind: PyramidMove,
    x: Int,
    z: Int,
    endX: Int,
    endZ: Int,
    vararg triggers: Pair<Int, Int>,
): PyramidCrossing {
    val plane = if (floor <= 3) floor else floor - 2
    return PyramidCrossing(
        floor,
        kind,
        CoordGrid(x, z, plane),
        CoordGrid(endX, endZ, plane),
        triggers.mapTo(mutableSetOf()) { CoordGrid(it.first, it.second, plane) },
    )
}

private val crossings =
    listOf(
        cross(1, PyramidMove.Wall, 3354, 2848, 3354, 2850, 3354 to 2849),
        cross(1, PyramidMove.Ledge, 3363, 2851, 3367, 2851, 3364 to 2851, 3366 to 2851),
        cross(1, PyramidMove.Plank, 3375, 2846, 3375, 2839, 3375 to 2845, 3375 to 2840),
        cross(
            1,
            PyramidMove.Hang,
            3372,
            2831,
            3366,
            2831,
            3372 to 2832,
            3367 to 2832,
            3370 to 2831,
            3368 to 2831,
        ),
        cross(1, PyramidMove.Ledge, 3363, 2831, 3359, 2831, 3362 to 2831, 3360 to 2831),
        cross(
            2,
            PyramidMove.Hang,
            3356,
            2835,
            3356,
            2842,
            3357 to 2836,
            3357 to 2841,
            3356 to 2837,
            3356 to 2839,
        ),
        cross(2, PyramidMove.Jump, 3356, 2846, 3356, 2848, 3356 to 2847),
        cross(
            2,
            PyramidMove.Hang,
            3358,
            2849,
            3365,
            2849,
            3359 to 2849,
            3364 to 2849,
            3360 to 2849,
            3362 to 2849,
        ),
        cross(2, PyramidMove.Ledge, 3372, 2840, 3372, 2836, 3372 to 2839, 3372 to 2837),
        cross(2, PyramidMove.Wall, 3371, 2833, 3369, 2833, 3370 to 2833),
        cross(2, PyramidMove.Jump, 3365, 2833, 3363, 2833, 3364 to 2833),
        cross(3, PyramidMove.Wall, 3358, 2838, 3358, 2840, 3358 to 2839),
        cross(3, PyramidMove.Ledge, 3358, 2842, 3358, 2846, 3358 to 2843, 3358 to 2845),
        cross(3, PyramidMove.Jump, 3370, 2842, 3370, 2840, 3370 to 2841),
        cross(3, PyramidMove.Plank, 3371, 2835, 3364, 2835, 3370 to 2835, 3365 to 2835),
        cross(4, PyramidMove.Jump, 3040, 4696, 3040, 4698, 3040 to 4697),
        cross(4, PyramidMove.Wall, 3041, 4701, 3043, 4701, 3042 to 4701),
        cross(4, PyramidMove.Jump, 3048, 4696, 3048, 4694, 3048 to 4695),
        cross(4, PyramidMove.Wall, 3048, 4693, 3046, 4693, 3047 to 4693),
        cross(5, PyramidMove.Jump, 3046, 4698, 3046, 4696, 3046 to 4697),
    )
private val stairs =
    listOf(
        CoordGrid(3354, 2831),
        CoordGrid(3356, 2833, 1),
        CoordGrid(3358, 2835, 2),
        CoordGrid(3360, 2837, 3),
        CoordGrid(3042, 4695, 2),
    )
private val landings =
    listOf(
        CoordGrid(3354, 2832, 1),
        CoordGrid(3356, 2834, 2),
        CoordGrid(3358, 2836, 3),
        CoordGrid(3040, 4694, 2),
        CoordGrid(3042, 4696, 3),
    )
private val stones =
    listOf(
        CoordGrid(3354, 2841, 1),
        CoordGrid(3368, 2849, 2),
        CoordGrid(3374, 2835, 1),
        CoordGrid(3048, 4699, 2),
        CoordGrid(3044, 4699, 3),
    )
private val blocks = listOf(CoordGrid(3372, 2847, 2), CoordGrid(3366, 2845, 3))

public class AgilityPyramid : PluginScript() {
    private val previous = WeakHashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        for ((index, type) in PyramidLocs.cliffs.withIndex()) onOpLoc1(type) {
            val edge = if (index == 0) 3336 else 3350
            if (!player.members || abs(it.loc.x - edge) > 1 || it.loc.z !in 2826..2829)
                return@onOpLoc1
            crossObstacle(
                CoordGrid(if (coords.x < edge) edge + 2 else edge - 2, it.loc.z),
                GnomeSequences.climb,
                3,
            )
        }
        onOpLoc1(PyramidLocs.up) { climb(it.loc, true) }
        onOpLoc1(PyramidLocs.down) { climb(it.loc, false) }
        for (type in PyramidLocs.obstacles) {
            onOpLoc1(type) { traverse(it.loc) }
            onApLoc1(type) { if (isWithinApRange(it.loc, 1)) traverse(it.loc) }
        }
        onOpLoc1(PyramidLocs.summit) {
            if (
                !player.members ||
                    vars[PyramidBits.level] != 5 ||
                    it.loc.coords.level != 3 ||
                    vars[PyramidBits.taken] != 0
            )
                return@onOpLoc1
            if (inv.isFull()) {
                mes("You need space in your inventory for the pyramid top.")
                return@onOpLoc1
            }
            anim(GnomeSequences.climb)
            delay(4)
            if (invAdd(inv, PyramidItems.gold).failure) return@onOpLoc1
            vars[PyramidBits.taken] = 1
            player.completeDiaryTask(DiaryTask.ClimbAgilityPyramid)
            mes("You take the golden pyramid from the summit.")
        }
        for (type in PyramidLocs.doors) onOpLoc1(type) {
            if (vars[PyramidBits.level] != 5 || !crossedFloor(5)) return@onOpLoc1
            recordLap(AgilityLap.Pyramid)
            statAdvance(
                stats.agility,
                (300 + statBase(stats.agility) * 8).coerceAtMost(1000).toDouble(),
            )
            reset()
            telejump(CoordGrid(3364, 2829))
        }
        onPlayerLogin {
            if (player.vars[PyramidBits.level] > 0) {
                previous[player] = player.coords
                player.timer(PyramidTimers.traps, 1)
            }
        }
        onPlayerLogout { previous.remove(player) }
        onPlayerTimer(PyramidTimers.traps) { hazards() }
    }

    private fun ProtectedAccess.crossedFloor(level: Int) =
        crossings
            .withIndex()
            .filter { it.value.floor == level }
            .all { vars[PyramidVars.visited] and (1 shl it.index) != 0 }

    private suspend fun ProtectedAccess.climb(loc: BoundLocInfo, up: Boolean) {
        if (!player.members || stat(stats.agility) < 30) {
            mes("You need an Agility level of 30 to climb the pyramid.")
            return
        }
        val index =
            if (up) stairs.indexOf(loc.coords)
            else
                landings.indexOfFirst {
                    it.x == loc.x && it.z == loc.z + 1 && it.level == loc.level
                }
        if (index < 0) return
        if (up && index == 0) {
            vars[PyramidVars.visited] = 0
            vars[PyramidBits.taken] = 0
            timer(PyramidTimers.traps, 1)
        } else if (up && (vars[PyramidBits.level] != index || !crossedFloor(index))) {
            mes("Follow the obstacles around this level before climbing higher.")
            return
        }
        val next = if (up) index + 1 else index
        val dest = if (up) landings[index] else stairs[index].translateZ(-1)
        faceLoc(loc)
        crossObstacle(dest, if (up) GnomeSequences.climb else GnomeSequences.descend, 3)
        vars[PyramidBits.level] = next
        previous[player] = coords
        if (next == 0) reset()
    }

    private suspend fun ProtectedAccess.traverse(loc: BoundLocInfo) {
        val floor = vars[PyramidBits.level]
        val index = crossings.indexOfFirst { it.floor == floor && loc.coords in it.triggers }
        if (index < 0) return
        val obstacle = crossings[index]
        val forward =
            coords.chebyshevDistance(obstacle.start) <= coords.chebyshevDistance(obstacle.end)
        if (random.of(100) < (75 - stat(stats.agility)).coerceAtLeast(0)) {
            fall(obstacle.kind.damage)
            return
        }
        crossObstacle(
            if (forward) obstacle.end else obstacle.start,
            obstacle.kind.animation,
            obstacle.kind.ticks,
        )
        statAdvance(stats.agility, obstacle.kind.xp)
        if (forward) vars[PyramidVars.visited] = vars[PyramidVars.visited] or (1 shl index)
    }

    private fun ProtectedAccess.hazards() {
        val floor = vars[PyramidBits.level]
        val prior = previous.put(player, coords)
        val inside =
            coords.x in 3352..3377 && coords.z in 2829..2854 ||
                coords.x in 3032..3057 && coords.z in 4685..4710
        if (floor == 0 || !inside) {
            reset()
            return
        }
        if (prior == null || coords == prior || prior.chebyshevDistance(coords) > 2) return
        val i =
            stones.indexOfFirst {
                it.level == coords.level &&
                    it.chebyshevDistance(coords) <= 1 &&
                    it.chebyshevDistance(prior) > 1
            }
        if (i >= 0) {
            val failed = random.of(100) < (75 - stat(stats.agility)).coerceAtLeast(0)
            vars[PyramidBits.traps[i]] = if (failed) 1 else 0
            if (failed) fall(8) else statAdvance(stats.agility, 12.0)
        } else if (
            blocks.any {
                it.level == coords.level &&
                    it.chebyshevDistance(coords) <= 1 &&
                    it.chebyshevDistance(prior) > 1
            } && mapClock % 8 in 0..1
        )
            fall(8)
    }

    private fun ProtectedAccess.fall(damage: Int) {
        val level = vars[PyramidBits.level]
        if (level !in 1..5) return
        val upper = level >= 4
        val cx = if (upper) 3044 else 3364
        val cz = if (upper) 4697 else 2841
        val dx = coords.x - cx
        val dz = coords.z - cz
        var x = coords.x + if (abs(dx) >= abs(dz)) dx.compareTo(0) * 2 else 0
        var z = coords.z + if (abs(dz) > abs(dx)) dz.compareTo(0) * 2 else 0
        if (level == 4) {
            x += 320
            z -= 1856
        }
        val below = level - 1
        val plane = if (below >= 4) below - 2 else below
        vars[PyramidBits.level] = below
        telejump(CoordGrid(x, z, plane))
        previous[player] = coords
        takeInstantHit(HitType.Typeless, damage)
        mes("You lose your footing and fall to the level below.")
        if (below == 0) reset()
    }

    private fun ProtectedAccess.reset() {
        vars[PyramidBits.level] = 0
        vars[PyramidBits.taken] = 0
        vars[PyramidVars.visited] = 0
        previous.remove(player)
        player.clearTimer(PyramidTimers.traps)
    }
}
