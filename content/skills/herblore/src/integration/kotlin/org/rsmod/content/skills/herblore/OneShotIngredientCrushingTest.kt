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
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class OneShotIngredientCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins all one-shot quest crushing contracts and dispatch`() =
        runCrushingTest { deps ->
            val expected =
                listOf(
                    Triple(objs.dramen_branch, 771, objs.cinnamon to 7472),
                    Triple(objs.tchiki_monkey_nuts, 7573, objs.tchiki_nut_paste to 7575),
                    Triple(objs.mysterious_meat, 22403, objs.mysterious_crushed_meat to 22404),
                    Triple(objs.cooked_karambwanji, 3151, objs.cooked_karambwanji_paste to 3156),
                )
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            for ((input, inputId, output) in expected) {
                assertEquals(inputId, input.id)
                assertEquals(output.second, output.first.id)
                if (input.id != objs.cooked_karambwanji.id) {
                    assertTrue(deps.objs[input].members)
                    assertTrue(deps.objs[output.first].members)
                }
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.pestle_and_mortar.id, input.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`one-shot quest ingredients transform immediately in both orders`() =
        runCrushingTest { deps ->
            for (recipe in OneShotIngredientCrushingRecipes.all) {
                repeat(2) { reverse ->
                    preparePlayer()
                    player.inv[3] = InvObj(objs.pestle_and_mortar)
                    player.inv[8] = InvObj(recipe.input)
                    player.withProtectedAccess {
                        if (reverse == 0) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[objs.pestle_and_mortar],
                                3,
                                deps.objs[recipe.input],
                                8,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[recipe.input],
                                8,
                                deps.objs[objs.pestle_and_mortar],
                                3,
                            )
                        }
                    }
                    assertEquals(
                        recipe.output.id,
                        player.inv[8]?.id,
                        "recipe=$recipe reverse=$reverse",
                    )
                    assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
                    assertEquals(0, player.statMap.getFineXP(stats.herblore))
                }
            }
        }

    @Test
    fun GameTestState.`one-shot recipes have no automatic continuation or quest-skill gate`() =
        runCrushingTest { deps ->
            for (recipe in OneShotIngredientCrushingRecipes.all) {
                preparePlayer(level = 1, questState = 0)
                player.inv[0] = InvObj(objs.pestle_and_mortar)
                player.inv[5] = InvObj(recipe.input)
                player.inv[10] = InvObj(recipe.input)
                player.inv[15] = InvObj(recipe.input)
                player.withProtectedAccess { deps.crushing.start(this, 10, recipe) }
                assertEquals(recipe.output.id, player.inv[10]?.id)
                assertEquals(2, player.count(recipe.input))
                advance(10)
                assertEquals(2, player.count(recipe.input))
                assertEquals(1, player.count(recipe.output))
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`one-shot recipes are full-inventory safe and reject free worlds`() =
        runCrushingTest { deps ->
            for (recipe in OneShotIngredientCrushingRecipes.all) {
                preparePlayer()
                for (slot in player.inv.indices) {
                    player.inv[slot] = InvObj(objs.coins)
                }
                player.inv[4] = InvObj(objs.pestle_and_mortar)
                player.inv[17] = InvObj(recipe.input)
                player.withProtectedAccess { deps.crushing.start(this, 17, recipe) }
                assertEquals(recipe.output.id, player.inv[17]?.id)
                assertEquals(28, player.inv.count { it != null })

                preparePlayer(members = false)
                player.inv[4] = InvObj(objs.pestle_and_mortar)
                player.inv[17] = InvObj(recipe.input)
                player.withProtectedAccess { deps.crushing.start(this, 17, recipe) }
                assertEquals(recipe.input.id, player.inv[17]?.id)
                assertEquals(0, player.count(recipe.output))
                assertMessageSent("You need to be on a members' world to use this item.")
            }
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
        val crushing: OneShotIngredientCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
