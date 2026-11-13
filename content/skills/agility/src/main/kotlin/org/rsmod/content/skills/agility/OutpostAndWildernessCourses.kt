package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import kotlin.math.abs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WildCourseBits : VarBitReferences() {
    val ticket = find("wildy_agility_token_available")
}

internal object WildCourseObjs : ObjReferences() {
    val ticket = find("wildy_agility_token")
}

public class OutpostAndWildernessCourses
@Inject
constructor(
    private val types: LocTypeList,
    private val marks: CourseMarks,
    private val random: GameRandom,
) : PluginScript() {
    private data class Step(
        val name: String,
        val anchor: CoordGrid,
        val from: CoordGrid,
        val to: CoordGrid,
        val ticks: Int,
        val xp: Double,
        val animation: SeqType,
        val failure: CoordGrid? = null,
    )

    private val progress = WeakHashMap<Player, Step>()
    private val outpost =
        listOf(
            Step(
                "obstical_ropeswing1",
                CoordGrid(2551, 3550),
                CoordGrid(2551, 3554),
                CoordGrid(2551, 3549),
                4,
                22.0,
                GnomeSequences.swing,
                CoordGrid(2551, 3549),
            ),
            Step(
                "barbarian_log_balance1",
                CoordGrid(2550, 3546),
                CoordGrid(2551, 3546),
                CoordGrid(2541, 3546),
                7,
                13.7,
                GnomeSequences.balance,
                CoordGrid(2545, 3545),
            ),
            Step(
                "agility_obstical_net_barbarian",
                CoordGrid(2538, 3545),
                CoordGrid(2539, 3546),
                CoordGrid(2537, 3546, 1),
                3,
                8.2,
                GnomeSequences.climb,
            ),
            Step(
                "balancing_ledge1",
                CoordGrid(2535, 3547, 1),
                CoordGrid(2536, 3547, 1),
                CoordGrid(2532, 3547, 1),
                5,
                22.0,
                GnomeSequences.balance,
                CoordGrid(2534, 3546),
            ),
            Step(
                "barbarian_laddertop_norim",
                CoordGrid(2532, 3545, 1),
                CoordGrid(2532, 3546, 1),
                CoordGrid(2532, 3546),
                2,
                0.0,
                GnomeSequences.descend,
            ),
            Step(
                "castlecrumbly1",
                CoordGrid(2536, 3553),
                CoordGrid(2535, 3553),
                CoordGrid(2537, 3553),
                3,
                13.7,
                GnomeSequences.climb,
            ),
            Step(
                "castlecrumbly1",
                CoordGrid(2539, 3553),
                CoordGrid(2538, 3553),
                CoordGrid(2540, 3553),
                3,
                13.7,
                GnomeSequences.climb,
            ),
            Step(
                "castlecrumbly1",
                CoordGrid(2542, 3553),
                CoordGrid(2541, 3553),
                CoordGrid(2543, 3553),
                3,
                13.7,
                GnomeSequences.climb,
            ),
        )
    private val wilderness =
        listOf(
            Step(
                "obstical_pipe2",
                CoordGrid(3004, 3938),
                CoordGrid(3004, 3937),
                CoordGrid(3004, 3950),
                8,
                12.5,
                GnomeSequences.crawl,
            ),
            Step(
                "obstical_ropeswing2",
                CoordGrid(3005, 3952),
                CoordGrid(3005, 3951),
                CoordGrid(3005, 3958),
                5,
                20.0,
                GnomeSequences.swing,
                CoordGrid(3005, 10355),
            ),
            Step(
                "steppingstone1",
                CoordGrid(3001, 3960),
                CoordGrid(3002, 3960),
                CoordGrid(2996, 3960),
                6,
                20.0,
                seqs.human_longjump,
                CoordGrid(3002, 3960),
            ),
            Step(
                "wilderness_log_balance1",
                CoordGrid(3001, 3945),
                CoordGrid(3002, 3945),
                CoordGrid(2994, 3945),
                7,
                20.0,
                GnomeSequences.balance,
                CoordGrid(3001, 10345),
            ),
            Step(
                "wildclimbingrock",
                CoordGrid(2994, 3936),
                CoordGrid(2994, 3937),
                CoordGrid(2994, 3933),
                4,
                0.0,
                GnomeSequences.climb,
            ),
        )

    override fun ScriptContext.startup() {
        val names = types.values.associateBy { it.internalName }
        for (course in listOf(outpost, wilderness)) {
            for ((name, steps) in course.groupBy { it.name }) {
                val loc = names.getValue(name)
                onOpLoc1(loc) { step(course, steps, it.loc) }
                onApLoc1(loc) { if (isWithinApRange(it.loc, 1)) step(course, steps, it.loc) }
            }
        }
        onOpLoc1(names.getValue("agility_obstical_pipe_barbarian")) {
            if (player.members && stat(stats.agility) >= 35) {
                // TODO(quest): Gate admission outside this pipe when the Barcrawl miniquest is
                // implemented.
                val dest = CoordGrid(2552, if (coords.z >= 3559) 3558 else 3561)
                crossObstacle(dest, GnomeSequences.crawl, 6)
                statAdvance(stats.agility, 10.0)
            } else mes("You need an Agility level of 35 to enter the course.")
        }
        for (name in listOf("balancegate52a", "balancegate52b_left", "balancegate52b_right")) {
            onOpLoc1(names.getValue(name)) {
                val entering = coords.z <= 3917
                if (!player.members || entering && stat(stats.agility) < 52) {
                    mes("You need an Agility level of 52 to enter the course.")
                } else {
                    if (!entering) {
                        progress.remove(player)
                        VarPlayerIntMapSetter.set(player, WildCourseBits.ticket, 0)
                    }
                    crossObstacle(
                        CoordGrid(2998, if (entering) 3932 else 3916),
                        GnomeSequences.balance,
                        10,
                    )
                }
            }
        }
        val dispenser = names.getValue("wildy_agility_pillar")
        onOpLoc1(dispenser) {
            if (vars[WildCourseBits.ticket] == 0) {
                mes("Complete a lap to earn a Wilderness agility ticket.")
                return@onOpLoc1
            }
            if (invAdd(inv, WildCourseObjs.ticket).success) {
                VarPlayerIntMapSetter.set(player, WildCourseBits.ticket, 0)
                mes("You collect a Wilderness agility ticket.")
            } else mes("You need some free inventory space.")
        }
        onOpLoc3(dispenser) {
            val count = invTotal(inv, WildCourseObjs.ticket)
            if (count == 0) {
                mes("You have no Wilderness agility tickets to redeem.")
                return@onOpLoc3
            }
            val each =
                when (count) {
                    in 1..10 -> 200
                    in 11..50 -> 210
                    in 51..100 -> 220
                    else -> 230
                }
            if (
                choice2(
                    "Redeem $count tickets for ${count.toLong() * each} Agility XP.",
                    true,
                    "Keep the tickets.",
                    false,
                ) && invDel(inv, WildCourseObjs.ticket, count).success
            ) {
                statAdvance(stats.agility, count.toDouble() * each)
            }
        }
        onPlayerLogout { progress.remove(player) }
    }

    private suspend fun ProtectedAccess.step(
        course: List<Step>,
        alternatives: List<Step>,
        loc: BoundLocInfo,
    ) {
        if (!player.members) return
        val wild = course === wilderness
        val level = stat(stats.agility)
        val required = if (wild) 49 else 35
        if (level < required) {
            mes("You need an Agility level of $required to use this obstacle.")
            return
        }
        if (wild && alternatives.first().name == "obstical_pipe2" && loc.z == 3948) {
            crossObstacle(CoordGrid(3004, 3937), GnomeSequences.crawl, 8)
            progress.remove(player)
            statAdvance(stats.agility, 12.5)
            return
        }
        val stage =
            alternatives.firstOrNull {
                it.anchor.level == loc.level &&
                    (it.anchor == loc.coords ||
                        it.name == "wildclimbingrock" &&
                            loc.z == it.anchor.z &&
                            abs(loc.x - it.anchor.x) <= 1)
            } ?: return
        if (coords.chebyshevDistance(stage.from) > 3 || coords.level != stage.from.level) return
        val index = course.indexOf(stage)
        val failed =
            stage.failure != null &&
                if (wild) random.of(100) < (100 - level).coerceAtLeast(1) / 3 + 1
                else random.of(100) < ((93 - level).coerceAtLeast(0) / 2)
        if (failed) {
            crossObstacle(requireNotNull(stage.failure), stage.animation, stage.ticks)
            val damage =
                if (wild) stat(stats.hitpoints) * 15 / 100 + 1 else (stat(stats.hitpoints) / 10 + 1)
            takeInstantHit(HitType.Typeless, damage)
            mes("You lose your footing.")
            return
        }
        crossObstacle(stage.to, stage.animation, stage.ticks)
        if (coords != stage.to) return
        val ordered = index == 0 || progress[player] === stage
        if (ordered && index < course.lastIndex) progress[player] = course[index + 1]
        else progress.remove(player)
        var experience = stage.xp
        if (ordered && index == course.lastIndex) {
            if (wild) {
                experience += 498.9
                player.completeDiaryTask(DiaryTask.WildernessAgilityLap)
                VarPlayerIntMapSetter.set(player, WildCourseBits.ticket, 1)
            } else {
                experience += 46.3
                statAdvance(stats.strength, 41.3)
                player.completeDiaryTask(DiaryTask.BarbarianAgilityLap)
                marks.roll(this, 35, CoordGrid(2532, 3547, 1))
            }
            recordLap(if(wild) AgilityLap.Wild else AgilityLap.Barbarian)
        }
        if (experience > 0) statAdvance(stats.agility, experience)
    }
}
