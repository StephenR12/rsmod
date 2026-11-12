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
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class SwampTarMixingTest {
    @Test
    fun GameTestState.`cache 233 pins every swamp tar contract and dispatch`() =
        runMixingTest { deps ->
            assertEquals(1939, objs.swamp_tar.id)
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(5249, seqs.human_salamander_tar_grind.id)
            assertEquals(101, herblore_queues.swampTar.id)
            assertEquals(
                listOf(
                    Contract(249, 10142, 19, 300),
                    Contract(6681, 10142, 19, 300),
                    Contract(251, 10143, 31, 425),
                    Contract(253, 10144, 39, 550),
                    Contract(255, 10145, 44, 725),
                ),
                SwampTarRecipes.all.map {
                    Contract(it.herb.id, it.output.id, it.level, (it.xp * 10).toInt())
                },
            )
            for (recipe in SwampTarRecipes.all) {
                assertTrue(deps.objs[recipe.output].members)
                assertTrue(deps.objs[recipe.output].stackable)
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.herb.id, objs.swamp_tar.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`every recipe consumes one herb and fifteen tar after exactly three ticks`() =
        runMixingTest { deps ->
            for (recipe in SwampTarRecipes.all) {
                prepare(level = recipe.level)
                add(objs.pestle_and_mortar)
                add(recipe.herb)
                add(objs.swamp_tar, 15)

                player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }

                assertEquals(seqs.human_salamander_tar_grind.id, player.pendingSequence.id)
                advance(2)
                assertEquals(1, player.count(recipe.herb))
                assertEquals(15, player.count(objs.swamp_tar))
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(0, player.count(recipe.herb), deps.objs[recipe.output].name)
                assertEquals(0, player.count(objs.swamp_tar), deps.objs[recipe.output].name)
                assertEquals(15, player.count(recipe.output), deps.objs[recipe.output].name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.herblore),
                    deps.objs[recipe.output].name,
                )
            }
        }

    @Test
    fun GameTestState
        .`one available action starts directly while multiple actions use native menu`() =
        runMixingTest { deps ->
            val recipe = SwampTarRecipes.all.first()
            prepare()
            add(objs.pestle_and_mortar)
            add(recipe.herb)
            add(objs.swamp_tar, 15)

            player.withProtectedAccess { deps.mixing.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().isEmpty())
            assertEquals(1, player.weakQueueList.count(herblore_queues.swampTar))
            advance(3)
            assertEquals(15, player.count(recipe.output))

            prepare()
            add(objs.pestle_and_mortar)
            add(recipe.herb, 3)
            add(objs.swamp_tar, 30)

            player.withProtectedAccess { deps.mixing.select(this, recipe) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(recipe.output.id, setup.values[2])
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance(6)
            assertEquals(30, player.count(recipe.output))
            assertEquals(1, player.count(recipe.herb))
            assertEquals(0, player.count(objs.swamp_tar))
        }

    @Test
    fun GameTestState
        .`batching makes fifteen tar every three ticks and stops at requested quantity`() =
        runMixingTest { deps ->
            val recipe = SwampTarRecipes.all.last()
            prepare()
            add(objs.pestle_and_mortar)
            add(recipe.herb, 3)
            add(objs.swamp_tar, 45)

            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 3) }

            repeat(3) { made ->
                advance(2)
                assertEquals(made * 15, player.count(recipe.output))
                advance()
                assertEquals((made + 1) * 15, player.count(recipe.output))
            }
            advance(3)
            assertEquals(45, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(herblore_queues.swampTar))
            assertEquals((recipe.xp * 30).toInt(), player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`both held-use orders preserve the selected guam input recipe`() =
        runMixingTest { deps ->
            val guamRecipes = SwampTarRecipes.all.filter { it.output == objs.guam_tar }
            for ((index, recipe) in guamRecipes.withIndex()) {
                prepare()
                add(objs.pestle_and_mortar)
                add(recipe.herb)
                add(objs.swamp_tar, 15)
                val herbSlot = player.inv.indexOfFirst { it?.id == recipe.herb.id }
                val tarSlot = player.inv.indexOfFirst { it?.id == objs.swamp_tar.id }

                player.withProtectedAccess {
                    if (index == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.herb],
                            herbSlot,
                            deps.objs[objs.swamp_tar],
                            tarSlot,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.swamp_tar],
                            tarSlot,
                            deps.objs[recipe.herb],
                            herbSlot,
                        )
                    }
                }
                advance(3)
                assertEquals(0, player.count(recipe.herb))
                assertEquals(15, player.count(recipe.output))
            }
        }

    @Test
    fun GameTestState
        .`access tools levels resources interruption and full inventory stay atomic`() =
        runMixingTest { deps ->
            val recipe = SwampTarRecipes.all.last()

            prepare(level = recipe.level, baseLevel = recipe.level - 5)
            addRequired(recipe)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            advance(3)
            assertEquals(15, player.count(recipe.output))

            prepare(level = recipe.level - 1)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need a Herblore level of 44 to make this tar.")

            prepare(members = false)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to be on a members' world to make tar.")

            prepare(questState = 3)
            assertAtomicFailure(deps, recipe)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")

            prepare()
            add(recipe.herb)
            add(objs.swamp_tar, 15)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            assertMessageSent("You need a pestle and mortar to make herb tar.")
            assertEquals(1, player.count(recipe.herb))
            assertEquals(15, player.count(objs.swamp_tar))
            assertEquals(0, player.weakQueueList.count(herblore_queues.swampTar))

            prepare()
            add(objs.pestle_and_mortar)
            add(recipe.herb)
            add(objs.swamp_tar, 14)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
            assertEquals(1, player.count(recipe.herb))
            assertEquals(14, player.count(objs.swamp_tar))
            assertEquals(0, player.weakQueueList.count(herblore_queues.swampTar))

            prepare()
            add(objs.pestle_and_mortar)
            add(recipe.herb, 2)
            add(objs.swamp_tar, 30)
            player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 2) }
            advance(3)
            player.inv[player.inv.indexOfFirst { it?.id == objs.pestle_and_mortar.id }] = null
            advance(3)
            assertEquals(15, player.count(recipe.output))
            assertEquals(1, player.count(recipe.herb))
            assertEquals(15, player.count(objs.swamp_tar))
            assertEquals(0, player.weakQueueList.count(herblore_queues.swampTar))

            prepare()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[1] = InvObj(recipe.herb)
            player.inv[2] = InvObj(objs.swamp_tar, 15)
            for (slot in 3 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.withProtectedAccess {
                weakQueue(herblore_queues.herbCleaning, 20)
                deps.mixing.start(this, recipe, quantity = 1)
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.herbCleaning))
            advance(3)
            assertEquals(15, player.count(recipe.output))
            assertEquals(27, player.inv.count { it != null })
        }

    private fun GameTestScope.assertAtomicFailure(deps: Dependencies, recipe: SwampTarRecipe) {
        addRequired(recipe)
        player.withProtectedAccess { deps.mixing.start(this, recipe, quantity = 1) }
        assertEquals(1, player.count(recipe.herb))
        assertEquals(15, player.count(objs.swamp_tar))
        assertEquals(0, player.count(recipe.output))
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.swampTar))
    }

    private fun GameTestScope.addRequired(recipe: SwampTarRecipe) {
        add(objs.pestle_and_mortar)
        add(recipe.herb)
        add(objs.swamp_tar, 15)
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
        val mixing: SwampTarMixing,
        val heldU: HeldUInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(val herb: Int, val output: Int, val level: Int, val fineXp: Int)
}
