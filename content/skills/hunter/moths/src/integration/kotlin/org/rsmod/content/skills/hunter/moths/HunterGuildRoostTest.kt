package org.rsmod.content.skills.hunter.moths

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HunterGuildRoostTest {
    @Test
    fun GameTestState.nativeGuildRopesReachTheRoostAndReturnToTheGroundFloor() =
        runGameTest(HunterGuildAccess::class) {
            player.members = true
            player.stats[stats.hunter] = 46
            for (level in listOf(0, 2)) for (x in 1544..1576 step 8) for (z in
                3032..3064 step 8) allocZoneCollision(CoordGrid(x, z, level))
            val up =
                placeMapLoc(
                    CoordGrid(1559, 3045),
                    HunterGuildLocs.ropeUp,
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            player.telejump(CoordGrid(1559, 3046))
            player.opLoc1(up)
            advance()
            assertEquals(CoordGrid(1556, 3046, 2), player.coords)
            val down =
                placeMapLoc(
                    CoordGrid(1556, 3047, 2),
                    HunterGuildLocs.ropeDown,
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            player.opLoc1(down)
            advance()
            assertEquals(CoordGrid(1559, 3046), player.coords)
        }

    @Test
    fun GameTestState.roostAccessRejectsVisibleBoostBelowUnboosted46AndNonmembers() =
        runGameTest(HunterGuildAccess::class) {
            player.members = true
            player.stats[stats.hunter] = 45
            player.setCurrentLevel(stats.hunter, 99)
            val up =
                placeMapLoc(
                    CoordGrid(1559, 3045),
                    HunterGuildLocs.ropeUp,
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            player.telejump(CoordGrid(1559, 3046))
            player.opLoc1(up)
            advance()
            assertEquals(0, player.coords.level)
            player.stats[stats.hunter] = 46
            player.members = false
            player.opLoc1(up)
            advance()
            assertEquals(0, player.coords.level)
        }
}
