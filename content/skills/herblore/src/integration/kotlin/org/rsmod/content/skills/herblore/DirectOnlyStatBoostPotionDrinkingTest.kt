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
class DirectOnlyStatBoostPotionDrinkingTest {
    @Test
    fun GameTestState
        .`cache 233 combat and hunter identities descriptions and dispatch are pinned`() =
        runPotionTest { deps ->
            val expectedIds =
                listOf(listOf(9739, 9741, 9743, 9745), listOf(9998, 10000, 10002, 10004))
            val expectedReplacements = expectedIds.map { it.drop(1) + objs.empty_vial.id }
            val expectedDescriptions = listOf("combat potion", "Hunter potion")

            assertEquals(
                expectedIds,
                DirectOnlyStatBoostPotionDoses.families.map { family ->
                    family.map { it.potion.id }
                },
            )
            assertEquals(
                expectedReplacements,
                DirectOnlyStatBoostPotionDoses.families.map { family ->
                    family.map { it.replacement.id }
                },
            )
            for ((family, description) in
                DirectOnlyStatBoostPotionDoses.families.zip(expectedDescriptions)) {
                assertEquals(
                    listOf(
                        "4 doses of $description.",
                        "3 doses of $description.",
                        "2 doses of $description.",
                        "1 dose of $description.",
                    ),
                    family.map { deps.objs[it.potion].desc },
                )
                for (dose in family) {
                    assertEquals("Drink", deps.objs[dose.potion].iop[0])
                    assertTrue(deps.objs[dose.potion].members)
                    assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
                }
            }
        }

    @Test
    fun GameTestState.`combat and hunter potions consume every dose in a full inventory`() =
        runPotionTest { deps ->
            for (family in DirectOnlyStatBoostPotionDoses.families) {
                prepare(deps)
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[SLOT] = InvObj(family.first().potion)

                for (expected in family) {
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
                        assertMessageSent(
                            "You have ${expected.dosesRemaining} $word of potion left."
                        )
                        advance(StandardPotionDrinking.POTION_DELAY_CYCLES)
                    }
                }
            }
        }

    @Test
    fun GameTestState.`combat and hunter final doses honor automatic vial smashing`() =
        runPotionTest { deps ->
            for (family in DirectOnlyStatBoostPotionDoses.families) {
                prepare(deps)
                player.setVarBit(varbits.auto_smash_vials, 1)
                player.inv[SLOT] = InvObj(family.last().potion)

                drink(deps)

                assertEquals(null, player.inv[SLOT])
                assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
            }
        }

    @Test
    fun GameTestState.`combat and hunter potions reject free worlds without mutation`() =
        runPotionTest { deps ->
            for (family in DirectOnlyStatBoostPotionDoses.families) {
                prepare(deps, members = false)
                player.inv[SLOT] = InvObj(family.first().potion)

                drink(deps)

                assertEquals(family.first().potion.id, player.inv[SLOT]?.id)
                assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
                assertEquals(0, deps.potions.remainingCycles(player))
            }
        }

    @Test
    fun GameTestState.`combat and hunter share the independent standard potion clock`() =
        runPotionTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.combat_potion_4)
            player.inv[SECOND_SLOT] = InvObj(objs.hunter_potion_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.hunter_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.hunter_potion_3.id, player.inv[SECOND_SLOT]?.id)
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

    private fun GameTestState.runPotionTest(body: GameTestScope.(Dependencies) -> Unit) =
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
