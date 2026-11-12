package org.rsmod.content.skills.fletching.crossbowassembly

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
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class CrossbowAssemblyTest {
    @Test
    fun GameTestState.`cache 233 pins all standard crossbow contracts`() =
        runCrossbowAssemblyTest { deps ->
            assertEquals(
                listOf(
                    "Bronze crossbow:9:9440:9420:9454:9174:12.0:6.0:4436:6671",
                    "Blurite crossbow:24:9442:9422:9456:9176:32.0:16.0:4437:6672",
                    "Iron crossbow:39:9444:9423:9457:9177:44.0:22.0:4438:6673",
                    "Steel crossbow:46:9446:9425:9459:9179:54.0:27.0:4439:6674",
                    "Mithril crossbow:54:9448:9427:9461:9181:64.0:32.0:4440:6675",
                    "Adamant crossbow:61:9450:9429:9463:9183:82.0:41.0:4441:6676",
                    "Rune crossbow:69:9452:9431:9465:9185:100.0:50.0:4442:6677",
                    "Dragon crossbow:78:21952:21918:21921:21902:135.0:70.0:7860:7961",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.stock.id}:${it.limbs.id}:${it.unstrung.id}:" +
                        "${it.output.id}:${it.limbXp}:${it.stringXp}:${it.limbSequence.id}:" +
                        it.stringSequence.id
                },
            )
            assertEquals(2347, objs.hammer.id)
            assertEquals(9438, objs.crossbow_string.id)
        }

    @Test
    fun GameTestState.`both stages open native one-product quantity menus`() =
        runCrossbowAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.single { it.name == "Rune crossbow" }
            preparePlayer()
            add(recipe.stock, 2)
            add(recipe.limbs, 3)
            add(objs.hammer)

            player.withProtectedAccess {
                deps.assembly.select(this, recipe, CrossbowAssemblyStage.Limbs)
            }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            var setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(recipe.unstrung.id, setup.values[2])
            assertEquals("How many would you like to make?", setup.values.last())

            resumeSkillMulti(quantity = 1)
            advance(2)
            client.clear()
            player.clearAllInvs()
            add(recipe.unstrung, 3)
            add(objs.crossbow_string, 2)
            player.withProtectedAccess {
                deps.assembly.select(this, recipe, CrossbowAssemblyStage.String)
            }
            setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(2, setup.values[1])
            assertEquals(recipe.output.id, setup.values[2])
        }

    @Test
    fun GameTestState.`every tier completes both stages atomically with exact xp and messages`() =
        runCrossbowAssemblyTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.stock)
                add(recipe.limbs)
                add(objs.hammer)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess {
                    deps.assembly.process(
                        this,
                        CrossbowAssemblyRequest(
                            recipe.unstrung,
                            CrossbowAssemblyStage.Limbs,
                            remaining = 1,
                        ),
                    )
                }
                assertEquals(0, player.count(recipe.stock), recipe.name)
                assertEquals(0, player.count(recipe.limbs), recipe.name)
                assertEquals(1, player.count(recipe.unstrung), recipe.name)
                assertMessageSent(
                    "You attach the stock to the limbs and create an unstrung crossbow."
                )

                add(objs.crossbow_string)
                player.withProtectedAccess {
                    deps.assembly.process(
                        this,
                        CrossbowAssemblyRequest(
                            recipe.unstrung,
                            CrossbowAssemblyStage.String,
                            remaining = 1,
                        ),
                    )
                }
                assertEquals(0, player.count(recipe.unstrung), recipe.name)
                assertEquals(0, player.count(objs.crossbow_string), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    ((recipe.limbXp + recipe.stringXp) * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertMessageSent("You add a string to the crossbow.")
            }
        }

    @Test
    fun GameTestState.`membership level tool and material failures do not mutate inventory`() =
        runCrossbowAssemblyTest { deps ->
            val dragon = deps.recipes.recipes.last()
            preparePlayer()
            player.members = false
            add(dragon.stock)
            add(dragon.limbs)
            add(objs.hammer)
            player.withProtectedAccess {
                deps.assembly.start(this, dragon, CrossbowAssemblyStage.Limbs, 1)
            }
            assertMessageSent("You need to be on a members' world to make this crossbow.")
            assertEquals(1, player.count(dragon.stock))

            preparePlayer()
            player.stats[stats.fletching] = 77
            add(dragon.unstrung)
            add(objs.crossbow_string)
            player.withProtectedAccess {
                deps.assembly.start(this, dragon, CrossbowAssemblyStage.String, 1)
            }
            assertMessageSent("You need a Fletching level of 78 to make this crossbow.")
            assertEquals(1, player.count(dragon.unstrung))

            preparePlayer()
            add(dragon.stock)
            add(dragon.limbs)
            player.withProtectedAccess {
                deps.assembly.start(this, dragon, CrossbowAssemblyStage.Limbs, 1)
            }
            assertMessageSent("You need a hammer to make this crossbow.")
            assertEquals(1, player.count(dragon.stock))

            preparePlayer()
            add(dragon.unstrung)
            player.withProtectedAccess {
                deps.assembly.start(this, dragon, CrossbowAssemblyStage.String, 1)
            }
            assertMessageSent("You need a crossbow string to string this crossbow.")
            assertEquals(1, player.count(dragon.unstrung))
        }

    @Test
    fun GameTestState.`normal and equipped Imcando hammers support limb assembly`() =
        runCrossbowAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.first()
            val hammers =
                listOf(
                    objs.hammer to null,
                    objs.imcando_hammer to Wearpos.RightHand,
                    objs.imcando_hammer_offhand to Wearpos.LeftHand,
                )
            for ((hammer, wearpos) in hammers) {
                preparePlayer()
                add(recipe.stock)
                add(recipe.limbs)
                if (wearpos == null) {
                    add(hammer)
                } else {
                    player.worn[wearpos.slot] = InvObj(hammer)
                }
                player.withProtectedAccess {
                    deps.assembly.start(this, recipe, CrossbowAssemblyStage.Limbs, 1)
                }
                advance(2)
                assertEquals(1, player.count(recipe.unstrung), hammer.toString())
            }
        }

    @Test
    fun GameTestState.`both stages produce one result every two ticks`() =
        runCrossbowAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.single { it.name == "Mithril crossbow" }
            preparePlayer()
            add(recipe.stock, 3)
            add(recipe.limbs, 3)
            add(objs.hammer)
            player.withProtectedAccess {
                deps.assembly.start(this, recipe, CrossbowAssemblyStage.Limbs, 3)
            }
            assertEquals(recipe.limbSequence.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.unstrung))
            advance()
            assertEquals(1, player.count(recipe.unstrung))
            advance(2)
            assertEquals(2, player.count(recipe.unstrung))
            advance(2)
            assertEquals(3, player.count(recipe.unstrung))

            add(objs.crossbow_string, 3)
            player.withProtectedAccess {
                deps.assembly.start(this, recipe, CrossbowAssemblyStage.String, 3)
            }
            assertEquals(recipe.stringSequence.id, player.pendingSequence.id)
            advance()
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(1, player.count(recipe.output))
            advance(4)
            assertEquals(3, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(fletching_queues.crossbowAssembly))
        }

    @Test
    fun GameTestState.`resource loss and another Fletching action interrupt batches safely`() =
        runCrossbowAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.single { it.name == "Steel crossbow" }
            preparePlayer()
            add(recipe.stock, 2)
            add(recipe.limbs, 2)
            add(objs.hammer)
            player.withProtectedAccess {
                deps.assembly.start(this, recipe, CrossbowAssemblyStage.Limbs, 2)
            }
            advance(2)
            player.inv[player.inv.indexOfFirst { it?.id == objs.hammer.id }] = null
            advance(2)
            assertEquals(1, player.count(recipe.unstrung))
            assertEquals(1, player.count(recipe.stock))
            assertEquals(0, player.weakQueueList.count(fletching_queues.crossbowAssembly))

            preparePlayer()
            add(recipe.unstrung)
            add(objs.crossbow_string)
            player.withProtectedAccess {
                weakQueue(fletching_queues.ammunition, 20)
                deps.assembly.start(this, recipe, CrossbowAssemblyStage.String, 1)
            }
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.weakQueueList.count(fletching_queues.crossbowAssembly))
        }

    @Test
    fun GameTestState.`full inventory and reverse held-use dispatch are supported`() =
        runCrossbowAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(recipe.stock, 13)
            add(recipe.limbs, 13)
            add(objs.hammer)
            add(objs.feather)
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess {
                deps.assembly.start(this, recipe, CrossbowAssemblyStage.Limbs, 1)
            }
            advance(2)
            assertEquals(1, player.count(recipe.unstrung))

            preparePlayer()
            add(recipe.stock)
            add(recipe.limbs)
            add(objs.hammer)
            val stockSlot = player.inv.indexOfFirst { it?.id == recipe.stock.id }
            val limbSlot = player.inv.indexOfFirst { it?.id == recipe.limbs.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.limbs],
                    limbSlot,
                    objTypes[recipe.stock],
                    stockSlot,
                )
            }
            resumeSkillMulti(quantity = 1)
            advance(2)
            assertEquals(1, player.count(recipe.unstrung))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].isStackable) {
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

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runCrossbowAssemblyTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            CrossbowAssemblyScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(CrossbowAssemblyModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val assembly: CrossbowAssembly,
        val recipes: CrossbowAssemblyRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
