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
class OilyFishingRodMakingTest {
    @Test
    fun GameTestState.`cache 233 pins both oily fishing rod contracts and dispatch`() =
        runRodTest { deps ->
            assertEquals(
                listOf(Contract(307, 1585, 0), Contract(22846, 23122, 1)),
                OilyFishingRodRecipes.all.map { Contract(it.rod.id, it.output.id, it.ticks) },
            )
            assertEquals(1582, objs.blamish_oil.id)
            for (recipe in OilyFishingRodRecipes.all) {
                assertTrue(deps.objs[recipe.output].members)
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.blamish_oil.id, recipe.rod.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`normal rod is immediate and pearl rod takes one tick`() =
        runRodTest { deps ->
            preparePlayer()
            player.inv[3] = InvObj(objs.blamish_oil)
            player.inv[8] = InvObj(objs.fishing_rod)
            player.withProtectedAccess {
                deps.making.start(this, 3, 8, OilyFishingRodRecipes.all[0])
            }
            assertEquals(null, player.inv[3])
            assertEquals(objs.oily_fishing_rod.id, player.inv[8]?.id)

            preparePlayer()
            player.inv[3] = InvObj(objs.blamish_oil)
            player.inv[8] = InvObj(objs.pearl_fishing_rod)
            player.withProtectedAccess {
                deps.making.start(this, 3, 8, OilyFishingRodRecipes.all[1])
            }
            assertEquals(objs.blamish_oil.id, player.inv[3]?.id)
            assertEquals(objs.pearl_fishing_rod.id, player.inv[8]?.id)
            advance()
            assertEquals(null, player.inv[3])
            assertEquals(objs.oily_pearl_fishing_rod.id, player.inv[8]?.id)
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`both item orders work with no Heroes Quest progress`() = runRodTest { deps ->
        for (recipe in OilyFishingRodRecipes.all) {
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.blamish_oil)
                player.inv[8] = InvObj(recipe.rod)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.blamish_oil],
                            3,
                            deps.objs[recipe.rod],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[recipe.rod],
                            8,
                            deps.objs[objs.blamish_oil],
                            3,
                        )
                    }
                }
                advance(recipe.ticks)
                assertEquals(recipe.output.id, player.inv[8]?.id)
                assertEquals(null, player.inv[3])
            }
        }
    }

    @Test
    fun GameTestState.`membership and stale delayed slots fail atomically`() = runRodTest { deps ->
        preparePlayer(members = false)
        player.inv[3] = InvObj(objs.blamish_oil)
        player.inv[8] = InvObj(objs.fishing_rod)
        player.withProtectedAccess { deps.making.start(this, 3, 8, OilyFishingRodRecipes.all[0]) }
        assertMessageSent("You need to be on a members' world to use this item.")
        assertEquals(objs.blamish_oil.id, player.inv[3]?.id)
        assertEquals(objs.fishing_rod.id, player.inv[8]?.id)

        preparePlayer()
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
        player.inv[3] = InvObj(objs.blamish_oil)
        player.inv[8] = InvObj(objs.pearl_fishing_rod)
        player.withProtectedAccess { deps.making.start(this, 3, 8, OilyFishingRodRecipes.all[1]) }
        player.inv[3] = InvObj(objs.coins)
        advance()
        assertEquals(objs.pearl_fishing_rod.id, player.inv[8]?.id)
        assertEquals(0, player.count(objs.oily_pearl_fishing_rod))
        assertEquals(0, player.weakQueueList.count(herblore_queues.oilyFishingRod))
    }

    private fun GameTestScope.preparePlayer(members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, 1)
        player.setCurrentLevel(stats.herblore, 1)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, 0)
        player.setVarp(varps.heroes_quest, 0)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestState.runRodTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val making: OilyFishingRodMaking,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )

    private data class Contract(val rod: Int, val output: Int, val ticks: Int)
}
