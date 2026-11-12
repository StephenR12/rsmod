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
class StatRestorePotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 restoration identities descriptions and dispatch are pinned`() =
        runPotionTest { deps ->
            val expectedIds =
                listOf(
                    listOf(2430, 127, 129, 131),
                    listOf(3008, 3010, 3012, 3014),
                    listOf(2434, 139, 141, 143),
                    listOf(3016, 3018, 3020, 3022),
                    listOf(3024, 3026, 3028, 3030),
                    listOf(12625, 12627, 12629, 12631),
                )
            val expectedReplacements = expectedIds.map { it.drop(1) + objs.empty_vial.id }
            val expectedDescriptions =
                listOf(
                    "restore potion",
                    "energy potion",
                    "Prayer restore potion",
                    "super energy potion",
                    "super restore potion",
                    "stamina potion",
                )

            assertEquals(
                expectedIds,
                StatRestorePotionDoses.families.map { family -> family.map { it.potion.id } },
            )
            assertEquals(
                expectedReplacements,
                StatRestorePotionDoses.families.map { family -> family.map { it.replacement.id } },
            )
            for ((family, description) in
                StatRestorePotionDoses.families.zip(expectedDescriptions)) {
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
                    assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
                }
            }
            assertFalse(StatRestorePotionDoses.energy.any { it.membersOnly })
            assertTrue(
                StatRestorePotionDoses.families
                    .filterNot { it === StatRestorePotionDoses.energy }
                    .all { family -> family.all { it.membersOnly } }
            )
        }

    @Test
    fun GameTestState.`every restoration family consumes all doses in a full inventory`() =
        runPotionTest { deps ->
            for (family in StatRestorePotionDoses.families) {
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
    fun GameTestState.`every restoration family final dose honors automatic vial smashing`() =
        runPotionTest { deps ->
            for (family in StatRestorePotionDoses.families) {
                prepare(deps)
                player.setVarBit(varbits.auto_smash_vials, 1)
                player.inv[SLOT] = InvObj(family.last().potion)

                drink(deps)

                assertEquals(null, player.inv[SLOT])
                assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
            }
        }

    @Test
    fun GameTestState.`only energy potions are usable on free worlds`() = runPotionTest { deps ->
        for (family in StatRestorePotionDoses.families) {
            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(family.first().potion)

            drink(deps)

            if (family === StatRestorePotionDoses.energy) {
                assertEquals(family[1].potion.id, player.inv[SLOT]?.id)
            } else {
                assertEquals(family.first().potion.id, player.inv[SLOT]?.id)
                assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
                assertEquals(0, deps.potions.remainingCycles(player))
            }
        }
    }

    @Test
    fun GameTestState.`all restoration families share the independent standard potion clock`() =
        runPotionTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.restore_potion_4)
            player.inv[SECOND_SLOT] = InvObj(objs.stamina_potion_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.stamina_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.stamina_potion_3.id, player.inv[SECOND_SLOT]?.id)
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
