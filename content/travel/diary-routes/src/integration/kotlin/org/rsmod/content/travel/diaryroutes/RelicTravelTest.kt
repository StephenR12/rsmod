package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RelicTravelTest {
    @Test
    fun GameTestState.`museum discovery opens the Digsite teleport and the final worn charge crumbles`() =
        runGameTest(MuseumFinds::class, ArchaeologicalJewellery::class) {
            player.members = true
            val find = objTypes.getValue(11195)
            player.withProtectedAccess { vars[cacheTypes.varps.getValue(131)] = 9 }
            player.worn[2] = InvObj(objTypes.getValue(11190))
            fun travel() {
                player.withProtectedAccess { publish(WornObjEvents.Op2(2,worn[2]!!)) }
                advance(5)
            }
            travel()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.UseDigsitePendant))
            assertNotNull(player.worn[2])
            player.inv[3] = InvObj(find)
            player.withProtectedAccess { bank[0] = InvObj(find,2) }
            val archaeologist = spawnNpc(player.coords.translate(0,1),npcTypes.getValue(1903))
            player.opNpc1(archaeologist)
            advance(2)
            assertEquals(1,player.vars[varbits.vm_necklace])
            assertEquals(0,player.count(find))
            player.withProtectedAccess { assertEquals(0,invTotal(bank,find)) }
            allocZoneCollision(CoordGrid(3341,3445))
            travel()
            assertEquals(CoordGrid(3341,3445),player.coords)
            assertNull(player.worn[2])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.UseDigsitePendant))
        }

    @Test
    fun GameTestState.`temple charges are shared and the surface stays locked before Desert Hard`() =
        runGameTest(ArchaeologicalJewellery::class) {
            player.members = true
            player.inv[1] = InvObj(objTypes.getValue(6707))
            player.worn[2] = InvObj(objTypes.getValue(6707))
            val charges = cacheTypes.varbits.getValue(1574)
            player.withProtectedAccess {
                vars[charges] = 2
                vars[cacheTypes.varbits.getValue(1560)] = 70
                publish(WornObjEvents.Op5(2,worn[2]!!))
            }
            assertEquals(2,player.vars[charges])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.UseTempleCamulet))
            val temple = CoordGrid(3105,9315,2)
            allocZoneCollision(temple)
            player.withProtectedAccess { opHeld4(1) }
            advance(5)
            assertEquals(temple,player.coords)
            assertEquals(1,player.vars[charges])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.UseTempleCamulet))
            player.withProtectedAccess { publish(WornObjEvents.Op4(2,worn[2]!!)) }
            advance(5)
            assertEquals(0,player.vars[charges])
        }

    @Test
    fun GameTestState.`five unique charged memoir destinations complete the diary and a repeated city consumes a charge`() =
        runGameTest(KourendRelics::class) {
            player.members = true
            val book = objTypes.values.single { it.internalName == "veos_kharedsts_memoirs" }
            player.inv[2] = InvObj(book)
            player.withProtectedAccess {
                for (id in 6030..6034) vars[cacheTypes.varbits.getValue(id)] = 1
                vars[cacheTypes.varbits.getValue(6035)] = 6
            }
            val cities = listOf(CoordGrid(1713,3612),CoordGrid(1804,3748),CoordGrid(1478,3576),CoordGrid(1545,3761),CoordGrid(1681,3747))
            for (index in listOf(0,0,1,2,3,4)) {
                assertFalse(player.hasCompletedDiaryTask(DiaryTask.VisitFiveKourendCities))
                allocZoneCollision(cities[index])
                player.withProtectedAccess { opHeld1(2) }
                advance()
                assertNotNull(player.ui.getModalOrNull(components.mainmodal))
                assertEquals(1,player.vars[cacheTypes.varbits.getValue(6030 + index)])
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,index))
                advance(5)
                assertEquals(cities[index],player.coords)
            }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.VisitFiveKourendCities))
            assertEquals(0,player.vars[cacheTypes.varbits.getValue(6035)])
        }
}
