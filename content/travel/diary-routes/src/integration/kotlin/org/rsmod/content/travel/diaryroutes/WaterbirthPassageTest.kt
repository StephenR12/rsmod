package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class WaterbirthPassageTest {
    @Test
    fun GameTestState.`Jarvald fare and deep exit honor distinct quest requirements`() =
        runGameTest(WaterbirthPassage::class) {
            player.members = true
            val boat = spawnNpc(CoordGrid(2549, 3759), npcTypes.getValue(10407))
            val board = CoordGrid(2549, 3758)
            player.teleport(board)
            player.opNpc3(boat)
            advance(2)
            assertEquals(board, player.coords)
            player.inv[0] = InvObj(objs.coins, 1000)
            player.opNpc3(boat)
            advance(2)
            assertEquals(CoordGrid(2621, 3684), player.coords)
            assertNull(player.inv[0])
            val start = CoordGrid(1808, 4405, 3)
            val down = placeMapLoc(start, locTypes.getValue(10195))
            player.teleport(start.translateX(-1))
            player.opLoc1(down)
            advance(2)
            assertEquals(CoordGrid(1810, 4405, 2), player.coords)
            val exit = placeMapLoc(CoordGrid(1975, 4408, 3), locTypes.getValue(10194))
            val foot = CoordGrid(1975, 4409, 3)
            player.teleport(foot)
            player.opLoc1(exit)
            advance(2)
            assertEquals(foot, player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ReachLighthouseFromWaterbirth))
            player.setVarBit(cacheTypes.varbits.getValue(34), 10)
            player.opLoc1(exit)
            advance(2)
            assertEquals(CoordGrid(2515, 10008), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ReachLighthouseFromWaterbirth))
        }
}
