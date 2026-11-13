package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class AdvancedCoursePlayTest {
    @Test
    fun GameTestState
        .`an unboostable base level and worn ninja greegree unlock six native Ape Atoll obstacles`() =
        runGameTest(ApeAtollCourse::class) {
            player.members = true
            player.setBaseLevel(stats.agility, 47)
            player.setCurrentLevel(stats.agility, 80)
            val xpAt47 = player.statMap.getFineXP(stats.agility)
            fun loc(name: String) = locTypes.values.single { it.internalName == "100_ilm_$name" }
            val bases =
                listOf(
                    CoordGrid(2754, 2742),
                    CoordGrid(2753, 2741),
                    CoordGrid(2752, 2741, 2),
                    CoordGrid(2746, 2741),
                    CoordGrid(2752, 2731),
                    CoordGrid(2757, 2734),
                )
            val types =
                listOf(
                    "stepping_stone",
                    "climbable_tree",
                    "monkeybars_start",
                    "cliff_climb_1",
                    "rope_swing",
                    "agility_tree_base",
                )
            val obstacles =
                bases.mapIndexed { i, tile ->
                    placeMapLoc(tile, loc(types[i]), LocShape[if (i == 0 || i == 3) 22 else 10])
                }
            for (x in 2742..2770 step 4) for (z in 2730..2748 step 4) for (plane in
                listOf(0, 2)) allocZoneCollision(CoordGrid(x, z, plane))
            player.teleport(CoordGrid(2755, 2742))
            player.opLoc1(obstacles.first())
            advance(6)
            assertEquals(xpAt47, player.statMap.getFineXP(stats.agility))
            player.setBaseLevel(stats.agility, 80)
            player.setCurrentLevel(stats.agility, 80)
            val xpAt80 = player.statMap.getFineXP(stats.agility)
            player.opLoc1(obstacles.first())
            advance(6)
            assertEquals(xpAt80, player.statMap.getFineXP(stats.agility))
            val greegree =
                objTypes.values.single {
                    it.internalName == "mm_monkey_greegree_for_small_ninja_monkey"
                }
            player.inv[0] = InvObj(greegree)
            player.withProtectedAccess { opHeld2(0) }
            advance(3)
            assertTrue(greegree in player.worn)
            assertEquals("mm_transmogrification_small_ninja_monkey", player.transmog?.internalName)
            for ((i, obstacle) in obstacles.withIndex()) {
                player.opLoc1(obstacle)
                advance(26)
                assertEquals(
                    xpAt80 + listOf(400, 800, 1200, 1800, 2800, 5800)[i],
                    player.statMap.getFineXP(stats.agility),
                    "Obstacle ${types[i]} landed ${player.coords}",
                )
            }
            assertEquals(CoordGrid(2770, 2747), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CompleteApeAtollLap))
        }

    @Test
    fun GameTestState.`five pyramid levels lead to one golden top and the capped exit bonus`() =
        runGameTest(AgilityPyramid::class) {
            player.members = true
            player.setBaseLevel(stats.agility, 99)
            player.setCurrentLevel(stats.agility, 99)
            fun click(name: String, loc: CoordGrid, from: CoordGrid) {
                val type = locTypes.values.single { it.internalName == "agility_pyramid_$name" }
                val target = placeMapLoc(loc, type)
                player.teleport(from)
                player.opLoc1(target)
                advance(10)
            }
            fun step(x: Int, z: Int, h: Int) {
                click("steps1", CoordGrid(x, z, h), CoordGrid(x, z - 1, h))
            }
            fun cross(name: String, x: Int, z: Int, h: Int, fromX: Int, fromZ: Int) {
                click(name, CoordGrid(x, z, h), CoordGrid(fromX, fromZ, h))
            }
            step(3354, 2831, 0)
            assertEquals(CoordGrid(3354, 2832, 1), player.coords)
            step(3356, 2833, 1)
            assertEquals(1, player.coords.level)
            cross("low_wall", 3354, 2849, 1, 3354, 2848)
            cross("ledge_hotspot", 3364, 2851, 1, 3363, 2851)
            cross("plank_start", 3375, 2845, 1, 3375, 2846)
            cross("wallhang_start_hotspot", 3372, 2832, 1, 3372, 2831)
            cross("ledgebalance_start_south_hotspot", 3362, 2831, 1, 3363, 2831)
            step(3356, 2833, 1)
            assertEquals(2, player.coords.level)
            cross("wallhang_start_west_hotspot", 3356, 2837, 2, 3356, 2836)
            cross("jump_hotspot", 3356, 2847, 2, 3356, 2846)
            cross("wallhang_start_hotspot", 3359, 2849, 2, 3358, 2849)
            cross("ledge_hotspot", 3372, 2839, 2, 3372, 2840)
            cross("low_wall", 3370, 2833, 2, 3371, 2833)
            cross("jump_hotspot", 3364, 2833, 2, 3365, 2833)
            step(3358, 2835, 2)
            assertEquals(3, player.coords.level)
            cross("low_wall", 3358, 2839, 3, 3358, 2838)
            cross("ledgebalance_start_west_hotspot", 3358, 2843, 3, 3358, 2842)
            cross("jump_hotspot", 3370, 2841, 3, 3370, 2842)
            cross("plank_start", 3370, 2835, 3, 3371, 2835)
            step(3360, 2837, 3)
            assertEquals(CoordGrid(3040, 4694, 2), player.coords)
            cross("jump_hotspot", 3040, 4697, 2, 3040, 4696)
            cross("low_wall", 3042, 4701, 2, 3041, 4701)
            cross("jump_hotspot", 3048, 4695, 2, 3048, 4696)
            cross("low_wall", 3047, 4693, 2, 3048, 4693)
            step(3042, 4695, 2)
            assertEquals(CoordGrid(3042, 4696, 3), player.coords)
            repeat(2) { cross("wall_rocks", 3043, 4697, 3, 3042, 4697) }
            val top = objTypes.values.single { it.internalName == "agility_pyramid_gold_pyramid" }
            assertEquals(1, player.count(top))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ClimbAgilityPyramid))
            cross("jump_hotspot", 3046, 4697, 3, 3046, 4698)
            val before = player.statMap.getFineXP(stats.agility)
            cross("door_hotspot", 3045, 4696, 3, 3045, 4695)
            assertEquals(CoordGrid(3364, 2829), player.coords)
            assertEquals(before + 10000, player.statMap.getFineXP(stats.agility))
        }
}
