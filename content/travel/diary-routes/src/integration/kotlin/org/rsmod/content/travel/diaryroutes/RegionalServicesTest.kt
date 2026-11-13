package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RegionalServicesTest {
    @Test
    fun GameTestState.`two unnoted silks earn 120 coins only after the merchant accepts the counteroffer`() =
        runGameTest(RegionalMerchants::class) {
            player.members = true
            player.teleport(CoordGrid(2658,3301))
            val merchant = spawnNpc(player.coords.translate(0,1),npcTypes.getValue(8728))
            val silk = objTypes.getValue(950)
            player.inv[0] = InvObj(silk)
            player.inv[1] = InvObj(silk)
            player.opNpc1(merchant)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,2))
            advance()
            assertEquals(2,player.count(silk))
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,1))
            advance()
            assertEquals(120,player.count(objs.coins))
            assertEquals(0,player.count(silk))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SellArdougneSilk))
        }

    @Test
    fun GameTestState.`Bert deposits one delivery per day for a completed Hand in the Sand`() =
        runGameTest(RegionalMerchants::class) {
            player.members = true
            val bert = spawnNpc(player.coords.translate(0,1),npcTypes.getValue(5819))
            player.opNpc3(bert)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ClaimBertSand))
            player.withProtectedAccess { vars[cacheTypes.varbits.getValue(1527)] = 160 }
            repeat(2) {
                player.opNpc3(bert)
                advance()
            }
            player.withProtectedAccess { assertEquals(84,invTotal(bank,objs.bucket_of_sand)) }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ClaimBertSand))
        }

    @Test
    fun GameTestState.`only the Falador pump fills this bucket and records its task`() =
        runInjectedGameTest(LocUInteractions::class,null,WaterSources::class) { use ->
            player.members=true
            player.teleport(CoordGrid(2947,3381))
            val pump=placeMapLoc(CoordGrid(2947,3382),locTypes.getValue(24004))
            player.inv[3]=InvObj(objs.bucket_empty)
            player.withProtectedAccess {
                use.interactOp(this,pump,pump,locTypes[pump],objTypes[objs.bucket_empty],inv,3)
            }
            assertEquals(1,player.count(objs.bucket_water))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FillFaladorPump))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.FillRellekkaWell))
        }

    @Test
    fun GameTestState.`Muddy chest consumes a key and delivers the fixed loot`() =
        runGameTest(KeyChests::class) {
            player.members = true
            val chest=placeMapLoc(player.coords.translate(0,-1),locTypes.getValue(170))
            player.opLoc1(chest)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.OpenMuddyChest))
            player.inv[0]=InvObj(objTypes.getValue(991))
            player.opLoc1(chest)
            advance(2)
            assertEquals(0,player.count(objTypes.getValue(991)))
            assertEquals(2,player.count(objTypes.getValue(2360)))
            assertEquals(5,player.count(objs.death_rune))
            assertEquals(5,player.count(objs.law_rune))
            assertEquals(15,player.count(objs.chaos_rune))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.OpenMuddyChest))
        }
}
