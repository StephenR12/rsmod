package org.rsmod.content.skills.pets

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class SkillPetsTest {
    private class World @Inject constructor(val pets: SkillPets,val events:EventBus,val npcs:NpcRepository,val use:HeldUInteractions)
    @Test
    fun GameTestState.materialChangesPreserveOwnershipAndTheMenagerieReturnsTheExactForm() =
        runInjectedGameTest(World::class,null,PetScript::class,PetMorphs::class) { w ->
            player.members = true
            val base = objTypes.getValue(20661)
            val crystal = objTypes.getValue(24555)
            val seed = objTypes.getValue(23661)
            player.inv[0] = InvObj(base)
            player.inv[1] = InvObj(seed)
            player.withProtectedAccess { w.use.interact(this,inv,seed,1,base,0) }
            assertEquals(crystal.id,player.inv[0]?.id)
            assertEquals(0,player.count(seed))
            assertTrue(w.pets.depositInMenagerie(player,crystal.id,3))
            assertFalse(w.pets.depositInMenagerie(player,crystal.id,3))
            assertEquals(listOf(SkillPets.HousePet(crystal.id,9492,30)),w.pets.storedInMenagerie(player))
            assertEquals(1,player.vars[cacheTypes.varbits.getValue(2175)])
            player.fillInv()
            assertFalse(w.pets.withdrawFromMenagerie(player,crystal.id))
            player.inv[4]=null
            assertTrue(w.pets.withdrawFromMenagerie(player,crystal.id))
            player.withProtectedAccess { opHeld5(4) }
            assertTrue(w.pets.depositInMenagerie(player,crystal.id,3))
            assertEquals(0,player.vars[cacheTypes.varps.getValue(65725)])
            assertEquals(1,w.pets.storedInMenagerie(player).size)
        }

    @Test
    fun GameTestState.aFullBagUsesOnlyTheNewlyCaughtResourceForThePet() =
        runInjectedGameTest(RateWorld::class,null,PetScript::class) { w ->
            player.observerUUID = 123L
            player.members=true
            player.setBaseLevel(org.rsmod.api.config.refs.stats.fishing,35)
            w.pets.award(player,20659)
            player.fillInv()
            player.inv[3]=InvObj(objs.raw_lobster)
            player.inv[12]=null
            val prior=w.rates.occupiedBy(player,objs.raw_lobster.id)
            player.inv[12]=InvObj(objs.raw_lobster)
            (w.random as org.rsmod.api.testing.random.FixedRandom).set(116_129 - 35*25 - 1)
            w.rates.roll(player,org.rsmod.api.config.refs.stats.fishing,13320,116_129,
                w.rates.newResourceSlot(player,objs.raw_lobster.id,prior))
            assertEquals(objs.raw_lobster.id,player.inv[3]?.id)
            assertEquals(13320,player.inv[12]?.id)
            assertTrue(w.ground.findAll(player.coords).any { it.type == objs.raw_lobster.id })
        }

    private class RateWorld @Inject constructor(val rates: SkillPetRates, val pets:SkillPets,
        val ground:org.rsmod.api.repo.obj.ObjRepository,@org.rsmod.api.random.CoreRandom val random:org.rsmod.api.random.GameRandom)

    @Test
    fun GameTestState.followingPetIsPrivateSurvivesLogoutAndCanBePickedUpAndReleased() =
        runInjectedGameTest(World::class,null,PetScript::class) { w ->
            player.members=true
            val form=20675
            w.pets.award(player,form)
            fun near()=w.npcs.findAll(ZoneKey.from(player.coords),2).filter { it.id==7359 }.toList()
            val first=near().single()
            assertEquals(first.slotId,player.vars[cacheTypes.varps.getValue(447)])
            val stranger=registerPlayer(player.coords.translate(0,1))
            stranger.members=true
            stranger.opNpc3(first);advance(2)
            assertEquals(0,stranger.count(objTypes.getValue(form)))
            w.events.publish(SessionStateEvent.Logout(player))
            assertTrue(near().isEmpty())
            w.events.publish(SessionStateEvent.Login(player))
            val pet=near().single()
            player.opNpc3(pet);advance(2)
            assertEquals(1,player.count(objTypes.getValue(form)))
            assertTrue(near().isEmpty())
            val slot=player.inv.indexOfFirst { it?.id==form }
            player.withProtectedAccess { opHeld5(slot) }
            assertEquals(0,player.count(objTypes.getValue(form)))
            assertEquals(1,near().size)
        }
    @Test
    fun GameTestState.fullBagProtectsEntitlementAndProbitaReclaimsOnceThroughTheNativeRow() =
        runInjectedGameTest(World::class,null,PetScript::class) { w ->
            player.members=true
            w.pets.award(player,13071)
            player.fillInv()
            w.pets.award(player,26901)
            assertEquals(1,player.vars[cacheTypes.varbits.getValue(13711)])
            assertEquals(0,player.count(objTypes.getValue(26901)))
            player.ifOpenMain(cacheTypes.interfaces.getValue(148))
            val row=6+5*49
            player.ifButton(cacheTypes.components.getValue((148 shl 16)+11),comsub=row);advance()
            assertEquals(0,player.count(objTypes.getValue(26901)))
            player.inv[0]=null
            player.ifButton(cacheTypes.components.getValue((148 shl 16)+11),comsub=row);advance()
            assertEquals(1,player.count(objTypes.getValue(26901)))
            player.ifButton(cacheTypes.components.getValue((148 shl 16)+11),comsub=row);advance()
            assertEquals(1,player.count(objTypes.getValue(26901)))
            assertEquals(13071,player.vars[cacheTypes.varps.getValue(65725)])
        }
    @Test
    fun GameTestState.guardiansEyeIsReusableAndUnsafeDeathLosesOnlyUnprotectedPets() =
        runInjectedGameTest(World::class,null,PetScript::class) { w ->
            player.members=true
            val original=20673
            val eye=objTypes.getValue(26820)
            player.inv[0]=InvObj(objTypes.getValue(original))
            player.inv[1]=InvObj(eye)
            player.withProtectedAccess { w.use.interact(this,inv,eye,1,objTypes.getValue(original),0) }
            assertEquals(26899,player.inv[0]?.id)
            assertEquals(1,player.count(eye))
            w.events.publish(PlayerDeathEvent.Unsafe(player))
            assertEquals(26899,player.inv[0]?.id)
            player.withProtectedAccess { opHeld5(0) }
            w.pets.award(player,13071)
            w.events.publish(PlayerDeathEvent.Unsafe(player))
            assertEquals(0,player.vars[cacheTypes.varps.getValue(65725)])
            assertEquals(1,player.count(objTypes.getValue(13071)))
            val lost=w.npcs.findAll(ZoneKey.from(player.coords),2).single { it.id==11428 }
            player.opNpc3(lost);advance(2)
            assertEquals(0,player.count(objTypes.getValue(26899)))
            player.ifOpenMain(cacheTypes.interfaces.getValue(148))
            player.ifButton(cacheTypes.components.getValue((148 shl 16)+11),comsub=6+5*31);advance()
            assertEquals(1,player.count(objTypes.getValue(26899)))
        }
}
