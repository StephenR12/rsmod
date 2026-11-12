package org.rsmod.content.skills.crafting.pottery

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class PotteryTest {
    @Test
    fun GameTestState.`cache 233 exposes the exact six ordinary pottery contracts`() =
        runPotteryTest { deps ->
            assertEquals(
                listOf(
                    "Pot:1:1787:1931:6.3:6.3:1:false:false",
                    "Empty cup:3:28193:1980:8.5:8.5:4:true:false",
                    "Pie dish:7:1789:2313:15.0:10.0:1:false:false",
                    "Bowl:8:1791:1923:18.0:15.0:1:false:false",
                    "Empty plant pot:19:5352:5350:20.0:17.5:1:true:false",
                    "Pot lid:25:4438:4440:20.0:20.0:1:true:true",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.unfired.id}:${it.fired.id}:${it.shapingXp}:" +
                        "${it.firingXp}:${it.outputCount}:${it.members}:${it.questLocked}"
                },
            )
            assertEquals(883, seqs.human_potterywheel.id)
            assertEquals(1317, seqs.potteryoven_quick.id)
            assertEquals(7, pottery_locs.wheels.size)
            assertEquals(4, pottery_locs.ovens.size)
        }

    @Test
    fun GameTestState.`wheel opens the native six-product quantity menu`() =
        runPotteryTest { deps ->
            preparePlayer()
            val wheel = prepareWheel()
            add(objs.soft_clay, 2)

            player.withProtectedAccess { deps.shaping.select(this, wheel) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.unfired_pot.id,
                    objs.unfired_cup.id,
                    objs.unfired_pie_dish.id,
                    objs.unfired_bowl.id,
                    objs.unfired_plant_pot.id,
                    objs.unfired_pot_lid.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "What would you like to make?",
                ),
                setup.values,
            )
        }

    @Test
    fun GameTestState.`all six shapes consume one clay and award exact xp`() =
        runPotteryTest { deps ->
            val wheel = prepareWheel()
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                player.setVarp(varps.onesmallfavour, 235)
                add(objs.soft_clay)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.shaping.start(this, wheel, recipe, quantity = 1) }
                advance(3)

                assertEquals(0, player.count(objs.soft_clay), recipe.name)
                assertEquals(recipe.outputCount, player.count(recipe.unfired), recipe.name)
                assertEquals(
                    (recipe.shapingXp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`pot lid requires the exact learned quest stage`() = runPotteryTest { deps ->
        val wheel = prepareWheel()
        val recipe = deps.recipes.recipes.single { it.unfired == objs.unfired_pot_lid }
        preparePlayer()
        player.setVarp(varps.onesmallfavour, 234)
        add(objs.soft_clay)
        player.withProtectedAccess { deps.shaping.start(this, wheel, recipe, quantity = 1) }
        assertMessageSent("You need to learn how to make pot lids during One Small Favour.")
        assertEquals(1, player.count(objs.soft_clay))

        player.setVarp(varps.onesmallfavour, 235)
        player.withProtectedAccess { deps.shaping.start(this, wheel, recipe, quantity = 1) }
        advance(3)
        assertEquals(1, player.count(objs.unfired_pot_lid))
    }

    @Test
    fun GameTestState.`shaping enforces membership level space and interruption atomically`() =
        runPotteryTest { deps ->
            val wheel = prepareWheel()
            val cup = deps.recipes.recipes.single { it.unfired == objs.unfired_cup }
            preparePlayer()
            player.members = false
            add(objs.soft_clay)
            player.withProtectedAccess { deps.shaping.start(this, wheel, cup, quantity = 1) }
            assertMessageSent("You need to be on a members' world to make this item.")
            assertEquals(1, player.count(objs.soft_clay))

            preparePlayer()
            player.stats[stats.crafting] = 2
            add(objs.soft_clay)
            player.withProtectedAccess { deps.shaping.start(this, wheel, cup, quantity = 1) }
            assertMessageSent("You need a Crafting level of 3 to make this item.")
            assertEquals(1, player.count(objs.soft_clay))

            preparePlayer()
            repeat(28) { add(if (it == 0) objs.soft_clay else objs.coins) }
            player.withProtectedAccess { deps.shaping.start(this, wheel, cup, quantity = 1) }
            assertMessageSent("You do not have enough inventory space to make this item.")
            assertEquals(1, player.count(objs.soft_clay))

            preparePlayer()
            val pot = deps.recipes.recipes.first()
            add(objs.soft_clay, 2)
            player.withProtectedAccess {
                weakQueue(crafting_queues.spinning, 20)
                deps.shaping.start(this, wheel, pot, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.spinning))
            advance(3)
            assertEquals(1, player.count(objs.unfired_pot))
            player.placeAt(wheel.coords.translateX(4))
            advance(3)
            assertEquals(1, player.count(objs.unfired_pot))
            assertEquals(1, player.count(objs.soft_clay))
        }

    @Test
    fun GameTestState.`all six pottery types fire with exact xp and no item level gate`() =
        runPotteryTest { deps ->
            val oven = prepareOven()
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                player.stats[stats.crafting] = 1
                add(recipe.unfired)
                random.next = 0
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.firing.start(this, oven, recipe, quantity = 1) }
                advance(7)

                assertEquals(0, player.count(recipe.unfired), recipe.name)
                assertEquals(1, player.count(recipe.fired), recipe.name)
                assertEquals(
                    (recipe.firingXp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`firing uses the exact level one through thirteen success curve`() =
        runPotteryTest { deps ->
            val oven = prepareOven()
            val pot = deps.recipes.recipes.first()
            val thresholds = listOf(181, 187, 194, 200, 206, 213, 219, 225, 232, 238, 244, 251, 256)
            for ((index, threshold) in thresholds.withIndex()) {
                preparePlayer()
                player.stats[stats.crafting] = index + 1
                add(pot.unfired)
                random.next = threshold - 1
                player.withProtectedAccess { deps.firing.start(this, oven, pot, quantity = 1) }
                advance(7)
                assertEquals(1, player.count(pot.fired), "level ${index + 1}")

                if (threshold < 256) {
                    preparePlayer()
                    player.stats[stats.crafting] = index + 1
                    add(pot.unfired)
                    random.next = threshold
                    player.withProtectedAccess { deps.firing.start(this, oven, pot, quantity = 1) }
                    advance(7)
                    assertEquals(0, player.count(pot.fired), "level ${index + 1}")
                    assertEquals(0, player.count(pot.unfired), "level ${index + 1}")
                    assertMessageSent("The pottery cracks in the oven.")
                }
            }
        }

    @Test
    fun GameTestState.`firing repeats every seven ticks and stops on movement`() =
        runPotteryTest { deps ->
            preparePlayer()
            val oven = prepareOven()
            val recipe = deps.recipes.recipes.first()
            add(recipe.unfired, 2)
            random.next = 0
            player.withProtectedAccess {
                weakQueue(crafting_queues.glassblowing, 20)
                deps.firing.start(this, oven, recipe, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.glassblowing))
            advance(7)
            assertEquals(1, player.count(recipe.fired))
            player.placeAt(oven.coords.translateX(4))
            advance(7)
            assertEquals(1, player.count(recipe.fired))
            assertEquals(1, player.count(recipe.unfired))
            assertEquals(0, player.weakQueueList.count(crafting_queues.potteryFiring))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
        player.setVarp(varps.onesmallfavour, 0)
    }

    private fun GameTestScope.prepareWheel(): BoundLocInfo {
        val wheel = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), pottery_locs.standardWheel)
        player.placeAt(wheel.coords.translateX(-1))
        return wheel
    }

    private fun GameTestScope.prepareOven(): BoundLocInfo {
        val oven = placeMapLoc(CoordGrid(0, 50, 51, 34, 31), pottery_locs.standardOven)
        player.placeAt(oven.coords.translateX(-1))
        return oven
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runPotteryTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, PotteryScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(PotteryModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val shaping: PotteryShaping,
        val firing: PotteryFiring,
        val recipes: PotteryRecipeRegistry,
    )
}
