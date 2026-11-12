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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class SuqahToothCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins the Suqah tooth contract and dispatch`() =
        runCrushingTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(9079, objs.suqah_tooth.id)
            assertEquals(9082, objs.ground_suqah_tooth.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertEquals(11095, seqs.human_herbing_grind_restart.id)
            assertTrue(deps.objs[objs.suqah_tooth].members)
            assertTrue(deps.objs[objs.ground_suqah_tooth].members)
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.pestle_and_mortar.id, objs.suqah_tooth.id),
                )
            )
        }

    @Test
    fun GameTestState.`Suqah teeth crush after one then two then three ticks with zero xp`() =
        runCrushingTest { deps ->
            preparePlayer(level = 1, questState = 0)
            add(objs.pestle_and_mortar)
            add(objs.suqah_tooth, 3)

            val slot = player.inv.indexOfFirst { it?.id == objs.suqah_tooth.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertEquals(0, player.count(objs.ground_suqah_tooth))
            assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            advance()
            assertEquals(1, player.count(objs.ground_suqah_tooth))
            advance()
            assertEquals(1, player.count(objs.ground_suqah_tooth))
            advance()
            assertEquals(2, player.count(objs.ground_suqah_tooth))
            advance(2)
            assertEquals(2, player.count(objs.ground_suqah_tooth))
            advance()
            assertEquals(3, player.count(objs.ground_suqah_tooth))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.weakQueueList.count(herblore_queues.suqahToothCrushing))
        }

    @Test
    fun GameTestState.`both orders work and free worlds fail atomically`() =
        runCrushingTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.pestle_and_mortar)
                player.inv[8] = InvObj(objs.suqah_tooth)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                            deps.objs[objs.suqah_tooth],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.suqah_tooth],
                            8,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                        )
                    }
                }
                advance()
                assertEquals(objs.ground_suqah_tooth.id, player.inv[8]?.id, "reverse=$reverse")
            }

            preparePlayer(members = false)
            add(objs.pestle_and_mortar)
            add(objs.suqah_tooth)
            val slot = player.inv.indexOfFirst { it?.id == objs.suqah_tooth.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertMessageSent("You need to be on a members' world to use this item.")
            advance()
            assertEquals(1, player.count(objs.suqah_tooth))
            assertEquals(0, player.count(objs.ground_suqah_tooth))
        }

    @Test
    fun GameTestState.`missing pestle cancels before the delayed result`() =
        runCrushingTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.suqah_tooth)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            player.inv[0] = null
            advance()
            assertEquals(objs.suqah_tooth.id, player.inv[9]?.id)
            assertEquals(0, player.count(objs.ground_suqah_tooth))
            assertEquals(0, player.weakQueueList.count(herblore_queues.suqahToothCrushing))
        }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        members: Boolean = true,
        questState: Int = 155,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, level)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarBit(varbits.lunar_quest_main, questState)
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

    private fun GameTestState.runCrushingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crushing: SuqahToothCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
