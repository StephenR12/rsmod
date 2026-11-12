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
import org.rsmod.api.config.refs.varbits
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
class AntifirePotionMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every antifire upgrade contract and dispatch`() =
        runMixingTest { deps ->
            assertEquals(363, seqs.human_herbing_vial.id)
            assertEquals(103, herblore_queues.doseScaledPotions.id)
            assertEquals(6140, varbits.super_antifire_unlocked.id)
            assertEquals(11994, objs.lava_scale_shard.id)
            assertEquals(21975, objs.crushed_superior_dragon_bones.id)
            assertTrue(deps.objs[objs.lava_scale_shard].members)
            assertTrue(deps.objs[objs.lava_scale_shard].stackable)
            assertTrue(deps.objs[objs.crushed_superior_dragon_bones].members)
            assertEquals(false, deps.objs[objs.crushed_superior_dragon_bones].stackable)
            assertEquals(
                listOf(
                    Contract(2458, 11994, 11957, 1, 1, 84, 275, 2, 11955, false),
                    Contract(2456, 11994, 11955, 2, 2, 84, 550, 2, 11953, false),
                    Contract(2454, 11994, 11953, 3, 3, 84, 825, 2, 11951, false),
                    Contract(2452, 11994, 11951, 4, 4, 84, 1100, 2, null, false),
                    Contract(2452, 21975, 21978, 4, 1, 92, 1300, 2, null, true),
                    Contract(21987, 11994, 22218, 1, 1, 98, 400, 2, 22215, true),
                    Contract(21984, 11994, 22215, 2, 2, 98, 800, 2, 22212, true),
                    Contract(21981, 11994, 22212, 3, 3, 98, 1200, 2, 22209, true),
                    Contract(21978, 11994, 22209, 4, 4, 98, 1600, 2, null, true),
                    Contract(11951, 21975, 22209, 4, 1, 98, 1800, 2, null, true),
                ),
                DoseScaledPotionRecipes.antifires.map {
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
                        it.requiresSuperAntifireUnlock,
                    )
                },
            )
            for (recipe in DoseScaledPotionRecipes.antifires) {
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
    fun GameTestState.`every antifire upgrade consumes exact resources at two ticks and xp`() =
        runMixingTest { deps ->
            for (recipe in DoseScaledPotionRecipes.antifires) {
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
    fun GameTestState
        .`menu both held-use orders and moving start use complete ingredient bundles`() =
        runMixingTest { deps ->
            val threeDose =
                DoseScaledPotionRecipes.antifires.single {
                    it.potion == objs.super_antifire_potion_3
                }
            prepare(level = threeDose.level)
            add(threeDose.potion, 3)
            add(threeDose.secondary, 7)
            val route = RouteRequestCoord(player.coords.translateX(2))
            player.routeRequest = route

            player.withProtectedAccess { deps.mixing.select(this, threeDose) }

            assertSame(route, player.routeRequest)
            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(threeDose.output.id, setup.values[2])
            player.routeRequest = null
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(4)
            assertEquals(2, player.count(threeDose.output))
            assertEquals(1, player.count(threeDose.potion))
            assertEquals(1, player.count(threeDose.secondary))
            assertEquals(2400, player.statMap.getFineXP(stats.herblore))

            for ((index, recipe) in
                listOf(
                        DoseScaledPotionRecipes.antifires.first(),
                        DoseScaledPotionRecipes.antifires.last(),
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
    fun GameTestState.`goggles preserve every full antifire secondary bundle`() =
        runMixingTest { deps ->
            for ((index, recipe) in DoseScaledPotionRecipes.antifires.withIndex()) {
                prepare(level = recipe.level)
                player.worn[Wearpos.Hat.slot] =
                    InvObj(
                        if (index % 2 == 0) {
                            objs.prescription_goggles
                        } else {
                            objs.prescription_goggles_focused
                        }
                    )
                addRequired(recipe)
                random.next = 0
                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance(recipe.ticks)
                assertEquals(1, player.count(recipe.output), deps.objs[recipe.output].name)
                assertEquals(
                    recipe.secondaryQuantity,
                    player.count(recipe.secondary),
                    deps.objs[recipe.output].name,
                )
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`amulets add only eligible antifire doses without ingredients or xp`() =
        runMixingTest { deps ->
            for (recipe in
                DoseScaledPotionRecipes.antifires.filter { it.extraDoseOutput != null }) {
                prepare(level = recipe.level)
                wearAlchemistsAmulet(deps, charges = 1)
                addRequired(recipe)
                random.next = 0
                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance(recipe.ticks)
                assertEquals(1, player.count(requireNotNull(recipe.extraDoseOutput)))
                assertEquals(0, player.count(recipe.secondary))
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
                assertEquals(
                    objs.alchemists_amulet_uncharged.id,
                    player.worn[Wearpos.Front.slot]?.id,
                )
            }

            val chemistryRecipe =
                DoseScaledPotionRecipes.antifires.single {
                    it.potion == objs.antifire_potion_2 && it.secondary == objs.lava_scale_shard
                }
            prepare(level = chemistryRecipe.level)
            player.worn[Wearpos.Front.slot] = InvObj(objs.amulet_of_chemistry)
            addRequired(chemistryRecipe)
            random.next = 0
            player.withProtectedAccess { deps.mixing.start(this, chemistryRecipe, quantity = 1) }
            advance(chemistryRecipe.ticks)
            assertEquals(1, player.count(objs.extended_antifire_3))
            assertEquals(0, player.count(objs.lava_scale_shard))
            assertEquals(550, player.statMap.getFineXP(stats.herblore))
            assertMessageSent(
                "Your amulet of chemistry helps you create a 3-dose potion. It has 4 charges left."
            )

            for (recipe in
                DoseScaledPotionRecipes.antifires.filter { it.extraDoseOutput == null }) {
                prepare(level = recipe.level)
                wearAlchemistsAmulet(deps, charges = 1)
                addRequired(recipe)
                random.next = 0
                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
                advance(recipe.ticks)
                assertEquals(1, player.count(recipe.output))
                assertEquals(0, player.count(recipe.secondary))
                assertEquals(objs.alchemists_amulet_charged.id, player.worn[Wearpos.Front.slot]?.id)
                player.withProtectedAccess {
                    assertEquals(1, deps.modifiers.alchemistsAmuletCharges(this))
                }
            }
        }

    @Test
    fun GameTestState.`unlock access interruption full inventory and queues stay atomic`() =
        runMixingTest { deps ->
            val extended = DoseScaledPotionRecipes.antifires.first()
            prepare(level = extended.level, baseLevel = extended.level - 5, unlocked = false)
            addRequired(extended)
            player.withProtectedAccess { deps.mixing.start(this, extended, quantity = 1) }
            advance(extended.ticks)
            assertEquals(1, player.count(extended.output))

            val superAntifire =
                DoseScaledPotionRecipes.antifires.single {
                    it.output == objs.super_antifire_potion_4
                }
            prepare(level = superAntifire.level, unlocked = false)
            assertAtomicFailure(deps, superAntifire)

            prepare(level = superAntifire.level, unlocked = true)
            addRequired(superAntifire)
            player.withProtectedAccess { deps.mixing.start(this, superAntifire, quantity = 1) }
            advance(superAntifire.ticks)
            assertEquals(1, player.count(superAntifire.output))

            for (recipe in DoseScaledPotionRecipes.antifires) {
                prepare(level = recipe.level - 1)
                assertAtomicFailure(deps, recipe)
                assertMessageSent(
                    "You need a Herblore level of ${recipe.level} to make this potion."
                )
            }

            prepare(members = false)
            assertAtomicFailure(deps, extended)
            assertMessageSent("You need to be on a members' world to make potions.")

            prepare(questState = 3)
            assertAtomicFailure(deps, extended)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")

            val recipe = DoseScaledPotionRecipes.antifires.last()
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
        unlocked: Boolean = true,
    ) {
        player.clearAllInvs()
        player.routeRequest = null
        player.members = members
        player.setBaseLevel(stats.herblore, baseLevel)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.setVarBit(varbits.super_antifire_unlocked, if (unlocked) 1 else 0)
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
        val requiresUnlock: Boolean,
    )
}
