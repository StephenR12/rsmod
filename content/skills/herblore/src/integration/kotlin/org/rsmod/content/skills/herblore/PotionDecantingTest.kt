package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class PotionDecantingTest {
    @Test
    fun GameTestState.allRegisteredFamiliesConserveDosesAndContainersWhenCombining() = game { d ->
        player.members = true
        assertTrue(DecantFluids.all.size > 50)
        assertEquals(DecantFluids.all.sumOf { it.doses.size }, DecantFluids.byId.size)
        for (fluid in DecantFluids.all) {
            player.clearInv()
            player.inv[0] = InvObj(fluid.doses[0])
            player.inv[1] = InvObj(fluid.doses[0])
            player.withProtectedAccess { assertTrue(d.decant.combine(this, 0, 1)) }
            assertEquals(fluid.empty.id, player.inv[0]?.id)
            assertEquals(fluid.doses[1].id, player.inv[1]?.id)
        }
    }

    @Test
    fun GameTestState.nativeItemUseCombinesThreeDosePotionsAndPreservesOtherSlots() = game { d ->
        player.members = true
        player.inv[3] = InvObj(objs.attack_potion_3)
        player.inv[7] = InvObj(objs.attack_potion_3)
        player.inv[9] = InvObj(objs.attack_potion_3)
        player.withProtectedAccess {
            d.use.interact(
                this,
                player.inv,
                d.types[objs.attack_potion_3],
                3,
                d.types[objs.attack_potion_3],
                7,
            )
        }
        assertEquals(objs.attack_potion_2.id, player.inv[3]?.id)
        assertEquals(objs.attack_potion_4.id, player.inv[7]?.id)
        assertEquals(objs.attack_potion_3.id, player.inv[9]?.id)
    }

    @Test
    fun GameTestState.fourThreeAndTwoDoseContainersSplitIntoTheDocumentedHalves() = game { d ->
        player.members = true
        val family = requireNotNull(DecantFluids.byId[objs.attack_potion_4.id])
        for (amount in 2..4) {
            player.clearInv()
            player.inv[0] = InvObj(family.doses[amount - 1])
            player.inv[1] = InvObj(objs.empty_vial)
            player.withProtectedAccess { assertTrue(d.decant.combine(this, 0, 1)) }
            assertEquals(family.doses[(amount + 1) / 2 - 1].id, player.inv[0]?.id)
            assertEquals(family.doses[amount / 2 - 1].id, player.inv[1]?.id)
        }
        player.clearInv()
        player.inv[0] = InvObj(objs.attack_potion_1)
        player.inv[1] = InvObj(objs.empty_vial)
        player.withProtectedAccess { assertFalse(d.decant.combine(this, 0, 1)) }
    }

    @Test
    fun GameTestState.oilsCombineAndNativeEmptyReturnsTheSameContainerWithoutXp() = game { d ->
        player.members = true
        val before = player.statMap.getFineXP(stats.herblore)
        player.inv[0] = InvObj(DecantObjs.olive[2])
        player.inv[1] = InvObj(DecantObjs.olive[2])
        player.withProtectedAccess {
            assertTrue(d.decant.combine(this, 0, 1))
            d.held.interact(this, player.inv, 1, HeldOp.Op4)
        }
        assertEquals(DecantObjs.olive[1].id, player.inv[0]?.id)
        assertEquals(objs.empty_vial.id, player.inv[1]?.id)
        player.inv[2] = InvObj(objs.sacred_oil_4)
        player.withProtectedAccess { d.held.interact(this, player.inv, 2, HeldOp.Op4) }
        assertEquals(objs.empty_vial.id, player.inv[2]?.id)
        assertEquals(before, player.statMap.getFineXP(stats.herblore))
    }

    @Test
    fun GameTestState.mismatchedFluidsSameSlotNotesAndNonmemberOilUseAreRejected() = game { d ->
        player.members = true
        player.inv[0] = InvObj(objs.attack_potion_2)
        player.inv[1] = InvObj(objs.strength_potion_2)
        player.withProtectedAccess {
            assertFalse(d.decant.combine(this, 0, 1))
            assertFalse(d.decant.combine(this, 0, 0))
        }
        player.inv[1] = InvObj(note(d, objs.attack_potion_2), 2)
        player.withProtectedAccess { assertFalse(d.decant.combine(this, 0, 1)) }
        player.members = false
        player.inv[0] = InvObj(objs.sacred_oil_2)
        player.inv[1] = InvObj(objs.sacred_oil_2)
        player.withProtectedAccess {
            assertFalse(d.decant.combine(this, 0, 1))
            assertFalse(
                d.decant.empty(this, 0, requireNotNull(DecantFluids.byId[objs.sacred_oil_2.id]))
            )
        }
        assertEquals(objs.sacred_oil_2.id, player.inv[0]?.id)
    }

    @Test
    fun GameTestState.bulkFourDoseCombiningReturnsEverySurplusVialForFree() = game { d ->
        player.members = true
        for (slot in 0..1) player.inv[slot] = InvObj(objs.attack_potion_3)
        for (slot in 2..3) player.inv[slot] = InvObj(objs.attack_potion_1)
        player.withProtectedAccess { assertTrue(d.decant.decant(this, 4)) }
        assertEquals(2, player.count(objs.attack_potion_4))
        assertEquals(2, player.count(objs.empty_vial))
        assertEquals(0, player.count(objs.coins))
    }

    @Test
    fun GameTestState.notedInputsProduceNotedResultsAndNotedSurplusContainers() = game { d ->
        player.members = true
        player.inv[0] = InvObj(note(d, objs.attack_potion_3), 100)
        player.withProtectedAccess { assertTrue(d.decant.decant(this, 4)) }
        assertEquals(75, player.count(note(d, objs.attack_potion_4)))
        assertEquals(25, player.count(note(d, objs.empty_vial)))
        assertEquals(0, player.count(objs.attack_potion_4))
    }

    @Test
    fun GameTestState
        .insufficientUnnotedSpaceAutomaticallyNotesResultsAndChargesMissingVialsExactlyOnce() =
        game { d ->
            player.members = true
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            player.inv[0] = InvObj(objs.attack_potion_4)
            player.inv[1] = InvObj(objs.coins, 1000)
            player.withProtectedAccess { assertTrue(d.decant.decant(this, 1)) }
            assertEquals(4, player.count(note(d, objs.attack_potion_1)))
            assertEquals(1000 - 3 * (d.types[objs.empty_vial].cost + 2), player.count(objs.coins))
            assertEquals(26, player.count(objs.logs))
        }

    @Test
    fun GameTestState.carriedEmptyContainersAreUsedBeforeBuyingOnlyTheMissingRemainder() =
        game { d ->
            player.members = true
            player.inv[0] = InvObj(objs.attack_potion_4)
            player.inv[1] = InvObj(objs.empty_vial)
            player.inv[2] = InvObj(objs.empty_vial)
            player.inv[3] = InvObj(objs.coins, 1000)
            player.withProtectedAccess { assertTrue(d.decant.decant(this, 1)) }
            assertEquals(4, player.count(objs.attack_potion_1))
            assertEquals(0, player.count(objs.empty_vial))
            assertEquals(1000 - d.types[objs.empty_vial].cost - 2, player.count(objs.coins))
        }

    @Test
    fun GameTestState.insufficientPaymentAndNonmemberNpcUsePreserveAllInputs() = game { d ->
        player.members = true
        player.inv[0] = InvObj(objs.attack_potion_4)
        player.withProtectedAccess { assertFalse(d.decant.decant(this, 1)) }
        assertEquals(objs.attack_potion_4.id, player.inv[0]?.id)
        player.members = false
        player.inv[1] = InvObj(objs.empty_vial)
        player.withProtectedAccess { assertFalse(d.decant.decant(this, 2)) }
        assertEquals(objs.attack_potion_4.id, player.inv[0]?.id)
        assertEquals(objs.empty_vial.id, player.inv[1]?.id)
    }

    @Test
    fun GameTestState.twoDoseBarbarianAndHunterMixesNeverInventFourDoseFormsOrSwapContainers() =
        game { d ->
            player.members = true
            val mix = BarbarianMixRecipes.all.first()
            player.inv[0] = InvObj(mix.oneDose)
            player.inv[1] = InvObj(mix.oneDose)
            player.withProtectedAccess { assertTrue(d.decant.decant(this, 4)) }
            assertEquals(1, player.count(mix.output))
            assertEquals(1, player.count(objs.empty_vial))
            player.clearInv()
            val fluid = DecantFluids.all.first { it.empty == DecantObjs.jar }
            player.inv[0] = InvObj(fluid.doses[1])
            player.inv[1] = InvObj(objs.empty_vial)
            player.withProtectedAccess { assertFalse(d.decant.decant(this, 1)) }
            assertEquals(1, player.count(fluid.doses[1]))
            player.inv[2] = InvObj(DecantObjs.jar)
            player.withProtectedAccess { assertTrue(d.decant.decant(this, 1)) }
            assertEquals(2, player.count(fluid.doses[0]))
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(0, player.count(DecantObjs.jar))
        }

    @Test
    fun GameTestState.oilDecantingAndMixedFamiliesUseSharedFreedVialsWithoutFalseFees() =
        game { d ->
            player.members = true
            for (slot in 0..3) player.inv[slot] = InvObj(DecantObjs.olive[0])
            player.inv[4] = InvObj(objs.attack_potion_4)
            // Four olive vials collapse to two; one spare holds the extra attack container.
            player.withProtectedAccess { assertTrue(d.decant.decant(this, 2)) }
            assertEquals(2, player.count(DecantObjs.olive[1]))
            assertEquals(2, player.count(objs.attack_potion_2))
            assertEquals(1, player.count(objs.empty_vial))
        }

    @Test
    fun GameTestState.bobAndZahurHaveTheirNativeDecantOperations() = game { d ->
        assertEquals("Decant", npcTypes[DecantNpcs.bob].op[3])
        assertEquals("Decant", npcTypes[ZahurNpcs.zahur].op[2])
        assertTrue(d.events.contains(NpcEvents.Op4::class.java, DecantNpcs.bob.id))
        assertTrue(d.events.contains(NpcEvents.Op3::class.java, ZahurNpcs.zahur.id))
    }

    @Test
    fun GameTestState.freeWorldStrengthPotionDoseCombiningIsAllowed() = game { d ->
        player.members = false
        player.inv[0] = InvObj(objs.strength_potion_1)
        player.inv[1] = InvObj(objs.strength_potion_1)
        player.withProtectedAccess { assertTrue(d.decant.combine(this, 0, 1)) }
        assertEquals(objs.empty_vial.id, player.inv[0]?.id)
        assertEquals(objs.strength_potion_2.id, player.inv[1]?.id)
    }

    private fun note(d: Deps, obj: ObjType): ObjType = d.types.getValue(d.types[obj].certlink)

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            null,
            PotionDecantingScript::class,
            BobBarterDecanting::class,
            ZahurScript::class,
            testBody = body,
        )

    data class Deps
    @Inject
    constructor(
        val decant: PotionDecanting,
        val types: ObjTypeList,
        val held: HeldInteractions,
        val use: HeldUInteractions,
        val events: EventBus,
    )
}
