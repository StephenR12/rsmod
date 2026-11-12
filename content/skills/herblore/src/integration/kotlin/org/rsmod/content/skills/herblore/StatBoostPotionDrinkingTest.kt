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
class StatBoostPotionDrinkingTest {
    @Test
    fun GameTestState
        .`cache 233 stat boost identities descriptions access and dispatch are pinned`() =
        runPotionTest { deps ->
            val expectedIds =
                listOf(
                    listOf(2428, 121, 123, 125),
                    listOf(113, 115, 117, 119),
                    listOf(2432, 133, 135, 137),
                    listOf(2444, 169, 171, 173),
                    listOf(3040, 3042, 3044, 3046),
                    listOf(2436, 145, 147, 149),
                    listOf(2440, 157, 159, 161),
                    listOf(2442, 163, 165, 167),
                    listOf(12695, 12697, 12699, 12701),
                    listOf(22461, 22464, 22467, 22470),
                    listOf(22449, 22452, 22455, 22458),
                    listOf(3032, 3034, 3036, 3038),
                    listOf(2438, 151, 153, 155),
                )
            val expectedReplacements = expectedIds.map { it.drop(1) + objs.empty_vial.id }
            val expectedDescriptions =
                listOf(
                    "Attack potion",
                    "Strength potion",
                    "Defence potion",
                    "ranging potion",
                    "Magic potion",
                    "super Attack potion",
                    "super Strength potion",
                    "super Defence potion",
                    "super combat potion",
                    "bastion potion",
                    "battlemage potion",
                    "Agility potion",
                    "Fishing potion",
                )

            assertEquals(
                expectedIds,
                StatBoostPotionDoses.families.map { family -> family.map { it.potion.id } },
            )
            assertEquals(
                expectedReplacements,
                StatBoostPotionDoses.families.map { family -> family.map { it.replacement.id } },
            )
            assertEquals(
                expectedDescriptions.map { description ->
                    listOf(
                        "4 doses of $description.",
                        "3 doses of $description.",
                        "2 doses of $description.",
                        "1 dose of $description.",
                    )
                },
                StatBoostPotionDoses.families.map { family ->
                    family.map { deps.objs[it.potion].desc }
                },
            )
            for (family in StatBoostPotionDoses.families) {
                for (dose in family) {
                    assertEquals("Drink", deps.objs[dose.potion].iop[0])
                    assertEquals(dose.membersOnly, deps.objs[dose.potion].members)
                    assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
                }
            }
            assertTrue(StatBoostPotionDoses.strength.all { !it.membersOnly })
            assertTrue(
                StatBoostPotionDoses.families
                    .filterNot { it === StatBoostPotionDoses.strength }
                    .all { family -> family.all { it.membersOnly } }
            )
        }

    @Test
    fun GameTestState.`every stat boost family consumes all doses in a full inventory`() =
        runPotionTest { deps ->
            for (family in StatBoostPotionDoses.families) {
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
    fun GameTestState.`every stat boost family final dose honors automatic vial smashing`() =
        runPotionTest { deps ->
            for (family in StatBoostPotionDoses.families) {
                prepare(deps)
                player.setVarBit(varbits.auto_smash_vials, 1)
                player.inv[SLOT] = InvObj(family.last().potion)

                drink(deps)

                assertEquals(null, player.inv[SLOT])
                assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
            }
        }

    @Test
    fun GameTestState.`only strength potions are usable on free worlds`() = runPotionTest { deps ->
        for (family in StatBoostPotionDoses.families) {
            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(family.first().potion)

            drink(deps)

            if (family === StatBoostPotionDoses.strength) {
                assertEquals(family[1].potion.id, player.inv[SLOT]?.id)
            } else {
                assertEquals(family.first().potion.id, player.inv[SLOT]?.id)
                assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
                assertEquals(0, deps.potions.remainingCycles(player))
            }
        }
    }

    @Test
    fun GameTestState.`all stat boost families share the independent standard potion clock`() =
        runPotionTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.attack_potion_4)
            player.inv[SECOND_SLOT] = InvObj(objs.battlemage_potion_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.battlemage_potion_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.battlemage_potion_3.id, player.inv[SECOND_SLOT]?.id)
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
