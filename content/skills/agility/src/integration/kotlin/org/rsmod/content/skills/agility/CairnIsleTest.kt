package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class CairnIsleTest {
    @Test
    fun GameTestState
        .`the rock slide opens the real walking route and the island end awards exploration`() =
        runGameTest(CairnIsle::class) {
            player.members = true
            val rocks = locTypes.values.single { it.internalName == "zqclimbingrocks" }
            val east = placeMapLoc(CoordGrid(2794, 2979), rocks, LocShape[22], LocAngle[0])
            player.teleport(CoordGrid(2795, 2979))
            player.setCurrentLevel(stats.agility, 14)
            player.opLoc1(east)
            advance(8)
            assertEquals(CoordGrid(2795, 2979), player.coords)
            player.setCurrentLevel(stats.agility, 99)
            player.opLoc1(east)
            advance(8)
            assertEquals(CoordGrid(2791, 2979), player.coords)
            val task = DiaryTask.ExploreCairnIsle
            fun complete() = task.region.tasks(task.difficulty)[task.number - 1].isComplete(player)
            assertFalse(complete())
            random.next = 99
            player.moveGameClick(CoordGrid(2770, 2979))
            advance(45)
            assertEquals(CoordGrid(2770, 2979), player.coords)
            assertTrue(complete())
        }
}
