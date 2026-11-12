package org.rsmod.content.skills.crafting.amethystcutting

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
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class AmethystCuttingTest {
    @Test
    fun GameTestState.`cache 233 pins the complete amethyst product family`() =
        runAmethystCuttingTest { deps ->
            assertEquals(
                listOf(
                    "Amethyst bolt tips:83:21338:15",
                    "Amethyst arrowtips:85:21350:15",
                    "Amethyst javelin tips:87:21352:5",
                    "Amethyst dart tips:89:25853:8",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.output.id}:${it.outputCount}"
                },
            )
            assertEquals(21347, objs.amethyst.id)
            assertEquals(1755, objs.chisel.id)
            assertEquals(6295, seqs.human_amethystcutting.id)
        }

    @Test
    fun GameTestState.`chisel use opens the native four-product quantity menu in level order`() =
        runAmethystCuttingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst, 3)

            player.withProtectedAccess { deps.cutting.select(this) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    3,
                    21338,
                    21350,
                    21352,
                    25853,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    3,
                    "What would you like to make?",
                ),
                setup.values,
            )
        }

    @Test
    fun GameTestState.`every product uses one amethyst and awards sixty Crafting xp`() =
        runAmethystCuttingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.chisel)
                add(objs.amethyst)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.cutting.process(this, AmethystCuttingRequest(recipe.output, remaining = 1))
                }

                assertEquals(0, player.count(objs.amethyst), recipe.name)
                assertEquals(recipe.outputCount, player.count(recipe.output), recipe.name)
                assertEquals(600, player.statMap.getFineXP(stats.crafting) - startXp, recipe.name)
            }
        }

    @Test
    fun GameTestState.`selected amethyst is cut one every two ticks`() =
        runAmethystCuttingTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst, 3)

            player.withProtectedAccess { deps.cutting.start(this, recipe, quantity = 3) }
            assertEquals(seqs.human_amethystcutting.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(15, player.count(recipe.output))
            advance(2)
            assertEquals(30, player.count(recipe.output))
            advance(2)
            assertEquals(45, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(crafting_queues.amethystCutting))
        }

    @Test
    fun GameTestState.`each product enforces its boosted Crafting level atomically`() =
        runAmethystCuttingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                player.stats[stats.crafting] = recipe.level - 1
                add(objs.chisel)
                add(objs.amethyst)

                player.withProtectedAccess { deps.cutting.start(this, recipe, quantity = 1) }

                assertEquals(1, player.count(objs.amethyst), recipe.name)
                assertEquals(0, player.count(recipe.output), recipe.name)
                assertMessageSent(
                    "You need a Crafting level of ${recipe.level} to make ${recipe.name.lowercase()}."
                )
            }
        }

    @Test
    fun GameTestState.`membership chisel and source failures do not start production`() =
        runAmethystCuttingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.chisel)
            add(objs.amethyst)
            player.withProtectedAccess { deps.cutting.select(this) }
            assertMessageSent("You need to be on a members' world to cut amethyst.")
            assertEquals(1, player.count(objs.amethyst))

            preparePlayer()
            add(objs.amethyst)
            player.withProtectedAccess { deps.cutting.select(this) }
            assertMessageSent("You need a chisel to cut amethyst.")
            assertEquals(1, player.count(objs.amethyst))

            preparePlayer()
            add(objs.chisel)
            player.withProtectedAccess { deps.cutting.select(this) }
            assertMessageSent("You do not have any amethyst left to cut.")
        }

    @Test
    fun GameTestState.`resource loss and another Crafting action interrupt the batch safely`() =
        runAmethystCuttingTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst, 2)
            player.withProtectedAccess { deps.cutting.start(this, recipe, quantity = 2) }
            advance(2)
            player.inv[player.inv.indexOfFirst { it?.id == objs.chisel.id }] = null
            advance(2)
            assertEquals(15, player.count(recipe.output))
            assertEquals(1, player.count(objs.amethyst))
            assertEquals(0, player.weakQueueList.count(crafting_queues.amethystCutting))

            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst)
            player.withProtectedAccess {
                weakQueue(crafting_queues.gemCutting, 20)
                deps.cutting.start(this, recipe, quantity = 1)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.gemCutting))
            assertEquals(1, player.weakQueueList.count(crafting_queues.amethystCutting))
        }

    @Test
    fun GameTestState.`full inventory and reverse held-use select the requested product`() =
        runAmethystCuttingTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst, 27)
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess { deps.cutting.start(this, recipe, quantity = 1) }
            advance(2)
            assertEquals(15, player.count(recipe.output))

            preparePlayer()
            add(objs.chisel)
            add(objs.amethyst)
            val chiselSlot = player.inv.indexOfFirst { it?.id == objs.chisel.id }
            val amethystSlot = player.inv.indexOfFirst { it?.id == objs.amethyst.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.amethyst],
                    amethystSlot,
                    objTypes[objs.chisel],
                    chiselSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_d, quantity = 1)
            advance(2)
            assertEquals(8, player.count(objs.amethyst_dart_tip))
            assertEquals(1, player.count(objs.chisel))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
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

    private fun GameTestState.runAmethystCuttingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            AmethystCuttingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(AmethystCuttingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val cutting: AmethystCutting,
        val recipes: AmethystCuttingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
