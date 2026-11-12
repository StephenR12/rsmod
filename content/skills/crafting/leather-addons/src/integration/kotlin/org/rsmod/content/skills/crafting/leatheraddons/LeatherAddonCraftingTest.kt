package org.rsmod.content.skills.crafting.leatheraddons

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class LeatherAddonCraftingTest {
    @Test
    fun GameTestState.`cache 233 exposes all seven leather add-on recipes`() =
        runLeatherAddonTest { deps ->
            assertEquals(
                listOf(
                    "Studded body:41:1129+2370=1133:40.0",
                    "Studded chaps:44:1095+2370=1097:42.0",
                    "Spiky vambraces:32:1063+10113=10077:6.0",
                    "Green spiky vambraces:32:1065+10113=10079:6.0",
                    "Blue spiky vambraces:32:2487+10113=10081:6.0",
                    "Red spiky vambraces:32:2489+10113=10083:6.0",
                    "Black spiky vambraces:32:2491+10113=10085:6.0",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.base.id}+${it.addon.id}=${it.output.id}:${it.xp}"
                },
            )
            assertEquals(1249, seqs.human_leather_crafting.id)
        }

    @Test
    fun GameTestState.`every recipe transforms after three ticks with exact xp`() =
        runLeatherAddonTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.base)
                add(recipe.addon)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.crafting.start(this, recipe) }

                assertEquals(seqs.human_leather_crafting.id, player.pendingSequence.id)
                advance(2)
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.base), recipe.name)
                assertEquals(0, player.count(recipe.addon), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`a use-on action crafts only one add-on`() = runLeatherAddonTest { deps ->
        preparePlayer()
        val recipe = deps.recipes.recipes.single { it.output == objs.green_spiky_vambraces }
        add(recipe.base, 2)
        add(recipe.addon, 2)

        player.withProtectedAccess { deps.crafting.start(this, recipe) }
        advance(6)

        assertEquals(1, player.count(recipe.output))
        assertEquals(1, player.count(recipe.base))
        assertEquals(1, player.count(recipe.addon))
        assertEquals(0, player.weakQueueList.count(crafting_queues.leatherAddonCrafting))
    }

    @Test
    fun GameTestState.`membership level and material failures are atomic`() =
        runLeatherAddonTest { deps ->
            val recipe = deps.recipes.recipes.single { it.output == objs.studded_body }

            preparePlayer()
            player.members = false
            add(recipe.base)
            add(recipe.addon)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need to be on a members' world to make this item.")
            assertInputsRemain(recipe)

            preparePlayer()
            player.stats[stats.crafting] = 40
            add(recipe.base)
            add(recipe.addon)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You need a Crafting level of 41 to make this item.")
            assertInputsRemain(recipe)

            preparePlayer()
            add(recipe.addon)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You do not have the armour needed to make that.")
            assertEquals(1, player.count(recipe.addon))

            preparePlayer()
            add(recipe.base)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            assertMessageSent("You do not have the add-on needed to make that.")
            assertEquals(1, player.count(recipe.base))
        }

    @Test
    fun GameTestState.`full inventory succeeds through the two-to-one transaction`() =
        runLeatherAddonTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.studded_chaps }
            add(recipe.base)
            add(recipe.addon)
            add(objs.leather, 26)
            assertEquals(28, player.inv.count { it != null })

            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            advance(3)

            assertEquals(1, player.count(recipe.output))
            assertEquals(27, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`movement material loss and another Crafting action stop safely`() =
        runLeatherAddonTest { deps ->
            val recipe = deps.recipes.recipes.single { it.output == objs.black_spiky_vambraces }
            preparePlayer()
            add(recipe.base)
            add(recipe.addon)
            player.withProtectedAccess {
                weakQueue(crafting_queues.weaving, 20)
                deps.crafting.start(this, recipe)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
            player.clearPendingAction()
            advance(3)
            assertEquals(0, player.count(recipe.output))
            assertInputsRemain(recipe)

            preparePlayer()
            add(recipe.base)
            add(recipe.addon)
            player.withProtectedAccess { deps.crafting.start(this, recipe) }
            player.inv[player.inv.indexOfFirst { it?.id == recipe.addon.id }] = null
            advance(3)
            assertEquals(0, player.count(recipe.output))
            assertEquals(1, player.count(recipe.base))
            assertEquals(0, player.weakQueueList.count(crafting_queues.leatherAddonCrafting))
        }

    @Test
    fun GameTestState.`held interaction accepts reverse item order`() =
        runLeatherAddonTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.recipes.single { it.output == objs.blue_spiky_vambraces }
            add(recipe.base)
            add(recipe.addon)
            val baseSlot = player.inv.indexOfFirst { it?.id == recipe.base.id }
            val addonSlot = player.inv.indexOfFirst { it?.id == recipe.addon.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.addon],
                    addonSlot,
                    objTypes[recipe.base],
                    baseSlot,
                )
            }
            advance(3)

            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(recipe.base))
            assertEquals(0, player.count(recipe.addon))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.assertInputsRemain(recipe: LeatherAddonRecipe) {
        assertEquals(1, player.count(recipe.base))
        assertEquals(1, player.count(recipe.addon))
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

    private fun GameTestState.runLeatherAddonTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            LeatherAddonCraftingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(LeatherAddonCraftingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: LeatherAddonCrafting,
        val recipes: LeatherAddonRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
