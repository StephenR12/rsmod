package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
class AntipoisonPotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 antipoison doses and free to play Drink dispatch are pinned`() =
        runAntipoisonTest { deps ->
            assertEquals(
                listOf(2446, 175, 177, 179),
                AntipoisonPotionDoses.all.map { it.potion.id },
            )
            assertEquals(
                listOf(175, 177, 179, 229),
                AntipoisonPotionDoses.all.map { it.replacement.id },
            )
            for (dose in AntipoisonPotionDoses.all) {
                assertEquals("Drink", deps.objs[dose.potion].iop[0])
                assertFalse(deps.objs[dose.potion].members)
                assertFalse(dose.membersOnly)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`antipoison consumes every dose in a full inventory on a free world`() =
        runAntipoisonTest { deps ->
            prepare(deps, members = false)
            repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
            player.inv[SLOT] = InvObj(objs.antipoison_4)

            for (expected in AntipoisonPotionDoses.all) {
                client.clear()
                drink(deps)

                assertEquals(expected.replacement.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertMessageSent(expected.drinkMessage)
                if (expected.dosesRemaining == 0) {
                    assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
                } else {
                    val doseWord = if (expected.dosesRemaining == 1) "dose" else "doses"
                    assertMessageSent(
                        "You have ${expected.dosesRemaining} $doseWord of potion left."
                    )
                    advance(StandardPotionDrinking.POTION_DELAY_CYCLES)
                }
            }
        }

    @Test
    fun GameTestState.`standard and goading potions share the independent three tick clock`() =
        runAntipoisonTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.antipoison_4)
            player.inv[SECOND_SLOT] = InvObj(objs.goading_potion_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.goading_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.goading_potion_3.id, player.inv[SECOND_SLOT]?.id)
        }

    @Test
    fun GameTestState.`last antipoison dose honors automatic vial smashing`() =
        runAntipoisonTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.antipoison_1)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
        }

    @Test
    fun GameTestState.`antipoison rejects a stale selected slot atomically`() =
        runAntipoisonTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.logs)

            player.withProtectedAccess {
                deps.drinking.drink(this, SLOT, AntipoisonPotionDoses.all.first())
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

    private fun GameTestState.runAntipoisonTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val drinking: StandardPotionDrinking,
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
