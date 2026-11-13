package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class GnomeBallTest {
    @Test
    fun GameTestState
        .`gate referee shooting and trophy work across all five goals without duplicating a deposit`() =
        runGameTest(GnomeBall::class) {
            player.members = true
            player.setCurrentLevel(stats.ranged, 90)
            val ball = objTypes.getValue(751)
            val gate =
                placeMapLoc(
                    CoordGrid(2383, 3488),
                    locTypes.getValue(2394),
                    org.rsmod.game.loc.LocShape.WallStraight,
                )
            val goal = placeMapLoc(CoordGrid(2404, 3488), locTypes.getValue(2393))
            val ref = spawnNpc(CoordGrid(2385, 3489), npcTypes.getValue(3157))
            player.teleport(CoordGrid(2382, 3488))
            player.inv[0] = InvObj(ball)
            player.opLoc1(gate)
            advance(2)
            assertEquals(CoordGrid(2384, 3488), player.coords)
            assertEquals(0, player.count(ball))
            repeat(5) { i ->
                player.teleport(CoordGrid(2385, 3488))
                player.opNpc1(ref)
                advance(2)
                assertEquals(751, player.appearance.heldProp)
                player.teleport(CoordGrid(2403, 3488))
                random.next = 0
                player.opLoc1(goal)
                advance(2)
                assertNull(player.appearance.heldProp)
                assertEquals((i + 1) % 5, player.vars[cacheTypes.varbits.getValue(8387)])
                assertTrue(player.hasCompletedDiaryTask(DiaryTask.ScoreGnomeBall))
            }
            player.teleport(CoordGrid(2384, 3488))
            player.opLoc1(gate)
            advance(2)
            assertEquals(CoordGrid(2382, 3488), player.coords)
            assertEquals(1, player.count(ball))
            assertEquals(0, player.vars[cacheTypes.varps.getValue(65682)])
        }
}
