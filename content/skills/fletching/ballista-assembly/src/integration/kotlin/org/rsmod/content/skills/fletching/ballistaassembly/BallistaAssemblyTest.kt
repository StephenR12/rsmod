package org.rsmod.content.skills.fletching.ballistaassembly

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class BallistaAssemblyTest {
    @Test
    fun GameTestState.`cache 233 pins both complete ballista assembly chains`() =
        runBallistaAssemblyTest { deps ->
            assertEquals(
                listOf(
                    "Incomplete light ballista:47:19586+19592=19595:15.0",
                    "Unstrung light ballista:47:19595+19601=19604:15.0",
                    "Light ballista:47:19604+19610=19478:300.0",
                    "Incomplete heavy ballista:72:19589+19592=19598:30.0",
                    "Unstrung heavy ballista:72:19598+19601=19607:30.0",
                    "Heavy ballista:72:19607+19610=19481:600.0",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.primary.id}+${it.secondary.id}=" +
                        "${it.output.id}:${it.xp}"
                },
            )
        }

    @Test
    fun GameTestState.`every stage atomically consumes both components and awards exact xp`() =
        runBallistaAssemblyTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.primary)
                add(recipe.secondary)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.assembly.assemble(this, recipe) }

                assertEquals(0, player.count(recipe.primary), recipe.name)
                assertEquals(0, player.count(recipe.secondary), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`light and heavy chains award exact total xp`() =
        runBallistaAssemblyTest { deps ->
            for ((recipes, expectedXp) in
                listOf(
                    deps.recipes.recipes.take(3) to 3300,
                    deps.recipes.recipes.takeLast(3) to 6600,
                )) {
                preparePlayer()
                add(recipes[0].primary)
                add(recipes[0].secondary)
                add(recipes[1].secondary)
                add(recipes[2].secondary)
                val startXp = player.statMap.getFineXP(stats.fletching)

                for (recipe in recipes) {
                    player.withProtectedAccess { deps.assembly.assemble(this, recipe) }
                }

                assertEquals(1, player.count(recipes.last().output))
                assertEquals(expectedXp, player.statMap.getFineXP(stats.fletching) - startXp)
            }
        }

    @Test
    fun GameTestState.`membership level and missing component failures are atomic`() =
        runBallistaAssemblyTest { deps ->
            val heavy = deps.recipes.recipes.last()
            preparePlayer()
            player.members = false
            add(heavy.primary)
            add(heavy.secondary)
            player.withProtectedAccess { deps.assembly.assemble(this, heavy) }
            assertMessageSent("You need to be on a members' world to make this ballista.")
            assertInputsRemain(heavy)

            preparePlayer()
            player.stats[stats.fletching] = 71
            add(heavy.primary)
            add(heavy.secondary)
            player.withProtectedAccess { deps.assembly.assemble(this, heavy) }
            assertMessageSent("You need a Fletching level of 72 to make this ballista.")
            assertInputsRemain(heavy)

            preparePlayer()
            add(heavy.primary)
            player.withProtectedAccess { deps.assembly.assemble(this, heavy) }
            assertMessageSent("You do not have the components needed to make this ballista.")
            assertEquals(1, player.count(heavy.primary))
            assertEquals(0, player.count(heavy.output))
        }

    @Test
    fun GameTestState.`full inventory and reverse held use complete one stage immediately`() =
        runBallistaAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(recipe.primary)
            add(recipe.secondary)
            add(recipe.primary, 26)
            assertEquals(0, player.inv.freeSpace())

            val primarySlot = player.inv.indexOfFirst { it?.id == recipe.primary.id }
            val secondarySlot = player.inv.indexOfFirst { it?.id == recipe.secondary.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.secondary],
                    secondarySlot,
                    objTypes[recipe.primary],
                    primarySlot,
                )
            }

            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`assembly cancels an existing Fletching batch without scheduling one`() =
        runBallistaAssemblyTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(recipe.primary)
            add(recipe.secondary)
            player.withProtectedAccess {
                weakQueue(fletching_queues.ammunition, 20)
                deps.assembly.assemble(this, recipe)
            }

            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(0, player.weakQueueList.count(fletching_queues.logCutting))
            assertEquals(1, player.count(recipe.output))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
    }

    private fun GameTestScope.assertInputsRemain(recipe: BallistaAssemblyRecipe) {
        assertEquals(1, player.count(recipe.primary))
        assertEquals(1, player.count(recipe.secondary))
        assertEquals(0, player.count(recipe.output))
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

    private fun GameTestState.runBallistaAssemblyTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BallistaAssemblyScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BallistaAssemblyModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val assembly: BallistaAssembly,
        val recipes: BallistaAssemblyRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
