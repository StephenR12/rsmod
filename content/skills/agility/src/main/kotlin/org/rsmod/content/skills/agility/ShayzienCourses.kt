package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ShayzienBits : VarBitReferences() {
    val basic = find("shayzien_agility_low_progress")
    val advanced = find("shayzien_agility_up_progress")
}

internal object ShayzienRecords : VarpReferences() {
    val basicLaps = find("shayzien_agility_basic_laps")
    val advancedLaps = find("shayzien_agility_advanced_laps")
    val basicBest = find("shayzien_agility_basic_best_ticks")
    val advancedBest = find("shayzien_agility_advanced_best_ticks")
}

internal object ShayzienRecordBuilder : VarpBuilder() {
    init {
        for (name in
            listOf("basic_laps", "advanced_laps", "basic_best_ticks", "advanced_best_ticks")) build(
            "shayzien_agility_$name"
        ) {
            transmitNever = true
        }
    }
}

internal object ShayzienSoldier : NpcReferences() {
    val soldier = find("shayzien_guard_agility")
}

internal object ShayzienSequences : SeqReferences() {
    val zip = find("zipline_slide")
}

internal object ShayzienSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ShayzienSpawn>("shayzien-course.toml")
    }
}

public class ShayzienCourses
@Inject
constructor(
    private val locs: LocTypeList,
    private val objects: ObjTypeList,
    private val marks: CourseMarks,
) : PluginScript() {
    private data class Obstacle(
        val name: String,
        val from: CoordGrid,
        val to: CoordGrid,
        val xp: Double,
        val ticks: Int,
        val sequence: SeqType,
        val grapple: Boolean = false,
        val fall: CoordGrid? = null,
    )

    private val startTicks = WeakHashMap<Player, Int>()
    private val shared =
        listOf(
            Obstacle(
                "both_start_ladder",
                CoordGrid(1554, 3630),
                CoordGrid(1554, 3632, 3),
                5.5,
                5,
                GnomeSequences.climb,
            ),
            Obstacle(
                "both_rope_climb",
                CoordGrid(1553, 3633, 3),
                CoordGrid(1538, 3633, 2),
                8.0,
                14,
                DungeonAgilitySequences.bars,
            ),
            Obstacle(
                "both_rope_walk",
                CoordGrid(1537, 3633, 2),
                CoordGrid(1527, 3633, 2),
                9.0,
                10,
                GnomeSequences.balance,
            ),
        )
    private val basic =
        listOf(
            Obstacle(
                "low_bar_climb",
                CoordGrid(1523, 3639, 2),
                CoordGrid(1523, 3643, 3),
                7.0,
                5,
                GnomeSequences.climb,
            ),
            Obstacle(
                "low_rope_walk_1",
                CoordGrid(1524, 3644, 3),
                CoordGrid(1539, 3644, 2),
                9.0,
                15,
                GnomeSequences.balance,
            ),
            Obstacle(
                "low_rope_walk_2",
                CoordGrid(1540, 3644, 2),
                CoordGrid(1553, 3644, 2),
                9.0,
                13,
                GnomeSequences.balance,
            ),
            Obstacle(
                "low_end_jump",
                CoordGrid(1554, 3642, 2),
                CoordGrid(1554, 3640),
                106.0,
                3,
                seqs.human_longjump,
            ),
        )
    private val advanced =
        listOf(
            Obstacle(
                "up_swing_jump_1",
                CoordGrid(1521, 3637, 2),
                CoordGrid(1510, 3637, 2),
                23.0,
                6,
                GrappleSequences.grapple,
                grapple = true,
            ),
            Obstacle(
                "up_jump_platform_1",
                CoordGrid(1510, 3635, 2),
                CoordGrid(1510, 3630, 2),
                18.0,
                3,
                seqs.human_longjump,
                fall = CoordGrid(1510, 3634),
            ),
            Obstacle(
                "up_jump_platform_2",
                CoordGrid(1510, 3628, 2),
                CoordGrid(1510, 3620, 2),
                21.0,
                4,
                seqs.human_longjump,
                fall = CoordGrid(1510, 3626),
            ),
            Obstacle(
                "up_swing_jump_2",
                CoordGrid(1511, 3619, 2),
                CoordGrid(1522, 3619, 2),
                23.0,
                6,
                GrappleSequences.grapple,
                grapple = true,
            ),
            Obstacle(
                "up_end_jump",
                CoordGrid(1522, 3620, 2),
                CoordGrid(1522, 3625),
                400.0,
                4,
                ShayzienSequences.zip,
            ),
        )

    override fun ScriptContext.startup() {
        val names = locs.values.associateBy { it.internalName }
        for (path in listOf(shared, basic, advanced)) {
            for ((i, stage) in path.withIndex()) {
                val target = names.getValue("shayzien_agility_${stage.name}")
                onOpLoc1(target) { attempt(stage, path, i, it.loc) }
                onApLoc1(target) { if (isWithinApRange(it.loc, 1)) attempt(stage, path, i, it.loc) }
            }
        }
        onPlayerLogout { startTicks.remove(player) }
        onOpNpc1(ShayzienSoldier.soldier) { records() }
        onOpNpc3(ShayzienSoldier.soldier) { records() }
    }

    private suspend fun ProtectedAccess.attempt(
        stage: Obstacle,
        path: List<Obstacle>,
        index: Int,
        target: BoundLocInfo,
    ) {
        if (
            !player.members ||
                coords.level != stage.from.level ||
                coords.chebyshevDistance(stage.from) > 2
        )
            return
        val upper = path === advanced
        if (upper && stat(stats.agility) < 45) {
            mes("You need an Agility level of 45 to use the advanced course.")
            return
        }
        if (stage.grapple && !player.hasGrapple(objects)) {
            mes("You need to equip a crossbow and a mith grapple.")
            return
        }
        val common = path === shared
        val first = common && index == 0
        if (first) startTicks[player] = mapClock
        val fail =
            stage.fall != null &&
                stat(stats.agility) < 64 &&
                random.of(100) < 64 - stat(stats.agility)
        faceLoc(target)
        crossObstacle(
            if (fail) requireNotNull(stage.fall) else stage.to,
            stage.sequence,
            stage.ticks,
        )
        if (fail) {
            vars[ShayzienBits.basic] = 0
            vars[ShayzienBits.advanced] = 0
            startTicks.remove(player)
            takeInstantHit(HitType.Typeless, random.of(1, 3))
            mes("You miss your footing and fall.")
            return
        }
        if (coords != stage.to) return
        val step = if (common) index else shared.size + index
        val progress = if (upper) ShayzienBits.advanced else ShayzienBits.basic
        val ordered = first || vars[progress] == step
        val finished = !common && index == path.lastIndex
        val next = if (ordered && !finished) step + 1 else 0
        vars[ShayzienBits.basic] = if (upper) 0 else next
        vars[ShayzienBits.advanced] = if (common || upper) next else 0
        if (!finished || ordered) statAdvance(stats.agility, stage.xp)
        if (!finished || !ordered) return
        recordLap(if(upper) AgilityLap.ShayzienAdvanced else AgilityLap.ShayzienBasic)
        val best = if (upper) ShayzienRecords.advancedBest else ShayzienRecords.basicBest
        startTicks.remove(player)?.let { start ->
            val elapsed = mapClock - start
            if (vars[best] == 0 || elapsed < vars[best]) vars[best] = elapsed
        }
        player.completeDiaryTask(DiaryTask.FinishShayzienAgilityLap)
        marks.roll(
            this,
            if (upper) 45 else 1,
            if (upper) CoordGrid(1522, 3620, 2) else CoordGrid(1554, 3642, 2),
        )

    }

    private fun ProtectedAccess.records() {
        fun time(ticks: Int) = if (ticks == 0) "none" else "${ticks * 6 / 10}.${ticks * 6 % 10}s"
        mes(
            "Basic course: ${vars[ShayzienRecords.basicLaps]} laps. Personal best: ${time(vars[ShayzienRecords.basicBest])}."
        )
        mes(
            "Advanced course: ${vars[ShayzienRecords.advancedLaps]} laps. Personal best: ${time(vars[ShayzienRecords.advancedBest])}."
        )
    }
}
