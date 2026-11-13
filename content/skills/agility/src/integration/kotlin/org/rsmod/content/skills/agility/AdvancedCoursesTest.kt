package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class AdvancedCoursesTest {
    @Test
    fun GameTestState
        .`the mapped Barbarian Outpost lap grants both skills and the Kandarin task`() =
        runGameTest(OutpostAndWildernessCourses::class) {
            player.observerUUID = 1L
            player.members = true
            player.setCurrentLevel(stats.agility, 99)
            fun target(name: String, x: Int, z: Int, plane: Int, shape: Int, angle: Int) =
                placeMapLoc(
                    CoordGrid(x, z, plane),
                    locTypes.values.single { it.internalName == name },
                    LocShape[shape],
                    LocAngle[angle],
                )
            val obstacles =
                listOf(
                    target("obstical_ropeswing1", 2551, 3550, 0, 10, 0),
                    target("barbarian_log_balance1", 2550, 3546, 0, 22, 3),
                    target("agility_obstical_net_barbarian", 2538, 3545, 0, 10, 3),
                    target("balancing_ledge1", 2535, 3547, 1, 22, 1),
                    target("barbarian_laddertop_norim", 2532, 3545, 1, 10, 2),
                    target("castlecrumbly1", 2536, 3553, 0, 10, 2),
                    target("castlecrumbly1", 2539, 3553, 0, 10, 2),
                    target("castlecrumbly1", 2542, 3553, 0, 10, 2),
                )
            val landings =
                listOf(
                    CoordGrid(2551, 3549),
                    CoordGrid(2541, 3546),
                    CoordGrid(2537, 3546, 1),
                    CoordGrid(2532, 3547, 1),
                    CoordGrid(2532, 3546),
                    CoordGrid(2537, 3553),
                    CoordGrid(2540, 3553),
                    CoordGrid(2543, 3553),
                )
            player.placeAt(CoordGrid(2551, 3554))
            for ((i, loc) in obstacles.withIndex()) {
                player.opLoc1(loc)
                advance(35)
                assertEquals(landings[i], player.coords, "Barbarian obstacle ${i + 1}")
            }
            assertEquals(1533, player.statMap.getFineXP(stats.agility))
            assertEquals(413, player.statMap.getFineXP(stats.strength))
            val task = DiaryTask.BarbarianAgilityLap
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
        }

    @Test
    fun GameTestState.`an ordered Wilderness course earns a single claimable ticket`() =
        runGameTest(OutpostAndWildernessCourses::class) {
            player.observerUUID = 1L
            player.members = true
            player.setCurrentLevel(stats.agility, 99)
            fun target(name: String, x: Int, z: Int, shape: Int, angle: Int) =
                placeMapLoc(
                    CoordGrid(x, z),
                    locTypes.values.single { it.internalName == name },
                    LocShape[shape],
                    LocAngle[angle],
                )
            val obstacles =
                listOf(
                    target("obstical_pipe2", 3004, 3938, 10, 3),
                    target("obstical_ropeswing2", 3005, 3952, 10, 2),
                    target("steppingstone1", 3001, 3960, 22, 0),
                    target("wilderness_log_balance1", 3001, 3945, 22, 3),
                    target("wildclimbingrock", 2994, 3936, 22, 2),
                )
            val landings =
                listOf(
                    CoordGrid(3004, 3950),
                    CoordGrid(3005, 3958),
                    CoordGrid(2996, 3960),
                    CoordGrid(2994, 3945),
                    CoordGrid(2994, 3933),
                )
            player.placeAt(CoordGrid(3004, 3937))
            for ((i, loc) in obstacles.withIndex()) {
                random.next = 99
                player.opLoc1(loc)
                advance(50)
                assertEquals(landings[i], player.coords, "Wilderness obstacle ${i + 1}")
            }
            assertEquals(5714, player.statMap.getFineXP(stats.agility))
            val task = DiaryTask.WildernessAgilityLap
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
            val dispenser = target("wildy_agility_pillar", 3005, 3936, 10, 1)
            val ticket = objTypes.values.single { it.internalName == "wildy_agility_token" }
            repeat(2) {
                player.opLoc1(dispenser)
                advance(30)
            }
            assertEquals(1, player.inv.count(ticket))
        }
}
