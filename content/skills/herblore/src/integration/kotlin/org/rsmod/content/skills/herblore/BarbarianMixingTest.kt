package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianMixingTest {
    @Test
    fun GameTestState.`cache 233 pins all Barbarian mix contracts and dispatch`() =
        runMixingTest { deps ->
            assertEquals(11324, objs.roe.id)
            assertEquals(11326, objs.caviar.id)
            assertEquals(3761, varbits.brut_herb_potion.id)
            assertEquals(100, herblore_queues.barbarianMix.id)
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(
                listOf(
                    Contract(123, 11429, 4, 80, true),
                    Contract(177, 11433, 6, 120, true),
                    Contract(4846, 11437, 9, 140, true),
                    Contract(117, 11443, 14, 170, true),
                    Contract(129, 11449, 24, 210, true),
                    Contract(3012, 11453, 29, 230, false),
                    Contract(135, 11457, 33, 250, false),
                    Contract(3036, 11461, 37, 270, false),
                    Contract(9743, 11445, 40, 280, false),
                    Contract(141, 11465, 42, 290, false),
                    Contract(147, 11469, 47, 330, false),
                    Contract(183, 11473, 51, 350, false),
                    Contract(153, 11477, 53, 380, false),
                    Contract(3020, 11481, 56, 390, false),
                    Contract(10002, 11517, 58, 400, false),
                    Contract(159, 11485, 59, 420, false),
                    Contract(9023, 11489, 61, 430, false),
                    Contract(3028, 11493, 67, 480, false),
                    Contract(165, 11497, 71, 500, false),
                    Contract(5947, 11501, 74, 520, false),
                    Contract(2456, 11505, 75, 530, false),
                    Contract(171, 11509, 80, 540, false),
                    Contract(3044, 11513, 83, 570, false),
                    Contract(191, 11521, 85, 580, false),
                    Contract(12629, 12633, 86, 600, false),
                    Contract(11955, 11960, 91, 610, false),
                    Contract(26344, 26350, 92, 630, false),
                    Contract(21984, 21994, 98, 700, false),
                    Contract(22215, 22221, 99, 780, false),
                ),
                BarbarianMixRecipes.all.map {
                    Contract(
                        it.potion.id,
                        it.output.id,
                        it.level,
                        (it.xp * 10).toInt(),
                        it.acceptsRoe,
                    )
                },
            )
            for (recipe in BarbarianMixRecipes.all) {
                assertTrue(
                    deps.objs[recipe.output].members,
                    "output ${recipe.output.id}: ${deps.objs[recipe.output].name}",
                )
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.potion.id, objs.caviar.id),
                    )
                )
                assertEquals(
                    recipe.acceptsRoe,
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.potion.id, objs.roe.id),
                    ),
                )
            }
        }

    @Test
    fun GameTestState.`every recipe makes its exact output in one tick with exact xp`() =
        runMixingTest { deps ->
            for ((index, recipe) in BarbarianMixRecipes.all.withIndex()) {
                prepare(level = recipe.level)
                val egg = if (recipe.acceptsRoe && index % 2 == 0) objs.roe else objs.caviar
                add(recipe.potion)
                add(egg)

                player.withProtectedAccess { deps.mixing.start(this, recipe, egg, quantity = 1) }

                assertEquals(seqs.human_herbing_vial.id, player.pendingSequence.id)
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(0, player.count(recipe.potion), deps.objs[recipe.output].name)
                assertEquals(0, player.count(egg), deps.objs[recipe.output].name)
                assertEquals(1, player.count(recipe.output), deps.objs[recipe.output].name)
                assertMessageSent(BarbarianMixing.SUCCESS_MESSAGE)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.herblore),
                    deps.objs[recipe.output].name,
                )
            }
        }

    @Test
    fun GameTestState.`training attack mix advances state while other mixes require completion`() =
        runMixingTest { deps ->
            val attack = BarbarianMixRecipes.all.first()
            val energy = BarbarianMixRecipes.all.first { it.output == objs.energy_mix_2 }

            prepare(training = BarbarianMixing.TRAINING_TASKED, level = attack.level)
            add(attack.potion)
            add(objs.roe)
            player.withProtectedAccess { deps.mixing.start(this, attack, objs.roe, quantity = 1) }
            advance()
            assertEquals(1, player.count(attack.output))
            assertEquals(BarbarianMixing.TRAINING_MIX_MADE, player.vars[varbits.brut_herb_potion])
            assertMessageSent(BarbarianMixing.SUCCESS_MESSAGE)
            assertTrue(
                client.filterIsInstance<IfSetText>().any {
                    it.text.replace("<br>", " ") == BarbarianMixing.TRAINING_MESSAGE
                }
            )
            val messageBox =
                requireNotNull(
                    player.ui.modals.getComponent(UserInterface(interfaces.messagebox.id))
                )
            player.ui.queueClose(messageBox)
            advance()

            prepare(training = BarbarianMixing.TRAINING_MIX_MADE, level = attack.level)
            add(attack.potion)
            add(objs.caviar)
            player.withProtectedAccess {
                deps.mixing.start(this, attack, objs.caviar, quantity = 1)
            }
            advance()
            assertEquals(1, player.count(attack.output))

            prepare(training = BarbarianMixing.TRAINING_MIX_MADE, level = energy.level)
            assertAtomicFailure(deps, energy, objs.caviar)
            assertMessageSent("You need to complete Otto's barbarian Herblore training first.")

            prepare(training = BarbarianMixing.TRAINING_COMPLETE, level = energy.level)
            add(energy.potion)
            add(objs.caviar)
            player.withProtectedAccess {
                deps.mixing.start(this, energy, objs.caviar, quantity = 1)
            }
            advance()
            assertEquals(1, player.count(energy.output))
        }

    @Test
    fun GameTestState.`make all menu and both item orders produce one mix per tick`() =
        runMixingTest { deps ->
            val recipe = BarbarianMixRecipes.all.first()
            repeat(2) { reverse ->
                prepare()
                add(recipe.potion, 3)
                add(objs.roe, 3)
                val potionSlot = player.inv.indexOfFirst { it?.id == recipe.potion.id }
                val eggSlot = player.inv.indexOfFirst { it?.id == objs.roe.id }

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.potion],
                            potionSlot,
                            deps.objs[objs.roe],
                            eggSlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.roe],
                            eggSlot,
                            deps.objs[recipe.potion],
                            potionSlot,
                        )
                    }
                }
                assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
                val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
                assertEquals(recipe.output.id, setup.values[2])
                assertEquals(3, setup.values[1])
                resumeSkillMulti(components.skillmulti_a, quantity = 3)

                repeat(3) { made ->
                    advance()
                    assertEquals(made + 1, player.count(recipe.output))
                }
                assertEquals(0, player.weakQueueList.count(herblore_queues.barbarianMix))
                assertEquals((recipe.xp * 30).toInt(), player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`access level egg resource and full inventory failures are atomic`() =
        runMixingTest { deps ->
            val recipe = BarbarianMixRecipes.all.last()

            prepare(level = recipe.level, baseLevel = recipe.level - 5)
            add(recipe.potion)
            add(objs.caviar)
            player.withProtectedAccess {
                deps.mixing.start(this, recipe, objs.caviar, quantity = 1)
            }
            advance()
            assertEquals(1, player.count(recipe.output))

            prepare(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe, objs.caviar)
            assertMessageSent("You need a Herblore level of 99 to make this potion.")

            prepare(members = false)
            assertAtomicFailure(deps, recipe, objs.caviar)
            assertMessageSent("You need to be on a members' world to make potions.")

            prepare(questState = 3)
            assertAtomicFailure(deps, recipe, objs.caviar)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")

            val roeRejected = BarbarianMixRecipes.all.first { !it.acceptsRoe }
            prepare(level = roeRejected.level)
            assertAtomicFailure(deps, roeRejected, objs.roe)

            prepare()
            player.inv[0] = InvObj(recipe.potion)
            player.inv[1] = InvObj(objs.caviar)
            for (slot in 2 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess {
                deps.mixing.start(this, recipe, objs.caviar, quantity = 1)
            }
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(player.inv.size - 1, player.inv.count { it != null })

            prepare()
            add(recipe.potion, 2)
            add(objs.caviar, 2)
            player.withProtectedAccess {
                deps.mixing.start(this, recipe, objs.caviar, quantity = 2)
            }
            advance()
            player.inv[player.inv.indexOfFirst { it?.id == objs.caviar.id }] = null
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.barbarianMix))
        }

    private fun GameTestScope.assertAtomicFailure(
        deps: Dependencies,
        recipe: BarbarianMixRecipe,
        egg: ObjType,
    ) {
        add(recipe.potion)
        add(egg)
        player.withProtectedAccess { deps.mixing.start(this, recipe, egg, quantity = 1) }
        assertEquals(1, player.count(recipe.potion))
        assertEquals(1, player.count(egg))
        assertEquals(0, player.count(recipe.output))
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.barbarianMix))
    }

    private fun GameTestScope.prepare(
        level: Int = 99,
        baseLevel: Int = level,
        members: Boolean = true,
        questState: Int = 4,
        training: Int = BarbarianMixing.TRAINING_COMPLETE,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, baseLevel)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.setVarBit(varbits.brut_herb_potion, training)
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

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target = requireNotNull(player.ui.modals.getComponent(UserInterface(270)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runMixingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val mixing: BarbarianMixing,
        val heldU: HeldUInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(
        val potion: Int,
        val output: Int,
        val level: Int,
        val fineXp: Int,
        val acceptsRoe: Boolean,
    )
}
