package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SalonAndAquariumTest {
    @Test
    fun GameTestState.`the makeover confirms actual hairstyle and shoe kit changes after quest eligibility`() =
        runGameTest(MakeoverServices::class) {
            player.members = true
            val hairdresser = spawnNpc(player.coords.translate(0,1),npcTypes.getValue(1305))
            val styles = cacheTypes.components.getValue((516 shl 16) or 7)
            val confirm = cacheTypes.components.getValue((516 shl 16) or 28)
            player.opNpc4(hairdresser)
            advance(2)
            player.ifButton(confirm,9)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.GetFaladorHaircut))
            player.opNpc4(hairdresser)
            advance(2)
            player.ifButton(styles,5)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.GetFaladorHaircut))
            player.ifButton(confirm,9)
            advance()
            assertEquals(1,player.appearance.identKitSnapshot()[0].toInt())
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.GetFaladorHaircut))

            val yrsa = spawnNpc(player.coords.translate(-1,0),npcTypes.getValue(3933))
            player.opNpc4(yrsa)
            advance(2)
            assertNull(player.ui.getModalOrNull(components.mainmodal))
            player.withProtectedAccess { vars[cacheTypes.varps.getValue(347)] = 10 }
            player.opNpc4(yrsa)
            advance(2)
            player.ifButton(styles,5)
            advance()
            player.ifButton(confirm,9)
            advance()
            assertEquals(43,player.appearance.identKitSnapshot()[6].toInt())
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ChangeRellekkaBoots))
        }

    @Test
    fun GameTestState.`Harry sells the tiny net for a prepared bowl and only a successful aquarium catch earns the diary`() =
        runGameTest(CatherbyAquarium::class) {
            player.members = true
            player.teleport(CoordGrid(2831,3444))
            val harry = spawnNpc(player.coords.translate(1,0),npcTypes.getValue(3213))
            player.inv[0] = InvObj(objs.coins,10)
            fun buyNet() {
                player.opNpc1(harry)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton,1))
                advance()
            }
            buyNet()
            assertEquals(10,player.count(objs.coins))
            player.inv[1] = InvObj(objTypes.getValue(6669))
            buyNet()
            assertEquals(1,player.count(objTypes.getValue(6674)))
            val tank = placeMapLoc(CoordGrid(2831,3445),locTypes.getValue(10091))
            player.opLoc1(tank)
            advance(2)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchHarrysPetFish))
            player.setCurrentLevel(stats.fishing,70)
            random.next = 0
            player.opLoc1(tank)
            advance(2)
            assertEquals(1,player.count(objTypes.getValue(6672)))
            assertEquals(0,player.count(objTypes.getValue(6669)))
            assertEquals(10,player.statMap.getFineXP(stats.fishing))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchHarrysPetFish))
        }
}
