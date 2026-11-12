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
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.varbit.VarBitTypeList

@Execution(ExecutionMode.SAME_THREAD)
class GoadingPotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 goading doses and Drink dispatch are pinned`() =
        runGoadingTest { deps ->
            assertEquals(
                listOf(30137, 30140, 30143, 30146),
                GoadingPotionDoses.all.map { it.potion.id },
            )
            assertEquals(
                listOf(30140, 30143, 30146, 229),
                GoadingPotionDoses.all.map { it.replacement?.id },
            )
            assertEquals(829, seqs.human_eat.id)
            assertEquals(5668, varbits.auto_smash_vials.id)
            for (dose in GoadingPotionDoses.all) {
                assertEquals("Drink", deps.objs[dose.potion].iop[0])
                assertTrue(deps.objs[dose.potion].members)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`goading potion consumes every dose with exact messages and animation`() =
        runGoadingTest { deps ->
            prepare(deps)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
            player.inv[SLOT] = InvObj(objs.goading_potion_4)

            for (expected in GoadingPotionDoses.all) {
                client.clear()
                drink(deps)

                assertEquals(expected.replacement?.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertMessageSent(GoadingPotionDrinking.DRINK_MESSAGE)
                if (expected.dosesRemaining == 0) {
                    assertMessageSent(GoadingPotionDrinking.FINISHED_MESSAGE)
                } else {
                    val doseWord = if (expected.dosesRemaining == 1) "dose" else "doses"
                    assertMessageSent(
                        "You have ${expected.dosesRemaining} $doseWord of potion left."
                    )
                    advance(GoadingPotionDrinking.POTION_DELAY_CYCLES)
                }
            }
        }

    @Test
    fun GameTestState
        .`goading potion uses an independent three tick timer and never delays attacks`() =
        runGoadingTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.goading_potion_4)
            player.inv[SECOND_SLOT] = InvObj(objs.goading_potion_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.goading_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance(2)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.goading_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance()
            drink(deps, SECOND_SLOT)
            assertEquals(objs.goading_potion_3.id, player.inv[SECOND_SLOT]?.id)
        }

    @Test
    fun GameTestState.`last goading dose honors automatic vial smashing`() =
        runGoadingTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.goading_potion_1)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertMessageSent(GoadingPotionDrinking.FINISHED_MESSAGE)
        }

    @Test
    fun GameTestState.`goading potion rejects free worlds and stale slots atomically`() =
        runGoadingTest { deps ->
            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(objs.goading_potion_4)
            drink(deps)
            assertEquals(objs.goading_potion_4.id, player.inv[SLOT]?.id)
            assertMessageSent(GoadingPotionDrinking.MEMBERS_MESSAGE)
            assertEquals(0, deps.potions.remainingCycles(player))

            prepare(deps)
            player.inv[SLOT] = InvObj(objs.logs)
            player.withProtectedAccess {
                deps.drinking.drink(this, SLOT, GoadingPotionDoses.all.first())
            }
            assertEquals(objs.logs.id, player.inv[SLOT]?.id)
            assertEquals(0, deps.potions.remainingCycles(player))
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

    private fun GameTestState.runGoadingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val drinking: GoadingPotionDrinking,
        val potions: PotionConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
        val varbits: VarBitTypeList,
    )

    private companion object {
        const val SLOT = 6
        const val SECOND_SLOT = 12
    }
}
