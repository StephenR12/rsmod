package org.rsmod.content.skills.crafting.weaving

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class WeavingTest {
    @Test
    fun GameTestState.`successful basket weaving on Falador farm loom awards the diary task`() = runWeavingTest { deps ->
        preparePlayer()
        val coords = CoordGrid(3039, 3287)
        allocZoneCollision(coords)
        val loom = placeMapLoc(coords, weaving_locs.all.first { it.id == 8717 })
        player.placeAt(coords.translateX(-1))
        add(objs.willow_branch, 6)
        val recipe = deps.recipes.recipes.single { it.output == objs.basket }
        player.withProtectedAccess { deps.weaving.start(this, loom, recipe, 1) }
        advance(4)
        assertEquals(1, player.count(objs.basket))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.WeaveFaladorBasket))
    }

    @Test
    fun GameTestState.`cache 233 exposes four weaving recipes looms and animation`() =
        runWeavingTest { deps ->
            assertEquals(
                listOf(
                    "Strip of cloth:10:1759x4:3224:12.0:4/3",
                    "Empty sack:21:5931x4:5418:38.0:4/3",
                    "Drift net:26:5931x2:21652:55.0:3/3",
                    "Basket:36:5933x6:5376:56.0:4/3",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.input.id}x${it.inputCount}:${it.output.id}:" +
                        "${it.xp}:${it.firstActionTicks}/${it.repeatActionTicks}"
                },
            )
            assertEquals(listOf(787, 8717, 31432), weaving_locs.all.map { it.id })
            assertEquals(2270, seqs.human_loom_weaving.id)
            assertTrue(deps.recipes.recipes.all { deps.objTypes[it.output].members })
        }

    @Test
    fun GameTestState.`loom opens the native four-product quantity menu`() =
        runWeavingTest { deps ->
            preparePlayer()
            val loom = prepareLoom()
            add(objs.ball_of_wool, 8)

            player.withProtectedAccess { deps.weaving.select(this, loom) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.strip_of_cloth.id,
                    objs.empty_sack.id,
                    objs.drift_net.id,
                    objs.basket.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "What would you like to weave?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..18
                }
            assertEquals(4, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 2 })
        }

    @Test
    fun GameTestState.`all four outputs consume exact materials and award exact xp`() =
        runWeavingTest { deps ->
            val loom = prepareLoom()
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.input, recipe.inputCount)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.weaving.start(this, loom, recipe, quantity = 1) }

                assertEquals(seqs.human_loom_weaving.id, player.pendingSequence.id)
                advance(recipe.firstActionTicks)
                assertEquals(0, player.count(recipe.input), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`ordinary weaving starts at four ticks then repeats every three ticks`() =
        runWeavingTest { deps ->
            preparePlayer()
            val loom = prepareLoom()
            val cloth = deps.recipes.recipes.single { it.output == objs.strip_of_cloth }
            add(objs.ball_of_wool, 8)

            player.withProtectedAccess { deps.weaving.start(this, loom, cloth, quantity = 2) }

            advance(3)
            assertEquals(0, player.count(objs.strip_of_cloth))
            advance()
            assertEquals(1, player.count(objs.strip_of_cloth))
            advance(2)
            assertEquals(1, player.count(objs.strip_of_cloth))
            advance()
            assertEquals(2, player.count(objs.strip_of_cloth))
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
        }

    @Test
    fun GameTestState.`drift nets take three ticks for first and repeated actions`() =
        runWeavingTest { deps ->
            preparePlayer()
            val loom = prepareLoom()
            val net = deps.recipes.recipes.single { it.output == objs.drift_net }
            add(objs.jute_fibre, 4)

            player.withProtectedAccess { deps.weaving.start(this, loom, net, quantity = 2) }

            advance(2)
            assertEquals(0, player.count(objs.drift_net))
            advance()
            assertEquals(1, player.count(objs.drift_net))
            advance(2)
            assertEquals(1, player.count(objs.drift_net))
            advance()
            assertEquals(2, player.count(objs.drift_net))
        }

    @Test
    fun GameTestState.`membership level and material failures are atomic`() =
        runWeavingTest { deps ->
            val loom = prepareLoom()
            val cloth = deps.recipes.recipes.single { it.output == objs.strip_of_cloth }

            preparePlayer()
            player.members = false
            add(objs.ball_of_wool, 4)
            player.withProtectedAccess { deps.weaving.select(this, loom) }
            assertMessageSent("You need to be on a members' world to use a loom.")
            assertEquals(4, player.count(objs.ball_of_wool))
            assertTrue(client.filterIsInstance<IfOpenSub>().none { it.interfaceId == 270 })

            preparePlayer()
            player.stats[stats.crafting] = 9
            add(objs.ball_of_wool, 4)
            player.withProtectedAccess { deps.weaving.start(this, loom, cloth, quantity = 1) }
            assertMessageSent("You need a Crafting level of 10 to make this item.")
            assertEquals(4, player.count(objs.ball_of_wool))

            preparePlayer()
            add(objs.ball_of_wool, 3)
            player.withProtectedAccess { deps.weaving.start(this, loom, cloth, quantity = 1) }
            assertMessageSent("You do not have enough material to make that.")
            assertEquals(3, player.count(objs.ball_of_wool))

            preparePlayer()
            player.withProtectedAccess { deps.weaving.select(this, loom) }
            assertMessageSent("You need something suitable to weave.")
        }

    @Test
    fun GameTestState.`movement material loss and another Crafting action stop the batch`() =
        runWeavingTest { deps ->
            val loom = prepareLoom()
            val basket = deps.recipes.recipes.single { it.output == objs.basket }
            preparePlayer()
            add(objs.willow_branch, 12)
            player.withProtectedAccess {
                weakQueue(crafting_queues.spinning, 20)
                deps.weaving.start(this, loom, basket, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.spinning))
            advance(4)
            assertEquals(1, player.count(objs.basket))
            player.placeAt(loom.coords.translateX(4))
            advance(3)
            assertEquals(1, player.count(objs.basket))
            assertEquals(6, player.count(objs.willow_branch))
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))

            preparePlayer()
            player.placeAt(loom.coords.translateX(-1))
            add(objs.willow_branch, 12)
            player.withProtectedAccess { deps.weaving.start(this, loom, basket, quantity = 2) }
            advance(4)
            while (player.count(objs.willow_branch) >= 6) {
                player.inv[player.inv.indexOfFirst { it?.id == objs.willow_branch.id }] = null
            }
            advance(3)
            assertEquals(1, player.count(objs.basket))
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.prepareLoom(): BoundLocInfo {
        val loom = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), weaving_locs.standard)
        player.placeAt(loom.coords.translateX(-1))
        return loom
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runWeavingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, WeavingScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(WeavingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val weaving: Weaving,
        val recipes: WeavingRecipeRegistry,
        val objTypes: ObjTypeList,
    )
}
