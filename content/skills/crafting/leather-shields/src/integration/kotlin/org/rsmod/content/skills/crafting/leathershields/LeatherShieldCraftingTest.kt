package org.rsmod.content.skills.crafting.leathershields

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class LeatherShieldCraftingTest {
    @Test
    fun GameTestState.`cache 233 exposes the complete leather shield recipe table`() =
        runLeatherShieldTest { deps ->
            assertEquals(
                listOf(
                    "Hard leather shield:41:1743+22251+4819=22269:7386:70.0",
                    "Snakeskin shield:56:6289+22254+4820=22272:7387:100.0",
                    "Green d'hide shield:62:1745+22257+1539=22275:7831:124.0",
                    "Blue d'hide shield:69:2505+22260+4822=22278:7832:140.0",
                    "Red d'hide shield:76:2507+22263+4823=22281:7833:156.0",
                    "Black d'hide shield:83:2509+22266+4824=22284:7834:172.0",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.leather.id}+${it.base.id}+${it.nails.id}=" +
                        "${it.output.id}:${it.animation.id}:${it.xp}"
                },
            )
            assertEquals(2347, objs.hammer.id)
            assertEquals(25644, objs.imcando_hammer.id)
            assertEquals(29775, objs.imcando_hammer_offhand.id)
        }

    @Test
    fun GameTestState.`every recipe transforms after five ticks with exact xp and animation`() =
        runLeatherShieldTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.hammer)
                addInputs(recipe)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.crafting.start(this, recipe) }

                assertEquals(recipe.animation.id, player.pendingSequence.id, recipe.name)
                advance(4)
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.leather), recipe.name)
                assertEquals(0, player.count(recipe.base), recipe.name)
                assertEquals(0, player.count(recipe.nails), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`regular and both imcando hammer forms work held or unequipped`() =
        runLeatherShieldTest { deps ->
            val recipe = deps.recipes.recipes.single { it.output == objs.green_dhide_shield }
            val setups: List<GameTestScope.() -> Unit> =
                listOf(
                    { add(objs.hammer) },
                    { add(objs.imcando_hammer) },
                    { player.worn[Wearpos.RightHand.slot] = InvObj(objs.imcando_hammer) },
                    { player.worn[Wearpos.LeftHand.slot] = InvObj(objs.imcando_hammer_offhand) },
                )

            for (setup in setups) {
                preparePlayer()
                setup()
                addInputs(recipe)

                player.withProtectedAccess { deps.crafting.start(this, recipe) }
                advance(5)

                assertEquals(1, player.count(recipe.output))
            }
        }

    @Test
    fun GameTestState.`membership level tool and material failures are atomic`() =
        runLeatherShieldTest { deps ->
            val recipe = deps.recipes.recipes.single { it.output == objs.black_dhide_shield }

            preparePlayer()
            player.members = false
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need to be on a members' world to make this shield.")
            assertInputsRemain(recipe)

            preparePlayer()
            player.stats[stats.crafting] = recipe.level - 1
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need a Crafting level of 83 to make this shield.")
            assertInputsRemain(recipe)

            preparePlayer()
            addInputs(recipe)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need a hammer to make this shield.")
            assertInputsRemain(recipe)

            preparePlayer()
            add(objs.hammer)
            add(recipe.leather)
            add(recipe.base)
            add(recipe.nails, 15)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need two pieces of the correct leather to make this shield.")
            assertEquals(1, player.count(recipe.leather))
            assertEquals(1, player.count(recipe.base))
            assertEquals(15, player.count(recipe.nails))

            preparePlayer()
            add(objs.hammer)
            add(recipe.leather, 2)
            add(recipe.nails, 15)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need the correct wooden shield to make this shield.")
            assertEquals(2, player.count(recipe.leather))
            assertEquals(15, player.count(recipe.nails))

            preparePlayer()
            add(objs.hammer)
            add(recipe.leather, 2)
            add(recipe.base)
            add(recipe.nails, 14)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need 15 of the correct nails to make this shield.")
            assertEquals(2, player.count(recipe.leather))
            assertEquals(1, player.count(recipe.base))
            assertEquals(14, player.count(recipe.nails))
        }

    @Test
    fun GameTestState.`full inventory succeeds through the many-to-one transaction`() =
        runLeatherShieldTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.hard_leather_shield }
            add(objs.hammer)
            addInputs(recipe)
            add(objs.leather, 23)
            assertEquals(28, player.inv.count { it != null })

            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            advance(5)

            assertEquals(1, player.count(recipe.output))
            assertEquals(25, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`one use-on action crafts only one shield`() = runLeatherShieldTest { deps ->
        preparePlayer()
        val recipe = deps.recipes.recipes.single { it.output == objs.snakeskin_shield }
        add(objs.hammer)
        add(recipe.leather, 4)
        add(recipe.base, 2)
        add(recipe.nails, 30)

        player.withProtectedAccess { deps.crafting.start(this, recipe) }
        advance(10)

        assertEquals(1, player.count(recipe.output))
        assertEquals(2, player.count(recipe.leather))
        assertEquals(1, player.count(recipe.base))
        assertEquals(15, player.count(recipe.nails))
        assertEquals(0, player.weakQueueList.count(crafting_queues.leatherShieldCrafting))
    }

    @Test
    fun GameTestState.`movement material loss and another Crafting action stop safely`() =
        runLeatherShieldTest { deps ->
            val recipe = deps.recipes.recipes.single { it.output == objs.red_dhide_shield }
            preparePlayer()
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess {
                weakQueue(crafting_queues.weaving, 20)
                deps.crafting.start(this, recipe)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
            player.clearPendingAction()
            advance(5)
            assertEquals(0, player.count(recipe.output))
            assertInputsRemain(recipe)

            preparePlayer()
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            player.inv[player.inv.indexOfFirst { it?.id == recipe.nails.id }] = null
            advance(5)
            assertEquals(0, player.count(recipe.output))
            assertEquals(2, player.count(recipe.leather))
            assertEquals(1, player.count(recipe.base))
            assertEquals(0, player.weakQueueList.count(crafting_queues.leatherShieldCrafting))

            preparePlayer()
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.hammer.id }] = null
            advance(5)
            assertEquals(0, player.count(recipe.output))
            assertInputsRemain(recipe)

            preparePlayer()
            add(objs.hammer)
            addInputs(recipe)
            player.withProtectedAccess {
                deps.crafting.start(this, recipe)
                clearCraftingQueues()
                weakQueue(crafting_queues.leatherAddonCrafting, 20)
            }
            advance(5)
            assertEquals(0, player.count(recipe.output))
            assertInputsRemain(recipe)
        }

    @Test
    fun GameTestState.`held interaction accepts reverse item order`() =
        runLeatherShieldTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.blue_dhide_shield }
            add(objs.hammer)
            addInputs(recipe)
            val leatherSlot = player.inv.indexOfFirst { it?.id == recipe.leather.id }
            val baseSlot = player.inv.indexOfFirst { it?.id == recipe.base.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.base],
                    baseSlot,
                    objTypes[recipe.leather],
                    leatherSlot,
                )
            }
            advance(5)

            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(recipe.leather))
            assertEquals(0, player.count(recipe.base))
            assertEquals(0, player.count(recipe.nails))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.addInputs(recipe: LeatherShieldRecipe) {
        add(recipe.leather, 2)
        add(recipe.base)
        add(recipe.nails, 15)
    }

    private fun GameTestScope.assertInputsRemain(recipe: LeatherShieldRecipe) {
        assertEquals(2, player.count(recipe.leather))
        assertEquals(1, player.count(recipe.base))
        assertEquals(15, player.count(recipe.nails))
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

    private fun GameTestState.runLeatherShieldTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            LeatherShieldCraftingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(LeatherShieldCraftingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: LeatherShieldCrafting,
        val recipes: LeatherShieldRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
