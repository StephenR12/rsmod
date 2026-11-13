package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonAgilitySequences : SeqReferences() {
    val bars = find("human_monkeybars_walk_long")
}

public class DungeonShortcuts
@Inject
constructor(private val types: LocTypeList, private val random: GameRandom) : PluginScript() {
    private data class Crossing(
        val loc: String,
        val level: Int,
        val xp: Double,
        val westOrSouth: CoordGrid,
        val eastOrNorth: CoordGrid,
        val ticks: Int,
        val animation: SeqType,
        val failure: Failure? = null,
        val diary: DiaryTask? = null,
    )

    private data class Failure(
        val safeLevel: Int,
        val chanceAtMinimum: Int,
        val damage: Int,
        val fall: CoordGrid? = null,
    )

    override fun ScriptContext.startup() {
        val crossings =
            listOf(
                Crossing(
                    "varrock_dungeon_pipe_sc",
                    51,
                    10.0,
                    CoordGrid(3149, 9906),
                    CoordGrid(3155, 9906),
                    6,
                    GnomeSequences.crawl,
                    Failure(81, 25, 4),
                    DiaryTask.SqueezeEdgevillePipe,
                ),
                Crossing(
                    "taverly_dungeon_pipe_sc",
                    70,
                    10.0,
                    CoordGrid(2886, 9799),
                    CoordGrid(2891, 9799),
                    5,
                    GnomeSequences.crawl,
                ),
                Crossing(
                    "taverly_dungeon_floor_spikes_sc",
                    80,
                    12.5,
                    CoordGrid(2878, 9813),
                    CoordGrid(2881, 9813),
                    3,
                    seqs.human_longjump,
                    Failure(99, 20, 3),
                    DiaryTask.JumpTaverleyStrangeFloor,
                ),
                Crossing(
                    "obstical_pipe4",
                    49,
                    7.5,
                    CoordGrid(2572, 9506),
                    CoordGrid(2577, 9506),
                    5,
                    GnomeSequences.crawl,
                    Failure(75, 25, 7),
                ),
                Crossing(
                    "balancing_ledge3",
                    40,
                    22.5,
                    CoordGrid(2580, 9512),
                    CoordGrid(2580, 9520),
                    7,
                    GnomeSequences.balance,
                    Failure(64, 29, 15, CoordGrid(2578, 9578)),
                ),
                Crossing(
                    "monkeybars_end2",
                    57,
                    20.0,
                    CoordGrid(2598, 9488),
                    CoordGrid(2598, 9495),
                    6,
                    DungeonAgilitySequences.bars,
                    Failure(85, 25, 15, CoordGrid(2598, 9556)),
                ),
            )
        val names = types.values.associateBy { it.internalName }
        for (crossing in crossings) {
            onOpLoc1(names.getValue(crossing.loc)) { cross(crossing, it.loc) }
        }
    }

    private suspend fun ProtectedAccess.cross(crossing: Crossing, loc: BoundLocInfo) {
        if (!player.members || coords.level != 0) return
        val a = crossing.westOrSouth
        val b = crossing.eastOrNorth
        val fromA = coords.chebyshevDistance(a) <= coords.chebyshevDistance(b)
        val start = if (fromA) a else b
        if (coords.chebyshevDistance(start) > 2 || loc.coords.chebyshevDistance(start) > 2) return
        val level = stat(stats.agility)
        if (level < crossing.level) {
            mes("You need an Agility level of ${crossing.level} to use this shortcut.")
            return
        }
        val failure = crossing.failure
        val fails =
            failure != null &&
                level < failure.safeLevel &&
                random.of(100) <
                    failure.chanceAtMinimum * (failure.safeLevel - level) /
                        (failure.safeLevel - crossing.level)
        faceLoc(loc)
        anim(crossing.animation)
        val land = if (fromA) b else a
        val dx = land.x - coords.x
        val dz = land.z - coords.z
        val direction =
            if (abs(dx) > abs(dz)) {
                if (dx > 0) Constants.em_face_east else Constants.em_face_west
            } else if (dz > 0) Constants.em_face_north else Constants.em_face_south
        if (fails && failure?.fall != null) {
            delay(crossing.ticks)
            telejump(failure.fall)
        } else {
            exactMove(coords, land, 0, crossing.ticks * 30, direction)
            delay(crossing.ticks)
        }
        resetAnim()
        if (fails) {
            takeInstantHit(HitType.Typeless, random.of(1, requireNotNull(failure).damage))
            mes("You slip on the obstacle.")
            return
        }
        if (coords == land) {
            statAdvance(stats.agility, crossing.xp)
            crossing.diary?.let { player.completeDiaryTask(it) }
        }
    }
}
