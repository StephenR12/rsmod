package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BlamishSnailSlimeMakingTest {
    @Test
    fun GameTestState.`cache 233 pins all blamish snail slime contracts and dispatch`() =
        runSlimeTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(3363, objs.thin_snail.id)
            assertEquals(3365, objs.lean_snail.id)
            assertEquals(3367, objs.fat_snail.id)
            assertEquals(3377, objs.sample_bottle.id)
            assertEquals(1581, objs.blamish_snail_slime.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertEquals(11095, seqs.human_herbing_grind_restart.id)
            for (snail in BlamishSnailSlimeMaking.snails) {
                assertTrue(deps.objs[snail].members)
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.pestle_and_mortar.id, snail.id),
                    )
                )
            }
            assertTrue(deps.objs[objs.sample_bottle].members)
            assertTrue(deps.objs[objs.blamish_snail_slime].members)
        }

    @Test
    fun GameTestState.`slime is immediate then repeats after one two and three ticks`() =
        runSlimeTest { deps ->
            preparePlayer()
            add(objs.pestle_and_mortar)
            add(objs.thin_snail, 4)
            add(objs.sample_bottle, 4)

            val snailSlot = player.inv.indexOfFirst { it?.id == objs.thin_snail.id }
            player.withProtectedAccess { deps.making.start(this, snailSlot, objs.thin_snail) }
            assertEquals(1, player.count(objs.blamish_snail_slime))
            assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            advance()
            assertEquals(2, player.count(objs.blamish_snail_slime))
            advance()
            assertEquals(2, player.count(objs.blamish_snail_slime))
            advance()
            assertEquals(3, player.count(objs.blamish_snail_slime))
            advance(2)
            assertEquals(3, player.count(objs.blamish_snail_slime))
            advance()
            assertEquals(4, player.count(objs.blamish_snail_slime))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.weakQueueList.count(herblore_queues.blamishSnailSlime))
        }

    @Test
    fun GameTestState.`every snail and both item orders make slime without quest progress`() =
        runSlimeTest { deps ->
            for (snail in BlamishSnailSlimeMaking.snails) {
                repeat(2) { reverse ->
                    preparePlayer()
                    player.inv[3] = InvObj(objs.pestle_and_mortar)
                    player.inv[8] = InvObj(snail)
                    player.inv[17] = InvObj(objs.sample_bottle)
                    player.withProtectedAccess {
                        if (reverse == 0) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[objs.pestle_and_mortar],
                                3,
                                deps.objs[snail],
                                8,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                deps.objs[snail],
                                8,
                                deps.objs[objs.pestle_and_mortar],
                                3,
                            )
                        }
                    }
                    assertEquals(null, player.inv[8], "snail=${snail.id}, reverse=$reverse")
                    assertEquals(
                        objs.blamish_snail_slime.id,
                        player.inv[17]?.id,
                        "snail=${snail.id}, reverse=$reverse",
                    )
                }
            }
        }

    @Test
    fun GameTestState.`membership and material loss reject or stop atomically`() =
        runSlimeTest { deps ->
            preparePlayer(members = false)
            add(objs.pestle_and_mortar)
            add(objs.lean_snail)
            add(objs.sample_bottle)
            val freeSlot = player.inv.indexOfFirst { it?.id == objs.lean_snail.id }
            player.withProtectedAccess { deps.making.start(this, freeSlot, objs.lean_snail) }
            assertMessageSent("You need to be on a members' world to use this item.")
            assertEquals(1, player.count(objs.lean_snail))
            assertEquals(1, player.count(objs.sample_bottle))
            assertEquals(0, player.count(objs.blamish_snail_slime))

            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[8] = InvObj(objs.fat_snail)
            player.inv[9] = InvObj(objs.fat_snail)
            player.inv[17] = InvObj(objs.sample_bottle)
            player.inv[18] = InvObj(objs.sample_bottle)
            player.withProtectedAccess { deps.making.start(this, 8, objs.fat_snail) }
            player.inv[0] = null
            advance()
            assertEquals(1, player.count(objs.fat_snail))
            assertEquals(1, player.count(objs.sample_bottle))
            assertEquals(1, player.count(objs.blamish_snail_slime))
            assertEquals(0, player.weakQueueList.count(herblore_queues.blamishSnailSlime))
        }

    private fun GameTestScope.preparePlayer(members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, 1)
        player.setCurrentLevel(stats.herblore, 1)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, 0)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runSlimeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val making: BlamishSnailSlimeMaking,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
