package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
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
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class DoseScaledPotionMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every anti-venom production contract and dispatch`() =
        runMixingTest { deps ->
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(103, herblore_queues.doseScaledPotions.id)
            assertEquals(12934, objs.zulrahs_scales.id)
            assertEquals(29784, objs.araxyte_venom_sack.id)
            assertTrue(deps.objs[objs.zulrahs_scales].stackable)
            assertTrue(deps.objs[objs.araxyte_venom_sack].stackable)
            assertEquals(
                listOf(
                    Contract(5958, 12934, 12911, 1, 5, 87, 300, 2, 12909),
                    Contract(5956, 12934, 12909, 2, 10, 87, 600, 2, 12907),
                    Contract(5954, 12934, 12907, 3, 15, 87, 900, 2, 12905),
                    Contract(5952, 12934, 12905, 4, 20, 87, 1200, 2, null),
                    Contract(12905, 269, 12913, 4, 1, 94, 1250, 2, null),
                    Contract(12905, 111, 12913, 4, 1, 94, 1250, 2, null),
                    Contract(12919, 29784, 29833, 1, 1, 94, 200, 3, 29830),
                    Contract(12917, 29784, 29830, 2, 2, 94, 400, 3, 29827),
                    Contract(12915, 29784, 29827, 3, 3, 94, 600, 3, 29824),
                    Contract(12913, 29784, 29824, 4, 4, 94, 800, 3, null),
                ),
                DoseScaledPotionRecipes.antiVenoms.map {
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
            for (recipe in DoseScaledPotionRecipes.antiVenoms) {
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
    fun GameTestState.`every recipe consumes dose-scaled resources at exact ticks and xp`() =
        runMixingTest { deps ->
            for (recipe in DoseScaledPotionRecipes.antiVenoms) {
                prepare(level = recipe.level)
                add(recipe.potion)
                add(recipe.secondary, recipe.secondaryQuantity)

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_herbing_vial.id, player.pendingSequence.id)
                advance(recipe.ticks - 1)
                assertEquals(1, player.count(recipe.potion), deps.objs[recipe.output].name)
                assertEquals(0, player.count(recipe.output), deps.objs[recipe.output].name)
                advance()
                assertEquals(0, player.count(recipe.potion), deps.objs[recipe.output].name)
                assertEquals(0, player.count(recipe.secondary), deps.objs[recipe.output].name)
                assertEquals(1, player.count(recipe.output), deps.objs[recipe.output].name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.herblore),
                    deps.objs[recipe.output].name,
                )
            }
        }

    @Test
    fun GameTestState.`native menu caps quantity by the complete secondary bundle`() =
        runMixingTest { deps ->
            val recipe =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.antidote_plus_plus_3 }
            prepare()
            add(recipe.potion, 3)
            add(recipe.secondary, 30)

            player.withProtectedAccess { deps.mixing.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(recipe.output.id, setup.values[2])
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(4)
            assertEquals(2, player.count(recipe.output))
            assertEquals(1, player.count(recipe.potion))
            assertEquals(0, player.count(recipe.secondary))
            assertEquals(1800, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`both held-use orders dispatch dose-scaled potion recipes`() =
        runMixingTest { deps ->
            for ((index, recipe) in
                listOf(
                        DoseScaledPotionRecipes.antiVenoms.first(),
                        DoseScaledPotionRecipes.antiVenoms.last(),
                    )
                    .withIndex()) {
                prepare(level = recipe.level)
                add(recipe.potion)
                add(recipe.secondary, recipe.secondaryQuantity)
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
    fun GameTestState.`amulets and goggles obey dose-aware anti-venom rules`() =
        runMixingTest { deps ->
            val antiVenomThree =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.antidote_plus_plus_3 }
            prepare(level = antiVenomThree.level)
            wearAlchemistsAmulet(deps, charges = 1)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles_focused)
            addRequired(antiVenomThree)
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.mixing.start(this, antiVenomThree, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(objs.anti_venom_4))
            assertEquals(15, player.count(objs.zulrahs_scales))
            assertEquals(900, player.statMap.getFineXP(stats.herblore))
            assertEquals(objs.alchemists_amulet_uncharged.id, player.worn[Wearpos.Front.slot]?.id)

            val antiVenomTwo =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.antidote_plus_plus_2 }
            prepare(level = antiVenomTwo.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            addRequired(antiVenomTwo)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, antiVenomTwo, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(objs.anti_venom_3))
            assertEquals(0, player.count(objs.zulrahs_scales))
            assertEquals(600, player.statMap.getFineXP(stats.herblore))
            assertMessageSent(
                "Your amulet of chemistry helps you create a 3-dose potion. It has 4 charges left."
            )

            val antiVenomFour =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.antidote_plus_plus_4 }
            prepare(level = antiVenomFour.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
            addRequired(antiVenomFour)
            val antiVenomFourCharges = deps.chemistryAmulet.remaining(player)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, antiVenomFour, quantity = 1) }
            advance(2)
            assertEquals(1, player.count(objs.anti_venom_4))
            assertEquals(20, player.count(objs.zulrahs_scales))
            assertEquals(antiVenomFourCharges, deps.chemistryAmulet.remaining(player))

            for (plusRecipe in
                DoseScaledPotionRecipes.antiVenoms.filter { it.output == objs.anti_venom_plus_4 }) {
                prepare(level = plusRecipe.level)
                wearAlchemistsAmulet(deps, charges = 1)
                player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
                addRequired(plusRecipe)
                random.next = 0
                player.withProtectedAccess { deps.mixing.start(this, plusRecipe, quantity = 1) }
                advance(2)
                assertEquals(1, player.count(objs.anti_venom_plus_4))
                assertEquals(plusRecipe.secondaryQuantity, player.count(plusRecipe.secondary))
                assertEquals(objs.alchemists_amulet_charged.id, player.worn[Wearpos.Front.slot]?.id)
                player.withProtectedAccess {
                    assertEquals(1, deps.modifiers.alchemistsAmuletCharges(this))
                }
            }

            val extendedThree =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.anti_venom_plus_3 }
            prepare(level = extendedThree.level)
            wearAlchemistsAmulet(deps, charges = 1)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
            addRequired(extendedThree)
            random.next = 0
            random.then = 0
            player.withProtectedAccess { deps.mixing.start(this, extendedThree, quantity = 1) }
            advance(3)
            assertEquals(1, player.count(objs.extended_anti_venom_plus_4))
            assertEquals(3, player.count(objs.araxyte_venom_sack))
            assertEquals(600, player.statMap.getFineXP(stats.herblore))

            val extendedFour =
                DoseScaledPotionRecipes.antiVenoms.single { it.potion == objs.anti_venom_plus_4 }
            prepare(level = extendedFour.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.prescription_goggles)
            addRequired(extendedFour)
            val extendedFourCharges = deps.chemistryAmulet.remaining(player)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, extendedFour, quantity = 1) }
            advance(3)
            assertEquals(1, player.count(objs.extended_anti_venom_plus_4))
            assertEquals(4, player.count(objs.araxyte_venom_sack))
            assertEquals(extendedFourCharges, deps.chemistryAmulet.remaining(player))
        }

    @Test
    fun GameTestState.`access resources interruption full inventory and queues stay atomic`() =
        runMixingTest { deps ->
            val recipe = DoseScaledPotionRecipes.antiVenoms.last()

            prepare(level = recipe.level, baseLevel = recipe.level - 5)
            addRequired(recipe)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(recipe.ticks)
            assertEquals(1, player.count(recipe.output))

            prepare(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 94 to make this potion.")

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
