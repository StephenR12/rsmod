package org.rsmod.content.skills.runecraft

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Execution(ExecutionMode.SAME_THREAD)
class BloodEssenceTest {
    @Test
    fun GameTestState.`cache 233 blood essence variants and charge storage match contracts`() =
        runBloodEssenceTest { _ ->
            val inactive = objTypes[RunecraftObjs.bloodEssenceInactive]
            val active = objTypes[RunecraftObjs.bloodEssenceActive]

            assertEquals(26390, inactive.id)
            assertEquals(26392, active.id)
            assertTrue(inactive.members)
            assertTrue(active.members)
            assertTrue(inactive.stackable)
            assertFalse(active.stackable)
            assertTrue(inactive.tradeable)
            assertFalse(active.tradeable)
            assertEquals("Activate", inactive.iop[0])
            assertEquals("Check", active.iop[0])
            assertEquals("Destroy", active.iop[4])
            assertEquals(65523, varobjs.blood_essence_charges.id)
            assertEquals(0..9, varobjs.blood_essence_charges.bits)
        }

    @Test
    fun GameTestState.`registered held operations activate and check blood essence`() =
        runBloodEssenceTest { _ ->
            preparePlayer()
            player.inv[0] = InvObj(RunecraftObjs.bloodEssenceInactive, 3)

            player.withProtectedAccess { opHeld1(0) }

            assertEquals(2, player.count(RunecraftObjs.bloodEssenceInactive))
            val activeSlot =
                player.inv.indices.single {
                    player.inv[it].isType(RunecraftObjs.bloodEssenceActive)
                }
            val active = player.inv.getValue(activeSlot)
            assertEquals(1_000, BloodEssenceState.fromActive(active).charges)
            assertEquals(146, client.filterIsInstance<SynthSound>().single().id)

            player.withProtectedAccess { opHeld1(activeSlot) }
            assertMessageSent("Your blood essence has 1000 charges remaining.")
        }

    @Test
    fun GameTestState
        .`activation enforces membership uniqueness and inventory capacity atomically`() =
        runBloodEssenceTest { deps ->
            preparePlayer()
            player.members = false
            player.inv[0] = InvObj(RunecraftObjs.bloodEssenceInactive)
            player.withProtectedAccess { deps.lifecycle.activate(this, 0) }
            assertEquals(1, player.count(RunecraftObjs.bloodEssenceInactive))
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer()
            player.inv[0] = InvObj(RunecraftObjs.bloodEssenceInactive, 2)
            player.inv[1] = activeEssence(500)
            player.withProtectedAccess { deps.lifecycle.activate(this, 0) }
            assertEquals(2, player.count(RunecraftObjs.bloodEssenceInactive))
            assertMessageSent(BloodEssenceLifecycle.ONLY_ONE_ACTIVE_MESSAGE)

            preparePlayer()
            player.inv[0] = InvObj(RunecraftObjs.bloodEssenceInactive)
            player.withProtectedAccess {
                bank[0] = activeEssence(500)
                deps.lifecycle.activate(this, 0)
            }
            assertEquals(1, player.count(RunecraftObjs.bloodEssenceInactive))
            assertMessageSent(BloodEssenceLifecycle.ONLY_ONE_ACTIVE_MESSAGE)

            preparePlayer()
            player.inv[0] = InvObj(RunecraftObjs.bloodEssenceInactive, 2)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess { deps.lifecycle.activate(this, 0) }
            assertEquals(2, player.count(RunecraftObjs.bloodEssenceInactive))
            assertEquals(0, player.count(RunecraftObjs.bloodEssenceActive))
            assertMessageSent("You don't have enough inventory space to do that.")
        }

    @Test
    fun GameTestState
        .`true blood altar rolls once per essence consumes only successful charges and adds no xp`() =
        runBloodEssenceTest { deps ->
            prepareBloodCraft()
            repeat(4) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            player.inv[4] = activeEssence(1_000)
            random.next = 0
            random.then = 1
            random.then = 0
            random.then = 1
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.blood) }

            assertEquals(6, player.count(StandardRuneRecipes.blood.output))
            assertEquals(420, player.statMap.getFineXP(stats.runecrafting) - startXp)
            val active = player.inv.first { it.isType(RunecraftObjs.bloodEssenceActive) }
            assertEquals(998, BloodEssenceState.fromActive(requireNotNull(active)).charges)
            assertEquals(0 until 2, random.lastInclusiveRange)
        }

    @Test
    fun GameTestState
        .`blood essence is additive with extracts and is destroyed exactly at zero charges`() =
        runBloodEssenceTest { deps ->
            prepareBloodCraft()
            repeat(2) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            player.inv[2] = InvObj(RunecraftObjs.scarredExtract, 2)
            player.inv[3] = activeEssence(2)
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 1 }
            random.next = 0
            random.then = 0
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.blood) }

            assertEquals(124, player.count(StandardRuneRecipes.blood.output))
            assertEquals(210, player.statMap.getFineXP(stats.runecrafting) - startXp)
            assertEquals(0, player.count(RunecraftObjs.scarredExtract))
            assertEquals(0, player.count(RunecraftObjs.bloodEssenceActive))
            assertNull(player.inv.firstOrNull { it.isType(RunecraftObjs.bloodEssenceActive) })
        }

    @Test
    fun GameTestState.`active essence never rolls or changes on non blood altars`() =
        runBloodEssenceTest { deps ->
            preparePlayer()
            player.stats[stats.runecrafting] = 99
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.inv[1] = activeEssence(321)

            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.nature) }

            assertEquals(2, player.count(StandardRuneRecipes.nature.output))
            val active = player.inv.first { it.isType(RunecraftObjs.bloodEssenceActive) }
            assertEquals(321, BloodEssenceState.fromActive(requireNotNull(active)).charges)
            assertNull(random.lastInclusiveRange)
        }

    private fun GameTestScope.preparePlayer() {
        player.members = true
        player.clearInv()
        player.withProtectedAccess { bank.fillNulls() }
        client.clear()
    }

    private fun GameTestScope.prepareBloodCraft() {
        preparePlayer()
        player.stats[stats.runecrafting] = 99
        player.withProtectedAccess {
            vars[RunecraftVarbits.sinsOfTheFather] =
                requireNotNull(StandardRuneRecipes.blood.quest).completionValue
        }
    }

    private fun activeEssence(charges: Int): InvObj =
        InvObj(RunecraftObjs.bloodEssenceActive, vars = BloodEssenceState(charges).pack())

    private fun GameTestState.runBloodEssenceTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            RunecraftScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(RunecraftModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val lifecycle: BloodEssenceLifecycle, val standard: StandardRuneCrafting)
}
