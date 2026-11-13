package org.rsmod.content.areas.city.lumbridge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class HamHideoutTest {
    @Test
    fun GameTestState.`unlocking the trapdoor earns thieving xp and only descent completes the task`() =
        runGameTest(HamHideout::class) {
            val outside = CoordGrid(3166, 3252)
            val start = outside.translate(-1, 0)
            player.teleport(start)
            player.members = true
            val trap = placeMapLoc(outside, locTypes.getValue(5492))
            val xp = player.statMap.getFineXP(stats.thieving)
            player.opLoc1(trap)
            advance()
            assertEquals(start, player.coords)
            random.next = 0
            player.opLoc5(trap)
            advance(3)
            assertEquals(xp + 40, player.statMap.getFineXP(stats.thieving))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterHamHideout))
            player.opLoc1(trap)
            advance()
            assertEquals(CoordGrid(3149,9652), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterHamHideout))
        }
}
