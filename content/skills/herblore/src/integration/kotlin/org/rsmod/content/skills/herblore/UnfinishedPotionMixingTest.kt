package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class UnfinishedPotionMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every unfinished potion contract`() =
        runUnfinishedPotionTest { deps ->
            assertEquals(227, objs.vial_of_water.id)
            assertEquals(22446, objs.vial_of_blood.id)
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(
                listOf(
                    Contract(227, 249, 91, 3, 1, false),
                    Contract(227, 251, 93, 5, 1, false),
                    Contract(227, 253, 95, 12, 1, false),
                    Contract(227, 255, 97, 22, 1, false),
                    Contract(227, 257, 99, 30, 1, false),
                    Contract(227, 2998, 3002, 34, 1, false),
                    Contract(227, 259, 101, 45, 1, false),
                    Contract(227, 261, 103, 50, 1, false),
                    Contract(227, 263, 105, 55, 1, false),
                    Contract(227, 30097, 30100, 58, 1, false),
                    Contract(227, 3000, 3004, 63, 1, false),
                    Contract(227, 265, 107, 66, 1, false),
                    Contract(227, 2481, 2483, 69, 1, false),
                    Contract(227, 267, 109, 72, 1, false),
                    Contract(227, 269, 111, 78, 1, false),
                    Contract(22446, 265, 22443, 80, 1, false),
                    Contract(5935, 2998, 5942, 68, 2, true),
                    Contract(5935, 259, 5951, 79, 1, true),
                    Contract(5935, 6016, 5936, 73, 2, false),
                    Contract(5935, 2398, 5939, 82, 2, true),
                    Contract(5935, 27790, 5939, 82, 2, true),
                ),
                UnfinishedPotionRecipes.all.map {
                    Contract(
                        it.base.id,
                        it.herb.id,
                        it.output.id,
                        it.level,
                        it.ticks,
                        it.preserveHerbWithGoggles,
                    )
                },
            )
            assertTrue(UnfinishedPotionRecipes.all.all { deps.objs[it.herb].members })
            assertTrue(UnfinishedPotionRecipes.all.all { deps.objs[it.output].members })
            assertTrue(deps.objs[objs.vial_of_blood].members)
        }

    @Test
    fun GameTestState.`herb and vial open the native one-product Make-All menu`() =
        runUnfinishedPotionTest { deps ->
            preparePlayer()
            add(objs.snapdragon, 3)
            add(objs.vial_of_water, 2)

            val recipe = UnfinishedPotionRecipes.all.first { it.herb == objs.snapdragon }
            player.withProtectedAccess { deps.mixing.select(this, recipe) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.snapdragon_potion_unf.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "How many would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == 270 &&
                        it.componentId == (components.skillmulti_a.id and 0xFFFF)
                }
            assertEquals(1, events.start)
            assertEquals(2, events.end)
        }

    @Test
    fun GameTestState.`every recipe makes its exact unfinished potion with zero xp`() =
        runUnfinishedPotionTest { deps ->
            for (recipe in UnfinishedPotionRecipes.all) {
                preparePlayer(level = recipe.level)
                add(recipe.herb)
                add(recipe.base)

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_herbing_vial.id, player.pendingSequence.id)
                assertEquals(0, player.count(recipe.output))
                advance(recipe.ticks)
                assertEquals(0, player.count(recipe.herb), deps.objs[recipe.output].name)
                assertEquals(0, player.count(recipe.base), deps.objs[recipe.output].name)
                assertEquals(1, player.count(recipe.output), deps.objs[recipe.output].name)
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`Make-All produces one unfinished potion per tick`() =
        runUnfinishedPotionTest { deps ->
            val recipe = UnfinishedPotionRecipes.all.first { it.herb == objs.avantoe }
            preparePlayer()
            add(recipe.herb, 3)
            add(objs.vial_of_water, 3)

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 3) }

            repeat(3) { made ->
                advance()
                assertEquals(made + 1, player.count(recipe.output))
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.unfinishedPotions))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`strong weapon poison stages take two ticks and goggles save nightshade`() =
        runUnfinishedPotionTest { deps ->
            for (herb in listOf(objs.nightshade, objs.stackable_nightshade)) {
                val recipe = UnfinishedPotionRecipes.all.single { it.herb == herb }
                preparePlayer(level = 82)
                player.worn[org.rsmod.game.type.obj.Wearpos.Hat.slot] =
                    InvObj(objs.prescription_goggles)
                add(recipe.herb)
                add(recipe.base)
                random.next = 0

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance()
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(1, player.count(recipe.output))
                assertEquals(1, player.count(recipe.herb))
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }

            val cactusRecipe = UnfinishedPotionRecipes.all.single { it.herb == objs.cactus_spine }
            preparePlayer(level = 73)
            player.worn[org.rsmod.game.type.obj.Wearpos.Hat.slot] =
                InvObj(objs.prescription_goggles)
            add(cactusRecipe.herb)
            add(cactusRecipe.base)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, cactusRecipe, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(cactusRecipe.output))
            assertEquals(0, player.count(cactusRecipe.herb))
        }

    @Test
    fun GameTestState.`antidote first stages use distinct timing and goggles save each herb`() =
        runUnfinishedPotionTest { deps ->
            val expected =
                mapOf(objs.unfinished_antidote_plus to 2, objs.unfinished_antidote_plus_plus to 1)
            for ((output, ticks) in expected) {
                val recipe = UnfinishedPotionRecipes.all.single { it.output == output }
                preparePlayer(level = recipe.level)
                player.worn[org.rsmod.game.type.obj.Wearpos.Hat.slot] =
                    InvObj(objs.prescription_goggles)
                add(recipe.herb)
                add(recipe.base)
                random.next = 0

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                if (ticks > 1) {
                    advance(ticks - 1)
                    assertEquals(0, player.count(recipe.output))
                }
                advance()
                assertEquals(1, player.count(recipe.output))
                assertEquals(1, player.count(recipe.herb))
                assertEquals(0, player.count(recipe.base))
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`every coconut potion first stage dispatches in both held-use orders`() =
        runUnfinishedPotionTest { deps ->
            for (recipe in
                UnfinishedPotionRecipes.all.filter { it.base == objs.vial_coconut_milk }) {
                repeat(2) { reverse ->
                    preparePlayer(level = recipe.level)
                    add(recipe.herb)
                    add(recipe.base)
                    val herbSlot = player.inv.indexOfFirst { it?.id == recipe.herb.id }
                    val baseSlot = player.inv.indexOfFirst { it?.id == recipe.base.id }
                    player.withProtectedAccess {
                        if (reverse == 0) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[recipe.herb],
                                herbSlot,
                                deps.objs[recipe.base],
                                baseSlot,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[recipe.base],
                                baseSlot,
                                deps.objs[recipe.herb],
                                herbSlot,
                            )
                        }
                    }
                    resumeSkillMulti(components.skillmulti_a, quantity = 1)
                    advance(recipe.ticks)
                    assertEquals(1, player.count(recipe.output), "herb=${recipe.herb.id}")
                }
            }
        }

    @Test
    fun GameTestState.`membership quest and boosted level gates preserve both ingredients`() =
        runUnfinishedPotionTest { deps ->
            val recipe =
                UnfinishedPotionRecipes.all.single { it.output == objs.cadantine_blood_potion_unf }

            preparePlayer(level = recipe.level, baseLevel = recipe.level - 5)
            add(recipe.herb)
            add(recipe.base)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance()
            assertEquals(1, player.count(recipe.output))

            preparePlayer(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 80 to make this potion.")

            preparePlayer(members = false)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to be on a members' world to make potions.")

            preparePlayer(questState = 3)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")
        }

    @Test
    fun GameTestState.`full inventory resource loss and Herblore queue replacement are safe`() =
        runUnfinishedPotionTest { deps ->
            val recipe = UnfinishedPotionRecipes.all.first()
            preparePlayer()
            player.inv[0] = InvObj(recipe.herb)
            player.inv[1] = InvObj(objs.vial_of_water)
            for (slot in 2 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(27, player.inv.count { it != null })

            preparePlayer()
            add(recipe.herb, 2)
            add(objs.vial_of_water, 2)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 2) }
            advance()
            player.inv[player.inv.indexOfFirst { it?.id == objs.vial_of_water.id }] = null
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.unfinishedPotions))

            preparePlayer()
            add(recipe.herb, 2)
            add(objs.vial_of_water, 2)
            player.withProtectedAccess {
                weakQueue(herblore_queues.herbCleaning, 20)
                deps.mixing.start(this, recipe, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.herbCleaning))
            advance()
            player.clearPendingAction()
            advance()
            assertEquals(1, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`both held-use orders dispatch the selected Make-All quantity`() =
        runUnfinishedPotionTest { deps ->
            val recipe = UnfinishedPotionRecipes.all.first { it.herb == objs.irit_leaf }
            repeat(2) { reverse ->
                preparePlayer()
                add(recipe.herb, 2)
                add(recipe.base, 2)
                val herbSlot = player.inv.indexOfFirst { it?.id == recipe.herb.id }
                val vialSlot = player.inv.indexOfFirst { it?.id == recipe.base.id }

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.herb],
                            herbSlot,
                            deps.objs[recipe.base],
                            vialSlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.base],
                            vialSlot,
                            deps.objs[recipe.herb],
                            herbSlot,
                        )
                    }
                }
                resumeSkillMulti(components.skillmulti_a, quantity = 2)
                advance(2)
                assertEquals(2, player.count(recipe.output))
            }
        }

    private fun GameTestScope.assertAtomicFailure(
        deps: Dependencies,
        recipe: UnfinishedPotionRecipe,
    ) {
        add(recipe.herb)
        add(recipe.base)
        player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
        assertEquals(1, player.count(recipe.herb))
        assertEquals(1, player.count(recipe.base))
        assertEquals(0, player.count(recipe.output))
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.unfinishedPotions))
    }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        baseLevel: Int = level,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, baseLevel)
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

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runUnfinishedPotionTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val mixing: UnfinishedPotionMixing,
        val heldU: HeldUInteractions,
        val objs: ObjTypeList,
    )

    private data class Contract(
        val base: Int,
        val herb: Int,
        val output: Int,
        val level: Int,
        val ticks: Int,
        val gogglesCanPreserveHerb: Boolean,
    )
}
