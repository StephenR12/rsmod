package org.rsmod.content.skills.fletching.bolttipcutting

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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class BoltTipCuttingTest {
    @Test
    fun GameTestState.`cache 233 pins every Fletching bolt tip recipe and sequence`() =
        runBoltTipCuttingTest { deps ->
            assertEquals(
                listOf(
                    "Opal bolt tips:11:1609:45:12:1.5:890:false",
                    "Jade bolt tips:26:1611:9187:12:2.0:891:false",
                    "Pearl bolt tips:41:411:46:6:3.2:4470:false",
                    "Pearl bolt tips:41:413:46:24:3.2:4470:false",
                    "Red topaz bolt tips:48:1613:9188:12:3.9:892:false",
                    "Sapphire bolt tips:56:1607:9189:12:4.0:888:false",
                    "Emerald bolt tips:58:1605:9190:12:5.5:889:false",
                    "Ruby bolt tips:63:1603:9191:12:6.3:887:false",
                    "Diamond bolt tips:65:1601:9192:12:7.0:886:false",
                    "Dragonstone bolt tips:71:1615:9193:12:8.2:885:true",
                    "Onyx bolt tips:73:6573:9194:24:9.4:2717:false",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.input.id}:${it.output.id}:${it.outputCount}:" +
                        "${it.xp}:${it.sequence.id}:${it.warnsBeforeCutting}"
                },
            )
            assertEquals(1755, objs.chisel.id)
            assertEquals(4470, seqs.xbows_fletching_pearl.id)
        }

    @Test
    fun GameTestState.`ordinary input opens the native one-product quantity menu`() =
        runBoltTipCuttingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.opal, 3)

            player.withProtectedAccess { deps.cutting.select(this, objs.opal) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(0, setup.values[0])
            assertEquals(3, setup.values[1])
            assertEquals(objs.opal_bolt_tips.id, setup.values[2])
            assertEquals(3, setup.values[12])
            assertEquals("How many would you like to make?", setup.values.last())
        }

    @Test
    fun GameTestState.`every recipe consumes one source and awards exact outputs and xp`() =
        runBoltTipCuttingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.chisel)
                add(recipe.input)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess {
                    deps.cutting.process(
                        this,
                        BoltTipCuttingRequest(recipe.input, recipe.output, remaining = 1),
                    )
                }

                assertEquals(0, player.count(recipe.input), recipe.name)
                assertEquals(recipe.outputCount, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
                assertMessageSent("You use your chisel to fletch small bolt tips.")
            }
        }

    @Test
    fun GameTestState.`selected gems are cut one every five ticks`() =
        runBoltTipCuttingTest { deps ->
            val recipe = deps.recipes.forInput(objs.diamond)!!
            preparePlayer()
            add(objs.chisel)
            add(recipe.input, 3)

            player.withProtectedAccess { deps.cutting.start(this, recipe, quantity = 3) }
            assertEquals(recipe.sequence.id, player.pendingSequence.id)
            advance(4)
            assertEquals(0, player.count(recipe.output))
            advance()
            assertEquals(12, player.count(recipe.output))
            advance(4)
            assertEquals(12, player.count(recipe.output))
            advance()
            assertEquals(24, player.count(recipe.output))
            advance(5)
            assertEquals(36, player.count(recipe.output))
            assertEquals(0, player.weakQueueList.count(fletching_queues.boltTipCutting))
        }

    @Test
    fun GameTestState.`dragonstone requires its exact warning before quantity selection`() =
        runBoltTipCuttingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.dragonstone, 2)

            player.withProtectedAccess { deps.cutting.select(this, objs.dragonstone) }
            val warning = client.filterIsInstance<RunClientScript>().single { it.id == 58 }
            assertEquals(
                listOf(
                    "Are you sure you want to cut this?",
                    "Yes, I want to cut my dragonstone into bolt tips.|" +
                        "No! Don't cut my lovely dragonstone!",
                ),
                warning.values,
            )
            confirmChoice()
            assertTrue(client.filterIsInstance<RunClientScript>().any { it.id == 2046 })
            resumeSkillMulti(quantity = 2)
            advance(10)
            assertEquals(24, player.count(objs.dragonstone_bolt_tips))

            preparePlayer()
            add(objs.chisel)
            add(objs.dragonstone)
            player.withProtectedAccess { deps.cutting.select(this, objs.dragonstone) }
            declineChoice()
            assertEquals(1, player.count(objs.dragonstone))
            assertEquals(0, player.count(objs.dragonstone_bolt_tips))
            assertMessageSent("You carefully take the chisel away from the expensive dragonstone.")
        }

    @Test
    fun GameTestState.`membership level chisel and source failures never mutate inventory`() =
        runBoltTipCuttingTest { deps ->
            val onyx = deps.recipes.forInput(objs.onyx)!!
            preparePlayer()
            player.members = false
            add(objs.chisel)
            add(objs.onyx)
            player.withProtectedAccess { deps.cutting.start(this, onyx, 1) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertEquals(1, player.count(objs.onyx))

            preparePlayer()
            player.stats[stats.fletching] = 72
            add(objs.chisel)
            add(objs.onyx)
            player.withProtectedAccess { deps.cutting.start(this, onyx, 1) }
            assertMessageSent("You need a Fletching level of 73 to make these bolt tips.")
            assertEquals(1, player.count(objs.onyx))

            preparePlayer()
            add(objs.onyx)
            player.withProtectedAccess { deps.cutting.start(this, onyx, 1) }
            assertMessageSent("You need a chisel to cut bolt tips.")
            assertEquals(1, player.count(objs.onyx))

            preparePlayer()
            add(objs.chisel)
            player.withProtectedAccess { deps.cutting.start(this, onyx, 1) }
            assertMessageSent("You do not have any onyx left.")
        }

    @Test
    fun GameTestState.`resource loss and another Fletching action interrupt batches safely`() =
        runBoltTipCuttingTest { deps ->
            val recipe = deps.recipes.forInput(objs.ruby)!!
            preparePlayer()
            add(objs.chisel)
            add(objs.ruby, 2)
            player.withProtectedAccess { deps.cutting.start(this, recipe, 2) }
            advance(5)
            player.inv[player.inv.indexOfFirst { it?.id == objs.chisel.id }] = null
            advance(5)
            assertEquals(12, player.count(objs.ruby_bolt_tips))
            assertEquals(1, player.count(objs.ruby))
            assertEquals(0, player.weakQueueList.count(fletching_queues.boltTipCutting))

            preparePlayer()
            add(objs.chisel)
            add(objs.ruby)
            player.withProtectedAccess {
                weakQueue(fletching_queues.ammunition, 20)
                deps.cutting.start(this, recipe, 1)
            }
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.weakQueueList.count(fletching_queues.boltTipCutting))
        }

    @Test
    fun GameTestState.`full inventory and reverse held-use dispatch are supported`() =
        runBoltTipCuttingTest { deps ->
            val recipe = deps.recipes.forInput(objs.opal)!!
            preparePlayer()
            add(objs.chisel)
            add(objs.opal, 27)
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess { deps.cutting.start(this, recipe, 1) }
            advance(5)
            assertEquals(12, player.count(objs.opal_bolt_tips))

            preparePlayer()
            add(objs.chisel)
            add(objs.oyster_pearl)
            val chiselSlot = player.inv.indexOfFirst { it?.id == objs.chisel.id }
            val pearlSlot = player.inv.indexOfFirst { it?.id == objs.oyster_pearl.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.oyster_pearl],
                    pearlSlot,
                    objTypes[objs.chisel],
                    chiselSlot,
                )
            }
            resumeSkillMulti(quantity = 1)
            advance(5)
            assertEquals(6, player.count(objs.pearl_bolt_tips))
            assertEquals(1, player.count(objs.chisel))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
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

    private fun GameTestScope.confirmChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
        )
    }

    private fun GameTestScope.declineChoice() {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 2)
        )
    }

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runBoltTipCuttingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BoltTipCuttingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BoltTipCuttingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val cutting: BoltTipCutting,
        val recipes: BoltTipCuttingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
