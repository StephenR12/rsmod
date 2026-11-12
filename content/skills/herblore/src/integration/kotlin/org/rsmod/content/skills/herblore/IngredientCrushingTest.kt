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
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class IngredientCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins every ordinary one-to-one crushing contract`() =
        runCrushingTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertEquals(11095, seqs.human_herbing_grind_restart.id)
            assertEquals(
                listOf(
                    Contract(237, 235),
                    Contract(1973, 1975),
                    Contract(9735, 9736),
                    Contract(10109, 10111),
                    Contract(243, 241),
                    Contract(5075, 6693),
                    Contract(22124, 21975),
                    Contract(26231, 26368),
                ),
                IngredientCrushingRecipes.ordinary.map { Contract(it.input.id, it.output.id) },
            )
            assertTrue(deps.objs[objs.pestle_and_mortar].members)
            val freeToPlay = setOf(objs.chocolate_bar.id, objs.chocolate_dust.id)
            for (recipe in IngredientCrushingRecipes.ordinary) {
                assertEquals(
                    recipe.input.id !in freeToPlay,
                    deps.objs[recipe.input].members,
                    deps.objs[recipe.input].name,
                )
                assertEquals(
                    recipe.output.id !in freeToPlay,
                    deps.objs[recipe.output].members,
                    deps.objs[recipe.output].name,
                )
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.pestle_and_mortar.id, recipe.input.id),
                    ),
                    deps.objs[recipe.input].name,
                )
            }
        }

    @Test
    fun GameTestState.`cache 233 pins every additional fixed crushing contract`() =
        runCrushingTest { deps ->
            assertEquals(
                listOf(
                    Contract(249, 6681),
                    Contract(973, 704),
                    Contract(3142, 3152),
                    Contract(3146, 3153),
                    Contract(3144, 3154),
                    Contract(4698, 9594),
                    Contract(1550, 4668),
                    Contract(530, 2391),
                    Contract(592, 8865),
                    Contract(6466, 6467),
                    Contract(319, 11266),
                    Contract(3263, 3264),
                    Contract(401, 6683),
                ),
                IngredientCrushingRecipes.additional.map { Contract(it.input.id, it.output.id) },
            )
            for (recipe in IngredientCrushingRecipes.additional) {
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.pestle_and_mortar.id, recipe.input.id),
                    ),
                    deps.objs[recipe.input].name,
                )
            }
        }

    @Test
    fun GameTestState.`additional fixed recipes need no level or Druidic Ritual state`() =
        runCrushingTest { deps ->
            for (recipe in IngredientCrushingRecipes.additional) {
                preparePlayer(level = 1, questState = 0)
                player.inv[0] = InvObj(objs.pestle_and_mortar)
                player.inv[9] = InvObj(recipe.input)
                for (slot in 1 until player.inv.size) {
                    if (slot != 9) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }

                player.withProtectedAccess { deps.crushing.start(this, 9, recipe) }

                assertEquals(recipe.output.id, player.inv[9]?.id, deps.objs[recipe.output].name)
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`every recipe immediately replaces its selected slot with zero xp`() =
        runCrushingTest { deps ->
            for (recipe in IngredientCrushingRecipes.all) {
                preparePlayer()
                player.inv[0] = InvObj(objs.pestle_and_mortar)
                player.inv[9] = InvObj(recipe.input)
                for (slot in 1 until player.inv.size) {
                    if (slot != 9) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }

                player.withProtectedAccess { deps.crushing.start(this, 9, recipe) }

                assertEquals(recipe.output.id, player.inv[9]?.id, deps.objs[recipe.output].name)
                assertEquals(1, player.count(objs.pestle_and_mortar))
                assertEquals(28, player.inv.count { it != null })
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
                assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            }
        }

    @Test
    fun GameTestState.`automatic crushing follows immediate then two and three tick cadence`() =
        runCrushingTest { deps ->
            val recipe = IngredientCrushingRecipes.all.first()
            preparePlayer()
            add(objs.pestle_and_mortar)
            add(recipe.input, 3)

            val firstSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            player.withProtectedAccess { deps.crushing.start(this, firstSlot, recipe) }
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(2, player.count(recipe.output))
            advance(2)
            assertEquals(2, player.count(recipe.output))
            advance()
            assertEquals(3, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.ingredientCrushing))
        }

    @Test
    fun GameTestState.`both held-use orders dispatch the selected ingredient`() =
        runCrushingTest { deps ->
            val recipe = IngredientCrushingRecipes.all.last()
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[2] = InvObj(objs.pestle_and_mortar)
                player.inv[7] = InvObj(recipe.input)

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                            deps.objs[recipe.input],
                            7,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.input],
                            7,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                        )
                    }
                }

                assertEquals(recipe.output.id, player.inv[7]?.id, "reverse=$reverse")
                assertEquals(objs.pestle_and_mortar.id, player.inv[2]?.id, "reverse=$reverse")
            }
        }

    @Test
    fun GameTestState.`knife cuts chocolate in both orders on a free world`() =
        runCrushingTest { deps ->
            assertEquals(946, objs.knife.id)
            assertTrue(!deps.objs[objs.knife].members)
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.knife.id, objs.chocolate_bar.id),
                )
            )
            repeat(2) { reverse ->
                preparePlayer(members = false, level = 1, questState = 0)
                player.inv[2] = InvObj(objs.knife)
                player.inv[9] = InvObj(objs.chocolate_bar)
                for (slot in 0 until player.inv.size) {
                    if (slot != 2 && slot != 9) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.knife],
                            2,
                            deps.objs[objs.chocolate_bar],
                            9,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.chocolate_bar],
                            9,
                            deps.objs[objs.knife],
                            2,
                        )
                    }
                }

                assertEquals(objs.chocolate_dust.id, player.inv[9]?.id, "reverse=$reverse")
                assertEquals(objs.knife.id, player.inv[2]?.id, "reverse=$reverse")
                assertEquals(28, player.inv.count { it != null }, "reverse=$reverse")
                assertEquals(0, player.statMap.getFineXP(stats.herblore), "reverse=$reverse")
                assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            }
        }

    @Test
    fun GameTestState.`knife chocolate uses sourced cadence and requires the starting tool`() =
        runCrushingTest { deps ->
            val recipe = IngredientCrushingRecipes.byInput.getValue(objs.chocolate_bar.id)
            preparePlayer(members = false)
            add(objs.knife)
            add(objs.chocolate_bar, 3)

            val inputSlot = player.inv.indexOfFirst { it?.id == objs.chocolate_bar.id }
            player.withProtectedAccess { deps.crushing.start(this, inputSlot, recipe, objs.knife) }
            assertEquals(1, player.count(objs.chocolate_dust))
            advance()
            assertEquals(1, player.count(objs.chocolate_dust))
            advance()
            assertEquals(2, player.count(objs.chocolate_dust))
            advance(2)
            assertEquals(2, player.count(objs.chocolate_dust))
            advance()
            assertEquals(3, player.count(objs.chocolate_dust))

            preparePlayer(members = false)
            add(objs.knife)
            add(objs.chocolate_bar, 2)
            val stoppedSlot = player.inv.indexOfFirst { it?.id == objs.chocolate_bar.id }
            player.withProtectedAccess {
                deps.crushing.start(this, stoppedSlot, recipe, objs.knife)
            }
            player.inv[player.inv.indexOfFirst { it?.id == objs.knife.id }] = null
            advance(2)
            assertEquals(1, player.count(objs.chocolate_dust))
            assertEquals(1, player.count(objs.chocolate_bar))
            assertEquals(0, player.weakQueueList.count(herblore_queues.ingredientCrushing))
        }

    @Test
    fun GameTestState.`members gate applies without a Druidic Ritual or level gate`() =
        runCrushingTest { deps ->
            val recipe = IngredientCrushingRecipes.all.first()
            preparePlayer(members = false)
            add(objs.pestle_and_mortar)
            add(recipe.input)
            val inputSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            player.withProtectedAccess { deps.crushing.start(this, inputSlot, recipe) }
            assertEquals(1, player.count(recipe.input))
            assertEquals(0, player.count(recipe.output))
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer(level = 1, questState = 0)
            add(objs.pestle_and_mortar)
            add(recipe.input)
            val unrestrictedSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            player.withProtectedAccess { deps.crushing.start(this, unrestrictedSlot, recipe) }
            assertEquals(0, player.count(recipe.input))
            assertEquals(1, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`missing resources and replacement Herblore work cancel safely`() =
        runCrushingTest { deps ->
            val first = IngredientCrushingRecipes.all[0]
            val second = IngredientCrushingRecipes.all[1]
            preparePlayer()
            add(objs.pestle_and_mortar)
            add(first.input, 2)
            val firstSlot = player.inv.indexOfFirst { it?.id == first.input.id }
            player.withProtectedAccess { deps.crushing.start(this, firstSlot, first) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.pestle_and_mortar.id }] = null
            advance(2)
            assertEquals(1, player.count(first.output))
            assertEquals(1, player.count(first.input))
            assertEquals(0, player.weakQueueList.count(herblore_queues.ingredientCrushing))

            preparePlayer()
            add(objs.pestle_and_mortar)
            add(first.input, 2)
            add(second.input)
            val oldSlot = player.inv.indexOfFirst { it?.id == first.input.id }
            val replacementSlot = player.inv.indexOfFirst { it?.id == second.input.id }
            player.withProtectedAccess {
                deps.crushing.start(this, oldSlot, first)
                deps.crushing.start(this, replacementSlot, second)
            }
            advance(2)
            assertEquals(1, player.count(first.output))
            assertEquals(1, player.count(first.input))
            assertEquals(1, player.count(second.output))
        }

    @Test
    fun GameTestState.`stacked inputs require an output destination and never partially mutate`() =
        runCrushingTest { deps ->
            val recipe = IngredientCrushingRecipes.byInput.getValue(objs.nihil_shard.id)
            assertTrue(deps.objs[recipe.input].stackable)
            assertTrue(deps.objs[recipe.output].stackable)
            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(recipe.input, 3)
            for (slot in 1 until player.inv.size) {
                if (slot != 9) {
                    player.inv[slot] = InvObj(objs.coins)
                }
            }

            player.withProtectedAccess { deps.crushing.start(this, 9, recipe) }

            assertEquals(3, player.count(recipe.input))
            assertEquals(0, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.ingredientCrushing))

            player.inv[8] = InvObj(recipe.output, 4)
            player.withProtectedAccess { deps.crushing.start(this, 9, recipe) }

            assertEquals(2, player.count(recipe.input))
            assertEquals(5, player.count(recipe.output))
            assertEquals(recipe.input.id, player.inv[9]?.id)
            assertEquals(2, player.inv[9]?.count)

            val nonStackingOutput = IngredientCrushingRecipes.byInput.getValue(objs.mud_rune.id)
            assertTrue(deps.objs[nonStackingOutput.input].stackable)
            assertTrue(!deps.objs[nonStackingOutput.output].stackable)
            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(nonStackingOutput.input, 2)
            for (slot in 1 until player.inv.size) {
                if (slot != 9) {
                    player.inv[slot] = InvObj(objs.coins)
                }
            }
            player.withProtectedAccess { deps.crushing.start(this, 9, nonStackingOutput) }
            assertEquals(2, player.count(nonStackingOutput.input))
            assertEquals(0, player.count(nonStackingOutput.output))

            player.inv[8] = null
            player.withProtectedAccess { deps.crushing.start(this, 9, nonStackingOutput) }
            assertEquals(1, player.count(nonStackingOutput.input))
            assertEquals(1, player.count(nonStackingOutput.output))
            assertEquals(nonStackingOutput.output.id, player.inv[8]?.id)
        }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, level)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runCrushingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crushing: IngredientCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(val input: Int, val output: Int)
}
