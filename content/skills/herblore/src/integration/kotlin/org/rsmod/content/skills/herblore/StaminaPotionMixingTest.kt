package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
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
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.movement.RouteRequestCoord
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class StaminaPotionMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every stamina potion production contract and dispatch`() =
        runMixingTest { deps ->
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(103, herblore_queues.doseScaledPotions.id)
            assertEquals(12640, objs.amylase_crystal.id)
            assertTrue(deps.objs[objs.amylase_crystal].members)
            assertTrue(deps.objs[objs.amylase_crystal].stackable)
            assertEquals(
                listOf(
                    Contract(3022, 12640, 12631, 1, 1, 77, 255, 2, 12629),
                    Contract(3020, 12640, 12629, 2, 2, 77, 510, 2, 12627),
                    Contract(3018, 12640, 12627, 3, 3, 77, 765, 2, 12625),
                    Contract(3016, 12640, 12625, 4, 4, 77, 1020, 2, null),
                ),
                DoseScaledPotionRecipes.staminaPotions.map {
                    Contract(
                        it.potion.id,
                        it.secondary.id,
                        it.output.id,
                        it.doses,
                        it.secondaryQuantity,
                        it.level,
                        (it.xp * 10).toInt(),
                        it.ticks,
                        it.extraDoseOutput?.id,
                    )
                },
            )
            for (recipe in DoseScaledPotionRecipes.staminaPotions) {
                assertTrue(deps.objs[recipe.potion].members)
                assertTrue(deps.objs[recipe.output].members)
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.potion.id, recipe.secondary.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`every stamina dose consumes exact amylase at two ticks and exact xp`() =
        runMixingTest { deps ->
            for (recipe in DoseScaledPotionRecipes.staminaPotions) {
                prepare(level = recipe.level)
                addRequired(recipe)

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_herbing_vial.id, player.pendingSequence.id)
                advance()
                assertEquals(1, player.count(recipe.potion))
                assertEquals(recipe.secondaryQuantity, player.count(recipe.secondary))
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(0, player.count(recipe.potion))
                assertEquals(0, player.count(recipe.secondary))
                assertEquals(1, player.count(recipe.output))
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`native menu and both held-use orders honor complete amylase bundles`() =
        runMixingTest { deps ->
            val threeDose =
                DoseScaledPotionRecipes.staminaPotions.single { it.potion == objs.super_energy_3 }
            prepare(level = threeDose.level)
            add(threeDose.potion, 3)
            add(threeDose.secondary, 7)

            player.withProtectedAccess { deps.mixing.select(this, threeDose) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(threeDose.output.id, setup.values[2])
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(4)
            assertEquals(2, player.count(threeDose.output))
            assertEquals(1, player.count(threeDose.potion))
            assertEquals(1, player.count(threeDose.secondary))
            assertEquals(1530, player.statMap.getFineXP(stats.herblore))

            for ((index, recipe) in
                listOf(
                        DoseScaledPotionRecipes.staminaPotions.first(),
                        DoseScaledPotionRecipes.staminaPotions.last(),
                    )
                    .withIndex()) {
                prepare(level = recipe.level)
                addRequired(recipe)
                val potionSlot = player.inv.indexOfFirst { it?.id == recipe.potion.id }
                val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
                player.withProtectedAccess {
                    if (index == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.potion],
                            potionSlot,
                            deps.objs[recipe.secondary],
                            secondarySlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.secondary],
                            secondarySlot,
                            deps.objs[recipe.potion],
                            potionSlot,
                        )
                    }
                }
                resumeSkillMulti(components.skillmulti_a, quantity = 1)
                advance(recipe.ticks)
                assertEquals(1, player.count(recipe.output))
            }
        }

    @Test
    fun GameTestState.`amulets and goggles obey dose-aware stamina potion rules`() =
        runMixingTest { deps ->
            val threeDose =
                DoseScaledPotionRecipes.staminaPotions.single { it.potion == objs.super_energy_3 }
            prepare(level = threeDose.level)
            wearAlchemistsAmulet(deps, charges = 1)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles_focused)
            addRequired(threeDose)
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.mixing.start(this, threeDose, quantity = 1) }
            advance(threeDose.ticks)
            assertEquals(1, player.count(objs.stamina_potion_4))
            assertEquals(3, player.count(objs.amylase_crystal))
            assertEquals(765, player.statMap.getFineXP(stats.herblore))
            assertEquals(objs.alchemists_amulet_uncharged.id, player.worn[Wearpos.Front.slot]?.id)

            val twoDose =
                DoseScaledPotionRecipes.staminaPotions.single { it.potion == objs.super_energy_2 }
            prepare(level = twoDose.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            addRequired(twoDose)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, twoDose, quantity = 1) }
            advance(twoDose.ticks)
            assertEquals(1, player.count(objs.stamina_potion_3))
            assertEquals(0, player.count(objs.amylase_crystal))
            assertEquals(510, player.statMap.getFineXP(stats.herblore))
            assertMessageSent(
                "Your amulet of chemistry helps you create a 3-dose potion. It has 4 charges left."
            )

            val fourDose = DoseScaledPotionRecipes.staminaPotions.last()
            prepare(level = fourDose.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
            addRequired(fourDose)
            val charges = deps.chemistryAmulet.remaining(player)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, fourDose, quantity = 1) }
            advance(fourDose.ticks)
            assertEquals(1, player.count(objs.stamina_potion_4))
            assertEquals(4, player.count(objs.amylase_crystal))
            assertEquals(charges, deps.chemistryAmulet.remaining(player))
        }

    @Test
    fun GameTestState.`access movement interruption full inventory and queues stay atomic`() =
        runMixingTest { deps ->
            val recipe = DoseScaledPotionRecipes.staminaPotions.last()

            prepare(level = recipe.level, baseLevel = recipe.level - 5)
            addRequired(recipe)
            val route = RouteRequestCoord(player.coords.translateX(2))
            player.routeRequest = route
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            assertSame(route, player.routeRequest)
            player.routeRequest = null
            advance(recipe.ticks)
            assertEquals(1, player.count(recipe.output))

            prepare(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 77 to make this potion.")

            prepare(members = false)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to be on a members' world to make potions.")

            prepare(questState = 3)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")

            prepare()
            add(recipe.potion)
            add(recipe.secondary, recipe.secondaryQuantity - 1)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            assertEquals(1, player.count(recipe.potion))
            assertEquals(recipe.secondaryQuantity - 1, player.count(recipe.secondary))
            assertEquals(0, player.weakQueueList.count(herblore_queues.doseScaledPotions))

            prepare()
            add(recipe.potion, 2)
            add(recipe.secondary, recipe.secondaryQuantity * 2)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 2) }
            advance(recipe.ticks)
            player.inv[player.inv.indexOfFirst { it?.id == recipe.secondary.id }] = null
            advance(recipe.ticks)
            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.count(recipe.potion))
            assertEquals(0, player.weakQueueList.count(herblore_queues.doseScaledPotions))

            prepare()
            player.inv[0] = InvObj(recipe.potion)
            player.inv[1] = InvObj(recipe.secondary, recipe.secondaryQuantity)
            for (slot in 2 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess {
                weakQueue(herblore_queues.finishedPotions, 20)
                deps.mixing.start(this, recipe, quantity = 1)
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.finishedPotions))
            advance(recipe.ticks)
            assertEquals(1, player.count(recipe.output))
            assertEquals(27, player.inv.count { it != null })
        }

    private fun GameTestScope.assertAtomicFailure(
        deps: Dependencies,
        recipe: DoseScaledPotionRecipe,
    ) {
        addRequired(recipe)
        player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
        assertEquals(1, player.count(recipe.potion))
        assertEquals(recipe.secondaryQuantity, player.count(recipe.secondary))
        assertEquals(0, player.count(recipe.output))
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.doseScaledPotions))
    }

    private fun GameTestScope.addRequired(recipe: DoseScaledPotionRecipe) {
        add(recipe.potion)
        add(recipe.secondary, recipe.secondaryQuantity)
    }

    private fun GameTestScope.wearAlchemistsAmulet(deps: Dependencies, charges: Int) {
        player.worn[Wearpos.Front.slot] = InvObj(objs.alchemists_amulet_charged)
        player.withProtectedAccess { deps.modifiers.setAlchemistsAmuletCharges(this, charges) }
    }

    private fun GameTestScope.prepare(
        level: Int = 99,
        baseLevel: Int = level,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.routeRequest = null
        player.members = members
        player.setBaseLevel(stats.herblore, baseLevel)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
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
        val mixing: DoseScaledPotionMixing,
        val modifiers: PotionProductionModifiers,
        val chemistryAmulet: ChemistryAmuletRepository,
        val heldU: HeldUInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(
        val potion: Int,
        val secondary: Int,
        val output: Int,
        val doses: Int,
        val secondaryQuantity: Int,
        val level: Int,
        val fineXp: Int,
        val ticks: Int,
        val extraDoseOutput: Int?,
    )
}
