package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class ForgottenBrewUpgradingTest {
    @Test
    fun GameTestState.`cache 233 pins every Forgotten brew upgrade contract and dispatch`() =
        runUpgradeTest { deps ->
            assertEquals(27616, objs.ancient_essence.id)
            assertEquals(99, herblore_queues.forgottenBrewUpgrade.id)
            assertEquals(ForgottenBrewUpgrading.REQUIRED_LEVEL, 91)
            assertEquals(ForgottenBrewUpgrading.ACTION_TICKS, 3)
            assertEquals(
                listOf(
                    Contract(26346, 27638, 20, 360),
                    Contract(26344, 27635, 40, 720),
                    Contract(26342, 27632, 60, 1080),
                    Contract(26340, 27629, 80, 1450),
                ),
                ForgottenBrewUpgradeRecipes.all.map {
                    Contract(
                        it.ancientBrew.id,
                        it.forgottenBrew.id,
                        it.essence,
                        (it.xp * 10).toInt(),
                    )
                },
            )
            for (recipe in ForgottenBrewUpgradeRecipes.all) {
                assertTrue(deps.objs[recipe.ancientBrew].members)
                assertTrue(deps.objs[recipe.forgottenBrew].members)
                assertTrue(
                    deps.events.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(recipe.ancientBrew.id, objs.ancient_essence.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`every dose upgrades after exactly three ticks in both item orders`() =
        runUpgradeTest { deps ->
            for ((index, recipe) in ForgottenBrewUpgradeRecipes.all.withIndex()) {
                prepare()
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[BREW_SLOT] = InvObj(recipe.ancientBrew)
                player.inv[ESSENCE_SLOT] = InvObj(objs.ancient_essence, recipe.essence + 7)

                interact(deps, recipe, reverse = index % 2 == 1)
                advance(2)
                assertEquals(recipe.ancientBrew.id, player.inv[BREW_SLOT]?.id)
                assertEquals(recipe.essence + 7, player.inv[ESSENCE_SLOT]?.count)
                assertEquals(0, player.statMap.getFineXP(stats.herblore))

                advance()
                assertEquals(recipe.forgottenBrew.id, player.inv[BREW_SLOT]?.id)
                assertEquals(7, player.inv[ESSENCE_SLOT]?.count)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`boosted level succeeds while access and resource failures are atomic`() =
        runUpgradeTest { deps ->
            val recipe = ForgottenBrewUpgradeRecipes.all.last()

            prepare(level = 91, baseLevel = 90)
            addInputs(recipe)
            interact(deps, recipe)
            advance(3)
            assertEquals(recipe.forgottenBrew.id, player.inv[BREW_SLOT]?.id)

            prepare(level = 90)
            addInputs(recipe)
            interact(deps, recipe)
            assertAtomic(recipe)
            assertMessageSent("You need a Herblore level of 91 to make this potion.")

            prepare(members = false)
            addInputs(recipe)
            interact(deps, recipe)
            assertAtomic(recipe)
            assertMessageSent("You need to be on a members' world to make potions.")

            prepare(questState = 3)
            addInputs(recipe)
            interact(deps, recipe)
            assertAtomic(recipe)
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")

            prepare()
            addInputs(recipe, essence = recipe.essence - 1)
            interact(deps, recipe)
            assertAtomic(recipe, essence = recipe.essence - 1)
        }

    @Test
    fun GameTestState.`delayed upgrade revalidates slots and a new Herblore action cancels it`() =
        runUpgradeTest { deps ->
            val first = ForgottenBrewUpgradeRecipes.all.first()
            val second = ForgottenBrewUpgradeRecipes.all[1]
            prepare()
            addInputs(first)
            interact(deps, first)
            advance(2)
            player.inv[ESSENCE_SLOT] = InvObj(objs.coins)
            advance()
            assertEquals(first.ancientBrew.id, player.inv[BREW_SLOT]?.id)
            assertEquals(0, player.statMap.getFineXP(stats.herblore))

            prepare()
            addInputs(first)
            interact(deps, first)
            player.inv[SECOND_BREW_SLOT] = InvObj(second.ancientBrew)
            player.inv[SECOND_ESSENCE_SLOT] = InvObj(objs.ancient_essence, second.essence)
            player.withProtectedAccess {
                deps.upgrading.start(this, SECOND_BREW_SLOT, SECOND_ESSENCE_SLOT, second)
            }
            advance(3)
            assertEquals(first.ancientBrew.id, player.inv[BREW_SLOT]?.id)
            assertEquals(second.forgottenBrew.id, player.inv[SECOND_BREW_SLOT]?.id)
            assertEquals((second.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
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

    private fun GameTestScope.addInputs(
        recipe: ForgottenBrewUpgradeRecipe,
        essence: Int = recipe.essence,
    ) {
        player.inv[BREW_SLOT] = InvObj(recipe.ancientBrew)
        player.inv[ESSENCE_SLOT] = InvObj(objs.ancient_essence, essence)
    }

    private fun GameTestScope.interact(
        deps: Dependencies,
        recipe: ForgottenBrewUpgradeRecipe,
        reverse: Boolean = false,
    ) {
        player.withProtectedAccess {
            if (reverse) {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[objs.ancient_essence],
                    ESSENCE_SLOT,
                    deps.objs[recipe.ancientBrew],
                    BREW_SLOT,
                )
            } else {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs[recipe.ancientBrew],
                    BREW_SLOT,
                    deps.objs[objs.ancient_essence],
                    ESSENCE_SLOT,
                )
            }
        }
    }

    private fun GameTestScope.assertAtomic(
        recipe: ForgottenBrewUpgradeRecipe,
        essence: Int = recipe.essence,
    ) {
        assertEquals(recipe.ancientBrew.id, player.inv[BREW_SLOT]?.id)
        assertEquals(essence, player.inv[ESSENCE_SLOT]?.count)
        assertEquals(0, player.statMap.getFineXP(stats.herblore))
        assertEquals(0, player.weakQueueList.count(herblore_queues.forgottenBrewUpgrade))
    }

    private fun GameTestState.runUpgradeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val upgrading: ForgottenBrewUpgrading,
        val heldU: HeldUInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(
        val ancient: Int,
        val forgotten: Int,
        val essence: Int,
        val fineXp: Int,
    )

    private companion object {
        const val BREW_SLOT = 3
        const val ESSENCE_SLOT = 8
        const val SECOND_BREW_SLOT = 10
        const val SECOND_ESSENCE_SLOT = 11
    }
}
