package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AgilityObjs : ObjReferences() {
    val mark = find("grace")
}

internal object AgilityVarps : VarpReferences() {
    val nextMarkMinute = find("agility_next_mark_minute")
}

internal object AgilityVarpBuilder : VarpBuilder() {
    init {
        build("agility_next_mark_minute") { transmitNever = true }
    }
}

public class CourseMarks @Inject constructor(private val ground: ObjRepository) {
    public fun roll(access: ProtectedAccess, level: Int, tile: CoordGrid) =
        with(access) {
            val minute = (System.currentTimeMillis() / 60_000L).toInt()
            if (minute < vars[AgilityVarps.nextMarkMinute]) return@with
            val base =
                when (level) {
                    40,
                    90 -> 3
                    80 -> 5
                    else -> 6
                }
            val denominator =
                if (level != 40 && statBase(stats.agility) >= level + 20) base * 5 else base
            fun rewarded(region: DiaryRegion, tier: DiaryDifficulty) =
                vars[region.tier(tier).reward] != 0
            val reroll =
                when {
                    level == 90 && rewarded(DiaryRegion.Ardougne, DiaryDifficulty.Elite) -> 25
                    level != 60 -> 0
                    rewarded(DiaryRegion.Kandarin, DiaryDifficulty.Hard) -> 15
                    rewarded(DiaryRegion.Kandarin, DiaryDifficulty.Medium) -> 10
                    rewarded(DiaryRegion.Kandarin, DiaryDifficulty.Easy) -> 5
                    else -> 0
                }
            if (random.of(denominator) >= 2 && (reroll == 0 || random.of(100) >= reroll))
                return@with
            val shortCooldown =
                when (level) {
                    70 -> rewarded(DiaryRegion.Desert, DiaryDifficulty.Hard) && random.of(3) == 0
                    80 -> rewarded(DiaryRegion.Fremennik, DiaryDifficulty.Hard) && random.of(3) == 0
                    90 -> rewarded(DiaryRegion.Ardougne, DiaryDifficulty.Elite) && random.of(2) == 0
                    else -> false
                }
            ground.add(AgilityObjs.mark, tile, duration = 1000, receiver = player, reveal = 1001)
            VarPlayerIntMapSetter.set(
                player,
                AgilityVarps.nextMarkMinute,
                minute + if (shortCooldown) 2 else 3,
            )
        }
}

