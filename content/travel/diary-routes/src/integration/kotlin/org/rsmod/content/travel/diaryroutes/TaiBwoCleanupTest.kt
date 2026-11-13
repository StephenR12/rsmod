package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class TaiBwoCleanupTest {
    @Test
    fun GameTestState.`native vines and fence turn successful work into favour`() =
        runGameTest(TaiBwoCleanup::class) {
            player.members = true
            player.setBaseLevel(stats.woodcutting, 99)
            player.setCurrentLevel(stats.woodcutting, 99)
            val jungle = placeMapLoc(CoordGrid(2770, 3061), locTypes.getValue(9013))
            player.teleport(CoordGrid(2769, 3061))
            player.inv[0] = InvObj(objTypes.getValue(975))
            val favour = cacheTypes.varbits.getValue(885)
            val stage = cacheTypes.varbits.getValue(888)
            player.setVarBit(favour, 997)
            player.opLoc4(jungle)
            advance(10)
            assertEquals(0, player.count(objTypes.getValue(6281)))
            player.setVarp(cacheTypes.varps.getValue(175), 12)
            player.setVarBit(stage, 1)
            repeat(16) { random.then = 0 }
            player.opLoc4(jungle)
            advance(12)
            assertEquals(1000, player.vars[favour])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EarnCleanupFavour))
            assertEquals(1, player.count(objTypes.getValue(6281)))
            val fence = placeMapLoc(CoordGrid(2808, 3059), locTypes.getValue(9025))
            player.teleport(CoordGrid(2808, 3059))
            player.opLoc1(fence)
            advance(6)
            assertEquals(0, player.count(objTypes.getValue(6281)))
            assertEquals(2, player.vars[stage])
        }

    @Test
    fun GameTestState
        .`Safta charges all four ingredients together and honours both cut and uncut gems`() =
        runGameTest(TaiBwoCleanup::class) {
            player.members = true
            player.setVarp(cacheTypes.varps.getValue(175), 12)
            player.setVarBit(cacheTypes.varbits.getValue(888), 5)
            val safta = spawnNpc(CoordGrid(2791, 3102), npcTypes.getValue(6422))
            player.teleport(CoordGrid(2790, 3102))
            player.inv[0] = InvObj(objTypes.getValue(1609))
            player.inv[1] = InvObj(objTypes.getValue(1625))
            player.inv[2] = InvObj(objTypes.getValue(1625))
            player.inv[3] = InvObj(objTypes.getValue(6311))
            player.inv[4] = InvObj(objTypes.getValue(6306), 299)
            fun order() {
                player.opNpc1(safta)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 1))
                advance(1)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
                advance(2)
            }
            order()
            assertEquals(299, player.count(objTypes.getValue(6306)))
            assertEquals(2, player.count(objTypes.getValue(1625)))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ExchangeMacheteGems))
            player.inv[4] = InvObj(objTypes.getValue(6306), 300)
            order()
            assertEquals(1, player.count(objTypes.getValue(6313)))
            assertEquals(0, player.count(objTypes.getValue(6306)))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ExchangeMacheteGems))
        }
}
