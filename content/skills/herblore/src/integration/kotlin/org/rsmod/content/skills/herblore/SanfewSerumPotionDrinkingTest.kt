package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class SanfewSerumPotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 Sanfew serum identities and dispatch are pinned`() =
        runSanfewTest { deps ->
            assertEquals(
                listOf(10925, 10927, 10929, 10931),
                SanfewSerumPotionDoses.all.map { it.potion.id },
            )
            assertEquals(
                listOf(10927, 10929, 10931, objs.empty_vial.id),
                SanfewSerumPotionDoses.all.map { it.replacement.id },
            )
            assertEquals(
                listOf(
                    "A 4 dose Sanfew Serum.",
                    "A 3 dose Sanfew Serum.",
                    "A 2 dose Sanfew Serum.",
                    "A 1 dose Sanfew Serum.",
                ),
                SanfewSerumPotionDoses.all.map { deps.objs[it.potion].desc },
            )
            for (dose in SanfewSerumPotionDoses.all) {
                assertEquals("Drink", deps.objs[dose.potion].iop[0])
                assertTrue(deps.objs[dose.potion].members)
                assertTrue(dose.membersOnly)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`Sanfew serum consumes every dose in a full inventory`() =
        runSanfewTest { deps ->
            prepare(deps)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
            player.inv[SLOT] = InvObj(objs.sanfew_serum_4)

            for (expected in SanfewSerumPotionDoses.all) {
                client.clear()
                drink(deps)

                assertEquals(expected.replacement.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertMessageSent(expected.drinkMessage)
                if (expected.dosesRemaining == 0) {
                    assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
                } else {
                    val word = if (expected.dosesRemaining == 1) "dose" else "doses"
                    assertMessageSent("You have ${expected.dosesRemaining} $word of potion left.")
                    advance(StandardPotionDrinking.POTION_DELAY_CYCLES)
                }
            }
        }

    @Test
    fun GameTestState.`Sanfew serum final dose honors automatic vial smashing`() =
        runSanfewTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.sanfew_serum_1)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
        }

    @Test
    fun GameTestState.`Sanfew serum rejects free worlds atomically`() = runSanfewTest { deps ->
        prepare(deps, members = false)
        player.inv[SLOT] = InvObj(objs.sanfew_serum_4)

        drink(deps)

        assertEquals(objs.sanfew_serum_4.id, player.inv[SLOT]?.id)
        assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
        assertEquals(0, deps.potions.remainingCycles(player))
    }

    @Test
    fun GameTestState.`Sanfew serum shares the standard independent potion clock`() =
        runSanfewTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.antipoison_4)
            player.inv[SECOND_SLOT] = InvObj(objs.sanfew_serum_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.sanfew_serum_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.sanfew_serum_3.id, player.inv[SECOND_SLOT]?.id)
        }

    private fun GameTestScope.prepare(deps: Dependencies, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        deps.potions.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies, slot: Int = SLOT) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestState.runSanfewTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val potions: PotionConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private companion object {
        const val SLOT = 6
        const val SECOND_SLOT = 12
    }
}
