package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class CoconutMilkMakingTest {
    @Test
    fun GameTestState.`cache 233 pins coconut milk identities and held-use dispatch`() =
        runCoconutMilkTest { deps ->
            assertEquals(2347, objs.hammer.id)
            assertEquals(5974, objs.coconut.id)
            assertEquals(5976, objs.coconut_half.id)
            assertEquals(5978, objs.coconut_shell.id)
            assertEquals(229, objs.empty_vial.id)
            assertEquals(5935, objs.vial_coconut_milk.id)
            for (obj in
                listOf(
                    objs.coconut,
                    objs.coconut_half,
                    objs.coconut_shell,
                    objs.vial_coconut_milk,
                )) {
                assertTrue(deps.objs[obj].members)
                assertFalse(deps.objs[obj].stackable)
            }
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.hammer.id, objs.coconut.id),
                )
            )
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.coconut_half.id, objs.empty_vial.id),
                )
            )
        }

    @Test
    fun GameTestState.`hammer cracks the selected coconut in either held-use order`() =
        runCoconutMilkTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.hammer)
                player.inv[8] = InvObj(objs.coconut)
                for (slot in player.inv.indices) {
                    if (slot != 3 && slot != 8) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.hammer],
                            3,
                            deps.objs[objs.coconut],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.coconut],
                            8,
                            deps.objs[objs.hammer],
                            3,
                        )
                    }
                }
                assertEquals(objs.coconut_half.id, player.inv[8]?.id, "reverse=$reverse")
                assertEquals(28, player.inv.count { it != null })
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
            }
        }

    @Test
    fun GameTestState
        .`filling is immediate then automatically repeats after three and four ticks`() =
        runCoconutMilkTest { deps ->
            preparePlayer()
            add(objs.coconut_half, 3)
            add(objs.empty_vial, 3)
            val halfSlot = player.inv.indexOfFirst { it?.id == objs.coconut_half.id }
            val vialSlot = player.inv.indexOfFirst { it?.id == objs.empty_vial.id }

            player.withProtectedAccess { deps.making.fill(this, halfSlot, vialSlot) }
            assertEquals(1, player.count(objs.vial_coconut_milk))
            assertEquals(1, player.count(objs.coconut_shell))
            advance(2)
            assertEquals(1, player.count(objs.vial_coconut_milk))
            advance()
            assertEquals(2, player.count(objs.vial_coconut_milk))
            advance(3)
            assertEquals(2, player.count(objs.vial_coconut_milk))
            advance()
            assertEquals(3, player.count(objs.vial_coconut_milk))
            assertEquals(3, player.count(objs.coconut_shell))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.weakQueueList.count(herblore_queues.coconutMilk))
        }

    @Test
    fun GameTestState.`milk filling supports both orders and preserves exact output slots`() =
        runCoconutMilkTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[8] = InvObj(objs.coconut_half)
                player.inv[17] = InvObj(objs.empty_vial)
                for (slot in player.inv.indices) {
                    if (slot != 8 && slot != 17) {
                        player.inv[slot] = InvObj(objs.coins)
                    }
                }
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.coconut_half],
                            8,
                            deps.objs[objs.empty_vial],
                            17,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.empty_vial],
                            17,
                            deps.objs[objs.coconut_half],
                            8,
                        )
                    }
                }
                assertEquals(objs.coconut_shell.id, player.inv[8]?.id, "reverse=$reverse")
                assertEquals(objs.vial_coconut_milk.id, player.inv[17]?.id, "reverse=$reverse")
                assertEquals(28, player.inv.count { it != null })
            }
        }

    @Test
    fun GameTestState.`members stale slots resource loss and queue replacement are safe`() =
        runCoconutMilkTest { deps ->
            preparePlayer(members = false)
            add(objs.hammer)
            add(objs.coconut)
            val coconutSlot = player.inv.indexOfFirst { it?.id == objs.coconut.id }
            player.withProtectedAccess { deps.making.crack(this, coconutSlot) }
            assertEquals(1, player.count(objs.coconut))
            assertEquals(0, player.count(objs.coconut_half))
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer()
            add(objs.coconut_half, 2)
            add(objs.empty_vial, 2)
            val halfSlot = player.inv.indexOfFirst { it?.id == objs.coconut_half.id }
            val vialSlot = player.inv.indexOfFirst { it?.id == objs.empty_vial.id }
            player.withProtectedAccess {
                weakQueue(herblore_queues.finishedPotions, 20)
                deps.making.fill(this, halfSlot, vialSlot)
            }
            assertEquals(0, player.weakQueueList.count(herblore_queues.finishedPotions))
            player.inv[player.inv.indexOfFirst { it?.id == objs.empty_vial.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.vial_coconut_milk))
            assertEquals(0, player.weakQueueList.count(herblore_queues.coconutMilk))

            preparePlayer()
            player.inv[8] = InvObj(objs.coconut_half)
            player.inv[17] = InvObj(objs.empty_vial)
            player.inv[8] = InvObj(objs.coins)
            player.withProtectedAccess { deps.making.fill(this, 8, 17) }
            assertEquals(objs.coins.id, player.inv[8]?.id)
            assertEquals(objs.empty_vial.id, player.inv[17]?.id)
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

    private fun GameTestState.runCoconutMilkTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val making: CoconutMilkMaking,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
