package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StrongholdTest {
    @Test
    fun GameTestState.`the first floor's paired gates allow escape and require a correct answer to leave safety`() =
        runGameTest(StrongholdOfSecurity::class) {
            val origin = CoordGrid(1858,5238)
            player.teleport(origin.translate(0,1))
            val entrance = placeMapLoc(origin,locTypes.getValue(19206),LocShape.WallStraight,LocAngle.North)
            val exit = placeMapLoc(origin.translate(0,-3),locTypes.getValue(19206),LocShape.WallStraight,LocAngle.North)
            player.opLoc1(entrance)
            advance(2)
            assertEquals(origin,player.coords)
            player.teleport(exit.coords.translate(0,1))
            fun answer(option:Int) {
                random.next = 0
                player.opLoc1(exit)
                advance(2)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,option))
                advance()
            }
            answer(0)
            assertEquals(exit.coords.translate(0,1),player.coords)
            answer(1)
            assertEquals(exit.coords,player.coords)
        }

    @Test
    fun GameTestState.`eligibility gates treasure and shortcuts while entering Famine awards the diary`() =
        runGameTest(StrongholdOfSecurity::class) {
            player.members = true
            player.teleport(CoordGrid(1863,5237))
            val portal = placeMapLoc(CoordGrid(1863,5238),locTypes.getValue(20786))
            player.appearance.combatLevel = 25
            player.opLoc1(portal)
            advance(2)
            assertEquals(CoordGrid(1863,5237),player.coords)
            val rewardTile = CoordGrid(1907,5223)
            allocZoneCollision(rewardTile)
            player.appearance.combatLevel = 26
            player.opLoc1(portal)
            advance(2)
            assertEquals(rewardTile,player.coords)
            val reward = placeMapLoc(rewardTile.translate(0,-1),locTypes.getValue(20656))
            player.opLoc1(reward)
            advance()
            assertEquals(0,player.count(objs.coins))
            player.twoFactorEnabled = true
            repeat(2) { player.opLoc1(reward); advance(2) }
            assertEquals(2000,player.count(objs.coins))
            assertEquals(1,player.vars[cacheTypes.varbits.getValue(2309)])
            player.teleport(CoordGrid(1903,5222))
            val ladder = placeMapLoc(CoordGrid(1902,5222),locTypes.getValue(20785))
            val second = CoordGrid(2042,5245)
            allocZoneCollision(second)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterStrongholdSecondFloor))
            player.opLoc1(ladder)
            advance(2)
            assertEquals(second,player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterStrongholdSecondFloor))
        }

    @Test
    fun GameTestState.`assembled skull sceptres share charges in free worlds and refund the next copy on shattering`() =
        runInjectedGameTest(HeldUInteractions::class,null,SkullSceptre::class) { held ->
            fun use(first:Int,second:Int) {
                val a = player.inv.indices.first { player.inv[it]?.id == first }
                val b = player.inv.indices.first { player.inv[it]?.id == second }
                player.withProtectedAccess { held.interact(this,inv,objTypes.getValue(first),a,objTypes.getValue(second),b) }
            }
            for ((slot,id) in listOf(9007,9008,9010,9011).withIndex()) player.inv[slot] = InvObj(objTypes.getValue(id))
            use(9008,9007)
            use(9010,9011)
            use(9012,9009)
            val sceptre = objTypes.getValue(9013)
            val slot = player.inv.indices.single { player.inv[it]?.id == sceptre.id }
            val charges = cacheTypes.varps.getValue(65576)
            val destination = CoordGrid(3081,3421)
            allocZoneCollision(destination)
            player.withProtectedAccess { vars[varbits.teleblock_cycles] = 150; opHeld3(slot) }
            advance(5)
            assertEquals(11,player.vars[charges])
            player.withProtectedAccess { vars[varbits.teleblock_cycles] = 0; opHeld3(slot) }
            advance(5)
            assertEquals(destination,player.coords)
            assertEquals(10,player.vars[charges])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.UseSkullSceptre))
            player.members = true
            player.inv[25] = InvObj(sceptre)
            player.withProtectedAccess { vars[charges] = 2; opHeld3(slot) }
            advance(5)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.UseSkullSceptre))
            assertNull(player.inv[slot])
            assertEquals(1,player.count(sceptre))
            assertEquals(11,player.vars[charges])
        }
}
