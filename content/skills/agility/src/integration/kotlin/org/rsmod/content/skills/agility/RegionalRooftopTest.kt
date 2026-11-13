package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class RegionalRooftopTest {
    @Test
    fun GameTestState.`Al Kharid lap routes between the mapped obstacles`() =
        lap("kharid", 2160, CoordGrid(3273, 3195))

    @Test
    fun GameTestState.`Varrock lap routes between the mapped obstacles`() =
        lap("varrock", 2697, CoordGrid(3221, 3414))

    @Test
    fun GameTestState.`Canifis lap routes between the mapped obstacles`() =
        lap("canifis", 2400, CoordGrid(3507, 3488))

    @Test
    fun GameTestState.`Falador lap routes between the mapped obstacles`() =
        lap("falador", 5860, CoordGrid(3036, 3341))

    @Test
    fun GameTestState.`Seers lap routes between the mapped obstacles`() =
        lap("seers", 5700, CoordGrid(2729, 3489))

    @Test
    fun GameTestState.`Pollnivneach lap routes between the mapped obstacles`() =
        lap("pollnivneach", 8900, CoordGrid(3351, 2961))

    @Test
    fun GameTestState.`Rellekka lap routes between the mapped obstacles`() =
        lap("rellekka", 7800, CoordGrid(2625, 3677))

    @Test
    fun GameTestState.`Ardougne lap routes between the mapped obstacles`() =
        lap("ardy", 8890, CoordGrid(2673, 3298))

    private fun GameTestState.lap(course: String, fineXp: Int, start: CoordGrid) =
        runGameTest(RooftopCourses::class) {
            player.members = true
            player.observerUUID = 1L
            player.setCurrentLevel(stats.agility, 99)
            val lines =
                checkNotNull(
                        this@RegionalRooftopTest.javaClass.getResourceAsStream(
                            "/rooftop-placements.tsv"
                        )
                    )
                    .bufferedReader()
                    .use { it.readLines() }
                    .map { it.split('\t') }
                    .filter { it.first() == course }
            val extent = lines.map { CoordGrid(it[2].toInt(), it[3].toInt()) }
            for (x in extent.minOf { it.x } - 8..extent.maxOf { it.x } + 8 step 8) {
                for (z in extent.minOf { it.z } - 8..extent.maxOf { it.z } + 8 step 8) {
                    for (level in 0..3) allocZoneCollision(CoordGrid(x, z, level))
                }
            }
            val targets =
                lines.map { row ->
                    val type = locTypes.values.single { it.internalName == row[1] }
                    placeMapLoc(
                        CoordGrid(row[2].toInt(), row[3].toInt(), row[4].toInt()),
                        type,
                        LocShape[row[5].toInt()],
                        LocAngle[row[6].toInt()],
                    )
                }
            player.placeAt(start)
            val initial = player.statMap.getFineXP(stats.agility)
            for (loc in targets) {
                val before = player.statMap.getFineXP(stats.agility)
                val position = player.coords
                player.opLoc1(loc)
                advance(55)
                assertTrue(
                    player.statMap.getFineXP(stats.agility) > before,
                    "Could not reach ${loc.id} from $position; stopped at ${player.coords}",
                )
            }
            assertEquals(fineXp, player.statMap.getFineXP(stats.agility) - initial)
        }
}