public class RooftopCourses
@Inject
constructor(private val locs: LocTypeList, private val marks: CourseMarks) : PluginScript() {
    private val nextObstacle = WeakHashMap<Player, Obstacle>()

    override fun ScriptContext.startup() {
        val names = locs.values.associateBy { it.internalName }
        for (course in courses) {
            for ((index, obstacle) in course.stages.withIndex()) {
                for (name in listOf(obstacle.name) + obstacle.alternatives) {
                    val loc = names.getValue("rooftops_${course.cacheName}_$name")
                    onOpLoc1(loc) { cross(course, index, obstacle, it.loc) }
                    onApLoc1(loc) {
                        if (isWithinApRange(it.loc, distance = obstacle.approach))
                            cross(course, index, obstacle, it.loc)
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.cross(
        course: Course,
        index: Int,
        obstacle: Obstacle,
        loc: BoundLocInfo,
    ) {
        if (!player.members) return
        if ((index == 0 || obstacle.fall != null) && stat(stats.agility) < course.level) {
            mes("You need an Agility level of ${course.level} to use this course.")
            return
        }
        arriveDelay()
        val fall = obstacle.fall
        val failed =
            fall != null &&
                stat(stats.agility) < fall.safeLevel &&
                random.of(100) < fall.safeLevel - stat(stats.agility)
        val end = if (failed) checkNotNull(fall).landing else obstacle.landing
        faceLoc(loc)
        val animation =
            when (obstacle.motion) {
                Motion.Climb -> GnomeSequences.climb
                Motion.Balance -> GnomeSequences.balance
                Motion.Jump -> seqs.human_longjump
            }
        anim(animation)
        if (coords.level == end.level) {
            val dx = end.x - coords.x
            val dz = end.z - coords.z
            val facing =
                if (kotlin.math.abs(dx) > kotlin.math.abs(dz)) {
                    if (dx > 0) Constants.em_face_east else Constants.em_face_west
                } else if (dz > 0) Constants.em_face_north else Constants.em_face_south
            exactMove(coords, end, 0, obstacle.ticks * 30, facing)
            delay(obstacle.ticks)
        } else {
            delay(obstacle.ticks)
            telejump(end)
        }
        resetAnim()
        if (coords != end) return
        if (failed) {
            nextObstacle.remove(player)
            takeInstantHit(HitType.Typeless, random.of(fall.damage.first, fall.damage.last))
            mes("You lose your footing and fall.")
            return
        }
        val inOrder = index == 0 || nextObstacle[player] == obstacle
        val last = index == course.stages.lastIndex
        if (inOrder && !last) nextObstacle[player] = course.stages[index + 1]
        else nextObstacle.remove(player)
        if (!last || inOrder) {
            val bonus =
                if (last)
                    when {
                        course.level == 70 &&
                            vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] != 0 -> 126.0
                        course.level == 80 &&
                            vars[DiaryRegion.Fremennik.tier(DiaryDifficulty.Hard).reward] != 0 ->
                            140.0
                        else -> 0.0
                    }
                else 0.0
            statAdvance(stats.agility, obstacle.xp + bonus)
        }
        if (last && inOrder) {
            course.diary?.let { task -> player.completeDiaryTask(task) }
            marks.roll(this, course.level, course.markTiles[random.of(course.markTiles.size)])
            recordLap(AgilityLap.entries.first { it.key==course.cacheName })
        }
    }
}

private enum class Motion {
    Climb,
    Balance,
    Jump,
}

private data class Fall(val landing: CoordGrid, val safeLevel: Int, val damage: IntRange)

private data class Obstacle(
    val name: String,
    val landing: CoordGrid,
    val xp: Double,
    val ticks: Int,
    val motion: Motion,
    val fall: Fall? = null,
    val alternatives: List<String> = emptyList(),
    val approach: Int = 1,
)

private data class Course(
    val cacheName: String,
    val displayName: String,
    val level: Int,
    val diary: DiaryTask?,
    val markTiles: List<CoordGrid>,
    val stages: List<Obstacle>,
)

private val courses =
    listOf(
        Course(
            "draynor",
            "Draynor Village",
            1,
            DiaryTask.DraynorRooftopLap,
            listOf(
                CoordGrid(3099, 3259, 3),
                CoordGrid(3099, 3279, 3),
                CoordGrid(3092, 3266, 3),
                CoordGrid(3088, 3274, 3),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(3102, 3279, 3), 5.0, 4, Motion.Climb),
                Obstacle("tightrope_1", CoordGrid(3090, 3277, 3), 8.0, 8, Motion.Balance),
                Obstacle("tightrope_2", CoordGrid(3092, 3267, 3), 7.0, 9, Motion.Balance),
                Obstacle("wallcrossing", CoordGrid(3088, 3261, 3), 7.0, 5, Motion.Balance),
                Obstacle("wallscramble", CoordGrid(3088, 3255, 3), 10.0, 3, Motion.Climb),
                Obstacle("leapdown", CoordGrid(3097, 3256, 3), 4.0, 3, Motion.Jump),
                Obstacle("crate", CoordGrid(3103, 3261), 79.0, 4, Motion.Climb),
            ),
        ),
        Course(
            "kharid",
            "Al Kharid",
            20,
            DiaryTask.AlKharidRooftopLap,
            listOf(
                CoordGrid(3315, 3161, 1),
                CoordGrid(3266, 3166, 3),
                CoordGrid(3284, 3172, 3),
                CoordGrid(3276, 3185, 3),
                CoordGrid(3302, 3187, 3),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(3273, 3192, 3), 12.0, 4, Motion.Climb),
                Obstacle(
                    "tightrope_1",
                    CoordGrid(3272, 3172, 3),
                    36.0,
                    8,
                    Motion.Balance,
                    Fall(CoordGrid(3271, 3176), 48, 1..5),
                ),
                Obstacle("rope_swing", CoordGrid(3283, 3166, 3), 48.0, 7, Motion.Jump),
                Obstacle(
                    "slide_side",
                    CoordGrid(3314, 3163, 1),
                    48.0,
                    8,
                    Motion.Jump,
                    Fall(CoordGrid(3308, 3163), 48, 1..5),
                ),
                Obstacle("bamboo_tree_top", CoordGrid(3317, 3174, 2), 12.0, 5, Motion.Jump),
                Obstacle("wallclimb_2", CoordGrid(3316, 3180, 3), 6.0, 3, Motion.Climb),
                Obstacle("tightrope_4", CoordGrid(3302, 3186, 3), 18.0, 9, Motion.Balance),
                Obstacle("leapdown", CoordGrid(3299, 3194), 36.0, 4, Motion.Jump),
            ),
        ),
        Course(
            "varrock",
            "Varrock",
            30,
            DiaryTask.VarrockRooftopLap,
            listOf(
                CoordGrid(3185, 3394, 3),
                CoordGrid(3191, 3394, 3),
                CoordGrid(3196, 3415, 1),
                CoordGrid(3238, 3406, 3),
                CoordGrid(3220, 3401, 3),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(3219, 3414, 3), 13.5, 4, Motion.Climb),
                Obstacle(
                    "clothesline",
                    CoordGrid(3202, 3416, 3),
                    23.0,
                    8,
                    Motion.Balance,
                    Fall(CoordGrid(3207, 3415), 46, 3..8),
                ),
                Obstacle("leaptoruins", CoordGrid(3193, 3416, 1), 19.0, 4, Motion.Jump),
                Obstacle(
                    "wallswing",
                    CoordGrid(3192, 3406, 3),
                    28.0,
                    6,
                    Motion.Balance,
                    Fall(CoordGrid(3192, 3410), 46, 2..5),
                ),
                Obstacle("wallscramble", CoordGrid(3197, 3398, 3), 10.0, 4, Motion.Jump),
                Obstacle("leaptobalcony", CoordGrid(3218, 3399, 3), 24.5, 5, Motion.Jump),
                Obstacle("leapdown", CoordGrid(3236, 3403, 3), 4.5, 3, Motion.Jump),
                Obstacle("stepuproof", CoordGrid(3238, 3410, 3), 3.5, 3, Motion.Climb),
                Obstacle("finish", CoordGrid(3237, 3418), 143.7, 5, Motion.Jump),
            ),
        ),
        Course(
            "canifis",
            "Canifis",
            40,
            DiaryTask.CanifisRooftopLap,
            listOf(
                CoordGrid(3506, 3495, 3),
                CoordGrid(3501, 3505, 3),
                CoordGrid(3490, 3501, 3),
                CoordGrid(3478, 3496, 3),
                CoordGrid(3478, 3484, 3),
            ),
            listOf(
                Obstacle("start_tree", CoordGrid(3506, 3493, 3), 10.0, 4, Motion.Climb),
                Obstacle("jump", CoordGrid(3502, 3504, 3), 8.0, 3, Motion.Jump),
                Obstacle("jump_2", CoordGrid(3491, 3499, 3), 8.0, 3, Motion.Jump),
                Obstacle(
                    "jump_5",
                    CoordGrid(3479, 3498, 3),
                    10.0,
                    4,
                    Motion.Jump,
                    Fall(CoordGrid(3484, 3498), 70, 2..5),
                ),
                Obstacle("jump_3", CoordGrid(3478, 3486, 3), 8.0, 3, Motion.Jump),
                Obstacle("polevault", CoordGrid(3489, 3476, 3), 10.0, 6, Motion.Jump),
                Obstacle("jump_4", CoordGrid(3510, 3476, 3), 11.0, 4, Motion.Jump),
                Obstacle("leapdown", CoordGrid(3510, 3484, 0), 175.0, 4, Motion.Jump),
            ),
        ),
        Course(
            "falador",
            "Falador",
            50,
            DiaryTask.FaladorRooftopLap,
            listOf(
                CoordGrid(3045, 3345, 3),
                CoordGrid(3049, 3357, 3),
                CoordGrid(3019, 3345, 3),
                CoordGrid(3013, 3345, 3),
                CoordGrid(3015, 3333, 3),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(3036, 3342, 3), 11.0, 4, Motion.Climb),
                Obstacle("tightrope_1", CoordGrid(3048, 3343, 3), 22.0, 7, Motion.Balance),
                Obstacle(
                    "handholds_start",
                    CoordGrid(3048, 3357, 3),
                    61.0,
                    10,
                    Motion.Balance,
                    Fall(CoordGrid(3051, 3353), 66, 3..8),
                ),
                Obstacle("gap_1", CoordGrid(3048, 3362, 3), 27.0, 3, Motion.Jump),
                Obstacle("gap_2", CoordGrid(3041, 3362, 3), 26.0, 3, Motion.Jump),
                Obstacle("tightrope_2", CoordGrid(3028, 3354, 3), 61.0, 9, Motion.Balance),
                Obstacle("tightrope_3", CoordGrid(3018, 3353, 3), 53.0, 8, Motion.Balance),
                Obstacle("gap_3", CoordGrid(3017, 3347, 3), 30.0, 3, Motion.Jump),
                Obstacle("ledge_1", CoordGrid(3014, 3344, 3), 14.0, 3, Motion.Jump),
                Obstacle("ledge_2", CoordGrid(3014, 3340, 3), 13.0, 3, Motion.Jump),
                Obstacle(
                    "ledge_3a",
                    CoordGrid(3015, 3334, 3),
                    13.0,
                    3,
                    Motion.Jump,
                    alternatives = listOf("ledge_3b"),
                ),
                Obstacle("ledge_4", CoordGrid(3020, 3334, 3), 14.0, 3, Motion.Jump),
                Obstacle("edge", CoordGrid(3028, 3334, 0), 241.0, 4, Motion.Jump),
            ),
        ),
        Course(
            "seers",
            "Seers' Village",
            60,
            DiaryTask.SeersRooftopLap,
            listOf(
                CoordGrid(2727, 3498, 3),
                CoordGrid(2707, 3493, 2),
                CoordGrid(2710, 3493, 2),
                CoordGrid(2712, 3481, 2),
                CoordGrid(2698, 3465, 2),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(2729, 3491, 3), 45.0, 4, Motion.Climb),
                Obstacle(
                    "jump",
                    CoordGrid(2713, 3494, 2),
                    20.0,
                    5,
                    Motion.Jump,
                    Fall(CoordGrid(2716, 3493), 79, 2..6),
                ),
                Obstacle(
                    "tightrope",
                    CoordGrid(2710, 3481, 2),
                    20.0,
                    8,
                    Motion.Balance,
                    Fall(CoordGrid(2710, 3484), 79, 2..6),
                ),
                Obstacle("jump_1", CoordGrid(2710, 3472, 3), 35.0, 4, Motion.Jump),
                Obstacle("jump_2", CoordGrid(2702, 3465, 2), 15.0, 4, Motion.Jump),
                Obstacle("leapdown", CoordGrid(2704, 3464, 0), 435.0, 4, Motion.Jump),
            ),
        ),
        Course(
            "pollnivneach",
            "Pollnivneach",
            70,
            DiaryTask.PollnivneachRooftopLap,
            listOf(
                CoordGrid(3347, 2969, 1),
                CoordGrid(3353, 2975, 1),
                CoordGrid(3359, 2983, 2),
                CoordGrid(3357, 3002, 2),
                CoordGrid(3362, 2993, 2),
            ),
            listOf(
                Obstacle("basket", CoordGrid(3351, 2966, 1), 10.0, 4, Motion.Climb),
                Obstacle(
                    "marketstall",
                    CoordGrid(3352, 2973, 1),
                    45.0,
                    4,
                    Motion.Jump,
                    Fall(CoordGrid(3350, 2971), 85, 2..6),
                    approach = 2,
                ),
                Obstacle(
                    "hangingbanner",
                    CoordGrid(3360, 2977, 1),
                    65.0,
                    4,
                    Motion.Jump,
                    approach = 2,
                ),
                Obstacle("gap", CoordGrid(3366, 2976, 1), 35.0, 3, Motion.Jump),
                Obstacle("tree", CoordGrid(3368, 2982, 1), 75.0, 5, Motion.Jump),
                Obstacle("wallclimb", CoordGrid(3365, 2983, 2), 5.0, 3, Motion.Climb),
                Obstacle("monkeybars_start", CoordGrid(3358, 2993, 2), 55.0, 7, Motion.Balance),
                Obstacle("treetop", CoordGrid(3359, 3000, 2), 60.0, 4, Motion.Jump),
                Obstacle("line", CoordGrid(3363, 2998, 0), 540.0, 5, Motion.Jump),
            ),
        ),
        Course(
            "rellekka",
            "Rellekka",
            80,
            DiaryTask.RellekkaRooftopLap,
            listOf(
                CoordGrid(2623, 3675, 3),
                CoordGrid(2628, 3652, 3),
                CoordGrid(2629, 3653, 3),
                CoordGrid(2641, 3650, 3),
                CoordGrid(2642, 3651, 3),
            ),
            listOf(
                Obstacle("wallclimb", CoordGrid(2625, 3676, 3), 20.0, 4, Motion.Climb),
                Obstacle("gap_1", CoordGrid(2622, 3665, 3), 30.0, 4, Motion.Jump),
                Obstacle(
                    "tightrope_1",
                    CoordGrid(2627, 3654, 3),
                    40.0,
                    7,
                    Motion.Balance,
                    Fall(CoordGrid(2624, 3655), 85, 2..6),
                ),
                Obstacle("gap_2", CoordGrid(2639, 3653, 3), 85.0, 7, Motion.Jump),
                Obstacle("gap_3", CoordGrid(2648, 3662, 3), 25.0, 6, Motion.Jump),
                Obstacle("tightrope_3", CoordGrid(2654, 3670, 3), 105.0, 8, Motion.Balance),
                Obstacle("dropoff", CoordGrid(2652, 3676, 0), 475.0, 4, Motion.Jump),
            ),
        ),
        Course(
            "ardy",
            "Ardougne",
            90,
            DiaryTask.ArdougneRooftopLap,
            listOf(CoordGrid(2657, 3318, 3)),
            listOf(
                Obstacle("wallclimb", CoordGrid(2671, 3299, 3), 43.0, 4, Motion.Climb),
                Obstacle(
                    "jump",
                    CoordGrid(2665, 3318, 3),
                    65.0,
                    5,
                    Motion.Jump,
                    Fall(CoordGrid(2667, 3313), 96, 2..6),
                ),
                Obstacle("plank", CoordGrid(2656, 3318, 3), 50.0, 5, Motion.Balance),
                Obstacle(
                    "jump_2",
                    CoordGrid(2653, 3313, 3),
                    21.0,
                    4,
                    Motion.Jump,
                    Fall(CoordGrid(2653, 3314), 96, 2..6),
                ),
                Obstacle("jump_3", CoordGrid(2651, 3305, 3), 28.0, 4, Motion.Jump),
                Obstacle("wallcrossing", CoordGrid(2656, 3295, 3), 57.0, 6, Motion.Balance),
                Obstacle("jump_4", CoordGrid(2668, 3297, 0), 625.0, 5, Motion.Jump),
            ),
        ),
    )
