package org.rsmod.content.skills.crafting.spinning

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
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SpinningTest {
    @Test
    fun GameTestState.`cache 233 exposes the five pre-cutoff spinning recipes`() =
        runSpinningTest { deps ->
            assertEquals(
                listOf(
                    "Ball of wool:1:1737:1759:2.5:false",
                    "Bow string:10:1779:1777:15.0:true",
                    "Crossbow string:10:9436:9438:15.0:true",
                    "Magic string:19:6051:6038:30.0:true",
                    "Rope:30:10814:954:25.0:true",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.inputs.first().id}:${it.output.id}:${it.xp}:${it.members}"
                },
            )
            assertEquals(13138, seqs.human_spinningwheel.id)
            assertEquals(11, spinning_locs.all.size)
        }

    @Test
    fun GameTestState.`wheel opens the native five-product quantity menu`() =
        runSpinningTest { deps ->
            preparePlayer()
            val wheel = prepareWheel()
            add(objs.wool, 2)

            player.withProtectedAccess { deps.spinning.select(this, wheel) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.ball_of_wool.id,
                    objs.bow_string.id,
                    objs.crossbow_string.id,
                    objs.magic_string.id,
                    objs.rope.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "What would you like to spin?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..19
                }
            assertEquals(5, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 2 })
        }

    @Test
    fun GameTestState.`all five outputs consume one material and award exact xp`() =
        runSpinningTest { deps ->
            val wheel = prepareWheel()
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.inputs.first())
                val startXp = player.statMap.getFineXP(stats.crafting)
                player.withProtectedAccess {
                    deps.spinning.start(this, wheel, recipe, recipe.inputs.first(), quantity = 1)
                }
                advance(3)
                assertEquals(0, player.count(recipe.inputs.first()), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`every ordinary root and sinew makes a crossbow string`() =
        runSpinningTest { deps ->
            val wheel = prepareWheel()
            val recipe = deps.recipes.recipes.single { it.output == objs.crossbow_string }
            assertEquals(listOf(9436, 6043, 6045, 6047, 6049, 6051), recipe.inputs.map(ObjType::id))
            for (material in recipe.inputs) {
                preparePlayer()
                add(material)
                player.withProtectedAccess {
                    deps.spinning.start(this, wheel, recipe, material, quantity = 1)
                }
                advance(3)
                assertEquals(0, player.count(material))
                assertEquals(1, player.count(objs.crossbow_string))
            }
        }

    @Test
    fun GameTestState.`spinning repeats every three ticks and stops on movement`() =
        runSpinningTest { deps ->
            preparePlayer()
            val wheel = prepareWheel()
            val recipe = deps.recipes.recipes.first()
            add(objs.wool, 2)
            player.withProtectedAccess {
                weakQueue(crafting_queues.glassblowing, 20)
                deps.spinning.start(this, wheel, recipe, objs.wool, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.glassblowing))
            advance(3)
            assertEquals(1, player.count(objs.ball_of_wool))
            player.placeAt(wheel.coords.translateX(4))
            advance(3)
            assertEquals(1, player.count(objs.ball_of_wool))
            assertEquals(1, player.count(objs.wool))
            assertEquals(0, player.weakQueueList.count(crafting_queues.spinning))
        }

    @Test
    fun GameTestState.`membership level and missing material failures are atomic`() =
        runSpinningTest { deps ->
            val wheel = prepareWheel()
            val bowString = deps.recipes.recipes.single { it.output == objs.bow_string }
            preparePlayer()
            player.members = false
            add(objs.flax)
            player.withProtectedAccess {
                deps.spinning.start(this, wheel, bowString, objs.flax, quantity = 1)
            }
            assertMessageSent("You need to be on a members' world to spin this material.")
            assertEquals(1, player.count(objs.flax))

            preparePlayer()
            player.stats[stats.crafting] = 9
            add(objs.flax)
            player.withProtectedAccess {
                deps.spinning.start(this, wheel, bowString, objs.flax, quantity = 1)
            }
            assertMessageSent("You need a Crafting level of 10 to make this item.")
            assertEquals(1, player.count(objs.flax))

            preparePlayer()
            player.withProtectedAccess { deps.spinning.select(this, wheel) }
            assertMessageSent("You need something suitable to spin.")
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.prepareWheel(): BoundLocInfo {
        val wheel = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), spinning_locs.standard)
        player.placeAt(wheel.coords.translateX(-1))
        return wheel
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runSpinningTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, SpinningScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SpinningModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val spinning: Spinning, val recipes: SpinningRecipeRegistry)
}
