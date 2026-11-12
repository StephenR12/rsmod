package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.FoodConsumptionRepository
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianMixDrinkingTest {
    @Test
    fun GameTestState.`cache 233 pins every Barbarian mix dose and Drink dispatch`() =
        runMixTest { deps ->
            assertEquals(
                listOf(
                    11429 to 11431,
                    11433 to 11435,
                    11437 to 11439,
                    11443 to 11441,
                    11449 to 11451,
                    11453 to 11455,
                    11457 to 11459,
                    11461 to 11463,
                    11445 to 11447,
                    11465 to 11467,
                    11469 to 11471,
                    11473 to 11475,
                    11477 to 11479,
                    11481 to 11483,
                    11517 to 11519,
                    11485 to 11487,
                    11489 to 11491,
                    11493 to 11495,
                    11497 to 11499,
                    11501 to 11503,
                    11505 to 11507,
                    11509 to 11511,
                    11513 to 11515,
                    11521 to 11523,
                    12633 to 12635,
                    11960 to 11962,
                    26350 to 26353,
                    21994 to 21997,
                    22221 to 22224,
                ),
                BarbarianMixRecipes.all.map { it.output.id to it.oneDose.id },
            )
            assertEquals(
                listOf(3, 3, 3, 3, 3, 3) + List(2) { 6 } + listOf(3) + List(20) { 6 },
                BarbarianMixRecipes.all.map { it.heal },
            )
            for (recipe in BarbarianMixRecipes.all) {
                for (dose in listOf(recipe.output, recipe.oneDose)) {
                    assertEquals("Drink", deps.objs[dose].iop[0])
                    assertTrue(deps.objs[dose].members)
                    assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.id))
                }
            }
            assertEquals(829, seqs.human_eat.id)
        }

    @Test
    fun GameTestState.`every mix consumes both doses in place and heals its dated amount`() =
        runMixTest { deps ->
            for (recipe in BarbarianMixRecipes.all) {
                prepare(deps)
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[SLOT] = InvObj(recipe.output)
                player.setBaseLevel(stats.hitpoints, 99)
                player.setCurrentLevel(stats.hitpoints, 50)

                drink(deps)

                assertEquals(recipe.oneDose.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertMessageSent(BarbarianMixDrinking.DRINK_MESSAGE)
                assertMessageSent("You have 1 dose of potion left.")
                assertEquals(50 + recipe.heal, player.stat(stats.hitpoints))

                advance(BarbarianMixDrinking.FOOD_DELAY_CYCLES)
                client.clear()
                drink(deps)

                assertEquals(objs.empty_vial.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertMessageSent(BarbarianMixDrinking.DRINK_MESSAGE)
                assertMessageSent(BarbarianMixDrinking.FINISHED_MESSAGE)
                assertEquals(50 + recipe.heal * 2, player.stat(stats.hitpoints))
            }
        }

    @Test
    fun GameTestState.`mixes share food timing and extend only an active attack delay`() =
        runMixTest { deps ->
            prepare(deps)
            player.inv[SLOT] = InvObj(objs.attack_mix_2)
            player.inv[SECOND_SLOT] = InvObj(objs.strength_mix_2)
            player.actionDelay = player.currentMapClock + 5

            drink(deps)

            assertEquals(player.currentMapClock + 8, player.actionDelay)
            assertEquals(3, deps.food.remainingCycles(player))
            drink(deps, SECOND_SLOT)
            assertEquals(objs.strength_mix_2.id, player.inv[SECOND_SLOT]?.id)
            advance(3)
            drink(deps, SECOND_SLOT)
            assertEquals(objs.strength_mix_1.id, player.inv[SECOND_SLOT]?.id)

            advance(3)
            player.inv[SLOT] = InvObj(objs.attack_mix_2)
            player.actionDelay = player.currentMapClock
            drink(deps)
            assertEquals(player.currentMapClock, player.actionDelay)
        }

    @Test
    fun GameTestState.`final mix dose supports vial smashing and free worlds fail atomically`() =
        runMixTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.attack_mix_1)
            drink(deps)
            assertNull(player.inv[SLOT])

            advance(3)
            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(objs.attack_mix_2)
            drink(deps)
            assertEquals(objs.attack_mix_2.id, player.inv[SLOT]?.id)
            assertEquals(0, deps.food.remainingCycles(player))
            assertMessageSent(BarbarianMixDrinking.MEMBERS_MESSAGE)
        }

    private fun GameTestScope.prepare(deps: Dependencies, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        deps.food.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies, slot: Int = SLOT) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestState.runMixTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val food: FoodConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private companion object {
        const val SLOT = 6
        const val SECOND_SLOT = 12
    }
}
