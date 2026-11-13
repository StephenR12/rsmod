package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class KourendActivitiesTest {
    @Test
    fun GameTestState.`a library customer accepts only the requested book and pays redeemable Magic XP`() =
        runGameTest(ArceuusLibrary::class) {
            player.members = true
            val returnTile = CoordGrid(1626,3815)
            player.teleport(returnTile)
            val client = spawnNpc(returnTile.translate(1,0),npcTypes.getValue(7047))
            random.next = 0
            player.opNpc1(client)
            advance(2)
            assertEquals(1,player.vars[cacheTypes.varps.getValue(65575)])
            player.opNpc1(client)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverLibraryBook))
            val stair = placeMapLoc(CoordGrid(1614,3794),locTypes.getValue(27853),angle=org.rsmod.game.loc.LocAngle.South)
            player.teleport(CoordGrid(1617,3794))
            allocZoneCollision(CoordGrid(1610,3794,1))
            player.opLoc1(stair)
            advance(2)
            assertEquals(CoordGrid(1610,3794,1),player.coords)
            player.teleport(CoordGrid(1623,3817,1))
            val shelf = placeMapLoc(CoordGrid(1623,3816,1),locTypes.getValue(28001),angle=org.rsmod.game.loc.LocAngle.East)
            random.next = 0
            random.then = 0
            random.then = 80
            player.opLoc1(shelf)
            advance(3)
            assertEquals(1,player.count(objTypes.getValue(13524)))
            player.teleport(returnTile)
            val other = spawnNpc(returnTile.translate(-1,0),npcTypes.getValue(7048))
            player.opNpc1(other)
            advance(2)
            assertEquals(1,player.count(objTypes.getValue(13524)))
            player.opNpc1(client)
            advance(2)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DeliverLibraryBook))
            assertEquals(0,player.vars[cacheTypes.varps.getValue(65575)])
            assertEquals(0,player.count(objTypes.getValue(13524)))
            val reward = player.inv.indices.single { player.inv[it]?.id == objTypes.getValue(13513).id }
            player.setBaseLevel(stats.magic,20)
            val xp = player.statMap.getFineXP(stats.magic)
            player.withProtectedAccess { opHeld1(reward) }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,0))
            advance(2)
            assertEquals(xp+3000,player.statMap.getFineXP(stats.magic))
            assertEquals(0,player.count(objTypes.getValue(13513)))
        }

    @Test
    fun GameTestState.`the scheduled gang boss drops intelligence and Ginea exchanges it for a manual`() =
        runInjectedGameTest(ObjRepository::class,null,GangMeetings::class) { ground ->
            player.members = true
            player.observerUUID = 2026L
            val meeting = CoordGrid(1650,3726)
            player.teleport(meeting)
            random.next = 0
            eventBus.publish(GameLifecycle.Startup)
            advance(3001)
            val boss = npcRepo.findAll(ZoneKey.from(meeting),1).single { npc -> npc.id == 6901 || npc.id == 6903 }
            eventBus.publish(NpcKillEvents.Credited(player,boss))
            val drop = ground.findAll(boss.coords).single { it.type == objTypes.getValue(13395).id }
            assertEquals(player.observerUUID,drop.receiverId)
            player.teleport(CoordGrid(1504,3631))
            val captain = spawnNpc(CoordGrid(1504,3632),npcTypes.getValue(6895))
            player.opNpc1(captain)
            advance(2)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverGangIntelligence))
            player.inv[0] = InvObj(objTypes.getValue(13395))
            player.opNpc1(captain)
            advance(2)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DeliverGangIntelligence))
            assertEquals(1,player.count(objTypes.getValue(13396)))
            player.setBaseLevel(stats.strength,72)
            val xp = player.statMap.getFineXP(stats.strength)
            player.withProtectedAccess { opHeld1(0) }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,2))
            advance(2)
            assertEquals(xp+22500,player.statMap.getFineXP(stats.strength))
        }
}
