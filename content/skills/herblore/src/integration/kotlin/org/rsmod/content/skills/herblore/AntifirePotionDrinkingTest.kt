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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.dbtable.DbTableTypeList
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class AntifirePotionDrinkingTest {
    @Test
    fun GameTestState.`cache 233 antifire identities quest boundary and dispatch are pinned`() =
        runAntifireTest { deps ->
            val expectedIds =
                listOf(
                    listOf(2452, 2454, 2456, 2458),
                    listOf(11951, 11953, 11955, 11957),
                    listOf(21978, 21981, 21984, 21987),
                    listOf(22209, 22212, 22215, 22218),
                )
            val expectedReplacements = expectedIds.map { it.drop(1) + objs.empty_vial.id }
            val expectedDescriptionStems =
                listOf(
                    "anti-firebreath potion",
                    "extended anti-firebreath potion",
                    "super anti-firebreath potion",
                    "extended super anti-firebreath potion",
                )

            assertEquals(
                expectedIds,
                AntifirePotionDoses.families.map { family -> family.map { it.potion.id } },
            )
            assertEquals(
                expectedReplacements,
                AntifirePotionDoses.families.map { family -> family.map { it.replacement.id } },
            )
            for ((family, stem) in AntifirePotionDoses.families.zip(expectedDescriptionStems)) {
                assertEquals(
                    listOf(
                        "4 doses of $stem.",
                        "3 doses of $stem.",
                        "2 doses of $stem.",
                        "1 dose of $stem.",
                    ),
                    family.map { deps.objs[it.potion].desc },
                )
            }
            assertEquals(176, varps.dragonquest.id)
            assertEquals(listOf(0), deps.dbTables.types.getValue(QUEST_TABLE).defaults[18])
            assertEquals(null, deps.dbRows.types.getValue(DRAGON_SLAYER_I_ROW).data[18])
            assertEquals(listOf(10), deps.dbRows.types.getValue(DRAGON_SLAYER_I_ROW).data[19])
            for (dose in AntifirePotionDoses.all) {
                assertEquals("Drink", deps.objs[dose.potion].iop[0])
                assertTrue(deps.objs[dose.potion].members)
                assertTrue(dose.membersOnly)
                assertEquals(
                    AntifirePotionDoses.QUEST_START_STATE,
                    dose.questRequirement?.minimumState,
                )
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`every antifire family consumes all doses in a full inventory`() =
        runAntifireTest { deps ->
            for (family in AntifirePotionDoses.families) {
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
    fun GameTestState.`every antifire family final dose honors automatic vial smashing`() =
        runAntifireTest { deps ->
            for (family in AntifirePotionDoses.families) {
                prepare(deps)
                player.setVarBit(varbits.auto_smash_vials, 1)
                player.inv[SLOT] = InvObj(family.last().potion)

                drink(deps)

                assertEquals(null, player.inv[SLOT])
                assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)
            }
        }

    @Test
    fun GameTestState.`every antifire family rejects free worlds atomically`() =
        runAntifireTest { deps ->
            for (family in AntifirePotionDoses.families) {
                prepare(deps, members = false)
                player.inv[SLOT] = InvObj(family.first().potion)

                drink(deps)

                assertEquals(family.first().potion.id, player.inv[SLOT]?.id)
                assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
                assertEquals(0, deps.potions.remainingCycles(player))
            }
        }

    @Test
    fun GameTestState.`every antifire family requires Dragon Slayer I to be started`() =
        runAntifireTest { deps ->
            for (family in AntifirePotionDoses.families) {
                prepare(deps, questState = 0)
                player.inv[SLOT] = InvObj(family.first().potion)

                drink(deps)

                assertEquals(family.first().potion.id, player.inv[SLOT]?.id)
                assertMessageSent(AntifirePotionDoses.QUEST_FAILURE_MESSAGE)
                assertEquals(0, deps.potions.remainingCycles(player))

                player.setVarp(varps.dragonquest, AntifirePotionDoses.QUEST_START_STATE)
                client.clear()
                drink(deps)
                assertEquals(family[1].potion.id, player.inv[SLOT]?.id)
            }
        }

    @Test
    fun GameTestState.`all antifire families share the independent standard potion clock`() =
        runAntifireTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.antifire_potion_4)
            player.inv[SECOND_SLOT] = InvObj(objs.extended_super_antifire_4)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 5, player.actionDelay)
            assertEquals(3, deps.potions.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.extended_super_antifire_4.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.extended_super_antifire_3.id, player.inv[SECOND_SLOT]?.id)
        }

    private fun GameTestScope.prepare(
        deps: Dependencies,
        members: Boolean = true,
        questState: Int = AntifirePotionDoses.QUEST_START_STATE,
    ) {
        player.clearAllInvs()
        player.members = members
        player.actionDelay = player.currentMapClock
        player.setVarp(varps.dragonquest, questState)
        player.setVarBit(varbits.auto_smash_vials, 0)
        deps.potions.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies, slot: Int = SLOT) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestState.runAntifireTest(body: GameTestScope.(Dependencies) -> Unit) =
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
        val dbRows: DbRowTypeList,
        val dbTables: DbTableTypeList,
    )

    private companion object {
        const val QUEST_TABLE = 0
        const val DRAGON_SLAYER_I_ROW = 31
        const val SLOT = 6
        const val SECOND_SLOT = 12
    }
}
