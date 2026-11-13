package org.rsmod.content.skills.combattraining.chompy

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class ChompyHuntingTest {
    private class World @Inject constructor(val npcs: NpcRepository,val drops: ObjRepository,val hits: StandardNpcHitModifier)
    private fun GameTestScope.unlock() {
        player.members=true
        player.setVarp(varps.big_chompy_bird_hunting,65)
        player.observerUUID=player.uuid
        advance(10)
    }

    @Test
    fun GameTestState.`swamp gas makes bait which lures a killable bird and its carcass can be plucked once`() =
        runInjectedGameTest(World::class,null,ChompyHunting::class,OgreBellows::class,NpcHitScript::class) { world ->
            unlock()
            val empty=objTypes.getValue(2871)
            val full=objTypes.getValue(2872)
            val bait=objTypes.getValue(2875)
            player.inv[0]=InvObj(empty)
            val pond=placeMapLoc(CoordGrid(2595,2966),locTypes.getValue(684))
            player.placeAt(CoordGrid(2594,2966))
            player.opLoc1(pond);advance(3)
            assertEquals(1,player.count(full))
            val bank=CoordGrid(2592,2964)
            val toad=spawnNpc(bank,npcTypes.getValue(1473))
            player.placeAt(bank.translate(-1,0))
            player.opNpc1(toad);advance(2)
            assertEquals(1,player.count(bait))
            assertEquals(1,player.count(objTypes.getValue(2873)))
            val clearing=CoordGrid(2588,2964)
            player.placeAt(clearing)
            player.withProtectedAccess { opHeld1(inv.indexOfFirst {it?.id==bait.id}) }
            assertEquals(0,player.count(bait))
            fun near(id:Int)=world.npcs.findAll(ZoneKey.from(clearing),3).filter {it.id==id}.toList()
            assertEquals(1,near(1474).size)
            random.next=0;random.then=0
            advanceUntil({near(1475).isNotEmpty()},28)
            val bird=near(1475).single()
            assertTrue(bird.coords.chebyshevDistance(clearing) in 3..10)
            player.worn[3]=InvObj(objs.ogre_bow)
            bird.queueHit(player,1,HitType.Melee,10,world.hits)
            bird.queueHit(player,1,HitType.Ranged,10,world.hits,sourceSecondary=objs.bronze_arrow)
            advance(2)
            assertEquals(10,bird.hitpoints)
            bird.queueHit(player,1,HitType.Ranged,10,world.hits,sourceSecondary=objs.ogre_arrow)
            advance(2)
            val corpse=near(1476).single()
            val tile=corpse.coords
            assertEquals(1,player.vars[cacheTypes.varps.getValue(65722)])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ClaimChompyHat))
            player.placeAt(tile.translate(1,0))
            random.next=16
            player.opNpc4(corpse);advance(3)
            assertTrue(near(1476).isEmpty())
            assertEquals(16,player.count(objs.feather))
            assertTrue(world.drops.findAll(tile).any {it.type==objs.raw_chompy.id})
            assertTrue(world.drops.findAll(tile).any {it.type==objs.bones.id})
        }

    @Test
    fun GameTestState.`each Rantz diary needs an actual hat claim and room to receive it`() = runGameTest(Rantz::class) {
        unlock()
        val rantz=spawnNpc(CoordGrid(2630,2981),npcTypes.getValue(1470))
        player.placeAt(CoordGrid(2629,2981))
        val count=cacheTypes.varps.getValue(65722)
        val tasks=listOf(DiaryTask.ClaimChompyHat,DiaryTask.ClaimMarksmanHat,DiaryTask.ClaimForesterHat,DiaryTask.ClaimOgreExpertHat)
        val thresholds=listOf(30,125,300,1000)
        val desired=listOf(2978,2983,2986,2990)
        fun menu() { player.opNpc4(rantz);advance(2) }
        fun first() { player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a,1));advance() }
        player.setVarp(count,30);menu()
        assertFalse(player.hasCompletedDiaryTask(tasks[0]))
        player.worn[3]=InvObj(objs.comp_ogre_bow)
        player.setVarp(count,29);menu()
        assertEquals(0,player.count(objTypes.getValue(2978)))
        for (i in thresholds.indices) {
            player.setVarp(count,thresholds[i])
            val hat=objTypes.getValue(desired[i])
            // Rantz checks loose, worn, banked and costume-room items before offering reclaims.
            val bank = player.invMap.getOrPut(cacheTypes.invs[org.rsmod.api.config.refs.invs.bank])
            for (id in 2978 until hat.id) bank[id-2978]=InvObj(objTypes.getValue(id))
            if (i==0) {
                for (slot in player.inv.indices) player.inv[slot]=InvObj(objs.beer)
                menu();first()
                assertFalse(player.hasCompletedDiaryTask(tasks[i]))
                player.inv[0]=null
            }
            menu()
            assertFalse(player.hasCompletedDiaryTask(tasks[i]))
            first()
            assertTrue(player.hasCompletedDiaryTask(tasks[i]))
            assertEquals(1,player.count(hat), "Claim at ${thresholds[i]} kills gave ${player.inv.filterNotNull().map { it.id }}")
            player.inv[0]=null
        }
    }

    @Test
    fun GameTestState.`Rantz pays the daily arrow allowance once even when a tier changes`() = runGameTest(Rantz::class) {
        unlock()
        val rantz=spawnNpc(CoordGrid(2630,2981),npcTypes.getValue(1470))
        player.placeAt(CoordGrid(2629,2981))
        fun claim() { player.opNpc3(rantz);advance(2) }
        claim();assertEquals(0,player.count(objs.ogre_arrow))
        player.setVarBit(DiaryRegion.Western.tier(DiaryDifficulty.Medium).reward,1)
        claim();assertEquals(50,player.count(objs.ogre_arrow))
        player.setVarBit(DiaryRegion.Western.tier(DiaryDifficulty.Elite).reward,1)
        claim();assertEquals(50,player.count(objs.ogre_arrow))
        player.setVarp(cacheTypes.varps.getValue(65723),1)
        claim();assertEquals(200,player.count(objs.ogre_arrow))
    }
    @Test
    fun GameTestState.`tethering balloons spends a bellows load and every fifteenth use consumes the wool`() =
        runInjectedGameTest(HeldUInteractions::class,null,OgreBellows::class) { held ->
            unlock()
            player.setVarBit(cacheTypes.varbits.getValue(1904),170)
            val full=objTypes.getValue(2872)
            val bloated=objTypes.getValue(2875)
            val rock=objTypes.getValue(1480)
            player.inv[4]=InvObj(objs.ball_of_wool)
            fun balloon() {
                player.inv[0]=InvObj(full);player.inv[1]=InvObj(bloated);player.inv[2]=InvObj(rock)
                player.withProtectedAccess { held.interact(this,inv,full,0,bloated,1) }
                assertEquals(2873,player.inv[0]?.id);assertEquals(7564,player.inv[1]?.id)
            }
            player.setVarp(cacheTypes.varps.getValue(65724),13)
            balloon();assertEquals(1,player.count(objs.ball_of_wool))
            balloon();assertEquals(0,player.count(objs.ball_of_wool))
        }
}
