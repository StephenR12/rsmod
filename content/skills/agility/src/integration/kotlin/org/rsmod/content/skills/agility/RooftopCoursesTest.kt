package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class RooftopCoursesTest {
    @Test
    fun GameTestState
        .`Draynor only grants the final experience for all seven obstacles in order`() =
        runGameTest(RooftopCourses::class) {
            player.observerUUID = 1L
            player.members = true
            val start = player.statMap.getFineXP(stats.agility)
            val steps =
                listOf(
                    "wallclimb" to CoordGrid(3103, 3279),
                    "tightrope_1" to CoordGrid(3098, 3277, 3),
                    "tightrope_2" to CoordGrid(3092, 3276, 3),
                    "wallcrossing" to CoordGrid(3089, 3264, 3),
                    "wallscramble" to CoordGrid(3088, 3256, 3),
                    "leapdown" to CoordGrid(3095, 3255, 3),
                    "crate" to CoordGrid(3102, 3261, 3),
                )
            for (x in 3080..3112 step 8) for (z in 3248..3288 step 8) {
                allocZoneCollision(CoordGrid(x, z))
                allocZoneCollision(CoordGrid(x, z, 3))
            }
            val targets =
                steps.mapIndexed { index, (name, coords) ->
                    placeMapLoc(
                        coords,
                        locTypes.values.single { it.internalName == "rooftops_draynor_$name" },
                        LocShape[listOf(5, 22, 22, 10, 10, 10, 10)[index]],
                        LocAngle[listOf(0, 3, 2, 0, 0, 0, 0)[index]],
                    )
                }
            fun use(index: Int) {
                val loc = targets[index]
                val from =
                    listOf(
                        CoordGrid(3103, 3279),
                        CoordGrid(3099, 3277, 3),
                        CoordGrid(3092, 3277, 3),
                        CoordGrid(3089, 3265, 3),
                        CoordGrid(3088, 3257, 3),
                        CoordGrid(3094, 3255, 3),
                        CoordGrid(3101, 3261, 3),
                    )
                player.teleport(from[index])
                player.opLoc1(loc)
                advance(12)
            }
            use(6)
            assertEquals(start, player.statMap.getFineXP(stats.agility))
            var expected = start
            for (index in targets.indices) {
                use(index)
                expected += listOf(50, 80, 70, 70, 100, 40, 790)[index]
                assertEquals(expected, player.statMap.getFineXP(stats.agility), steps[index].first)
            }
            assertEquals(CoordGrid(3103, 3261), player.coords)
            assertEquals(1200, player.statMap.getFineXP(stats.agility) - start)
        }
}
