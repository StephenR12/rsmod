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
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class HerbCleaningTest {
    @Test
    fun GameTestState.`cache 233 herb identities levels xp and Clean operations are pinned`() =
        runHerbCleaningTest { deps ->
            assertEquals(
                listOf(
                    Contract(199, 249, 3, 25),
                    Contract(201, 251, 5, 38),
                    Contract(203, 253, 11, 50),
                    Contract(205, 255, 20, 63),
                    Contract(207, 257, 25, 75),
                    Contract(3049, 2998, 30, 80),
                    Contract(209, 259, 40, 88),
                    Contract(211, 261, 48, 100),
                    Contract(213, 263, 54, 113),
                    Contract(30094, 30097, 58, 118),
                    Contract(3051, 3000, 59, 118),
                    Contract(215, 265, 65, 125),
                    Contract(2485, 2481, 67, 131),
                    Contract(217, 267, 70, 138),
                    Contract(219, 269, 75, 150),
                ),
                HerbCleaningRecipes.all.map {
                    Contract(it.grimy.id, it.clean.id, it.level, (it.xp * 10).toInt())
                },
            )
            assertTrue(HerbCleaningRecipes.all.all { deps.objs[it.grimy].members })
            assertTrue(HerbCleaningRecipes.all.all { deps.objs[it.clean].members })
            assertTrue(HerbCleaningRecipes.all.all { deps.objs[it.grimy].iop[0] == "Clean" })
        }

    @Test
    fun GameTestState.`every grimy herb cleans immediately through its cache operation`() =
        runHerbCleaningTest { deps ->
            for (recipe in HerbCleaningRecipes.all) {
                preparePlayer(level = recipe.level)
                player.inv[SLOT] = InvObj(recipe.grimy)

                clean(deps, SLOT)

                assertEquals(recipe.clean.id, player.inv[SLOT]?.id)
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState.`visible boosts meet cleaning levels while lower levels remain atomic`() =
        runHerbCleaningTest { deps ->
            val recipe = HerbCleaningRecipes.all.last()
            preparePlayer(level = recipe.level, baseLevel = recipe.level - 5)
            player.inv[SLOT] = InvObj(recipe.grimy)
            clean(deps, SLOT)
            assertEquals(recipe.clean.id, player.inv[SLOT]?.id)

            preparePlayer(level = recipe.level - 1)
            player.inv[SLOT] = InvObj(recipe.grimy)
            clean(deps, SLOT)
            assertEquals(recipe.grimy.id, player.inv[SLOT]?.id)
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertMessageSent("You do not have a high enough Herblore level to clean this herb.")
        }

    @Test
    fun GameTestState.`membership and Druidic Ritual completion gates are atomic`() =
        runHerbCleaningTest { deps ->
            val recipe = HerbCleaningRecipes.all.first()
            preparePlayer(members = false)
            player.inv[SLOT] = InvObj(recipe.grimy)
            clean(deps, SLOT)
            assertEquals(recipe.grimy.id, player.inv[SLOT]?.id)
            assertMessageSent("You need to be on a members' world to clean herbs.")

            preparePlayer(questState = 3)
            player.inv[SLOT] = InvObj(recipe.grimy)
            clean(deps, SLOT)
            assertEquals(recipe.grimy.id, player.inv[SLOT]?.id)
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertMessageSent("You need to complete Druidic Ritual before training Herblore.")
        }

    @Test
    fun GameTestState.`automatic cleaning processes one matching herb every two ticks`() =
        runHerbCleaningTest { deps ->
            val recipe = HerbCleaningRecipes.all.first()
            preparePlayer()
            repeat(3) { player.inv[it] = InvObj(recipe.grimy) }

            clean(deps, 0)
            assertEquals(1, player.count(recipe.clean))
            assertEquals(2, player.count(recipe.grimy))
            advance()
            assertEquals(1, player.count(recipe.clean))
            assertEquals(2, player.count(recipe.grimy))
            advance()
            assertEquals(2, player.count(recipe.clean))
            assertEquals(1, player.count(recipe.grimy))
            advance()
            assertEquals(2, player.count(recipe.clean))
            assertEquals(1, player.count(recipe.grimy))
            advance()
            assertEquals(3, player.count(recipe.clean))
            assertEquals(0, player.count(recipe.grimy))
            assertEquals(75, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.weakQueueList.count(herblore_queues.herbCleaning))
        }

    @Test
    fun GameTestState
        .`manual cleaning replaces a full slot and missing queued herbs stop safely`() =
        runHerbCleaningTest { deps ->
            val recipe = HerbCleaningRecipes.all.first()
            preparePlayer()
            player.inv[SLOT] = InvObj(recipe.grimy)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(objs.coins)
            }
            clean(deps, SLOT)
            assertEquals(recipe.clean.id, player.inv[SLOT]?.id)
            assertEquals(28, player.inv.count { it != null })

            preparePlayer()
            player.inv[0] = InvObj(recipe.grimy)
            player.inv[1] = InvObj(recipe.grimy)
            clean(deps, 0)
            player.inv[1] = null
            advance()
            assertEquals(1, player.weakQueueList.count(herblore_queues.herbCleaning))
            advance()
            assertEquals(1, player.count(recipe.clean))
            assertEquals(0, player.weakQueueList.count(herblore_queues.herbCleaning))
        }

    private fun GameTestScope.preparePlayer(
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

    private fun GameTestScope.clean(deps: Dependencies, slot: Int) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestState.runHerbCleaningTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val cleaning: HerbCleaning, val held: HeldInteractions, val objs: ObjTypeList)

    private data class Contract(val grimy: Int, val clean: Int, val level: Int, val fineXp: Int)

    private companion object {
        const val SLOT = 0
    }
}
