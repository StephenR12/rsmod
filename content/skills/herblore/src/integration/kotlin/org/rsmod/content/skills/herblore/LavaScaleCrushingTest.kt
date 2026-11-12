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
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class LavaScaleCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins the lava-scale crushing contract`() = runLavaTest { deps ->
        assertEquals(233, objs.pestle_and_mortar.id)
        assertEquals(11992, objs.lava_scale.id)
        assertEquals(11994, objs.lava_scale_shard.id)
        assertEquals(4468, varbits.wilderness_diary_hard_complete.id)
        assertEquals(364, seqs.human_herbing_grind.id)
        assertEquals(11095, seqs.human_herbing_grind_restart.id)
        assertTrue(deps.objs[objs.lava_scale].members)
        assertTrue(deps.objs[objs.lava_scale_shard].members)
        assertTrue(deps.objs[objs.lava_scale_shard].stackable)
        assertTrue(
            deps.eventBus.contains(
                HeldUEvents.Type::class.java,
                EventBus.composeLongKey(objs.pestle_and_mortar.id, objs.lava_scale.id),
            )
        )
    }

    @Test
    fun GameTestState.`three independent level rolls produce every ordinary yield`() =
        runLavaTest { deps ->
            for (successes in 0..3) {
                preparePlayer(level = 50)
                add(objs.pestle_and_mortar)
                add(objs.lava_scale)
                repeat(successes) { random.next = 0 }
                repeat(3 - successes) { random.next = 99 }

                val slot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
                player.withProtectedAccess { deps.crushing.start(this, slot) }

                assertEquals(3 + successes, player.count(objs.lava_scale_shard))
                assertEquals(0, player.statMap.getFineXP(stats.herblore))
                assertEquals(objs.lava_scale_shard.id, player.inv[slot]?.id)
            }
        }

    @Test
    fun GameTestState
        .`Wilderness hard diary multiplies each yield by fifty percent rounded down`() =
        runLavaTest { deps ->
            val expected = listOf(4, 6, 7, 9)
            for (successes in 0..3) {
                preparePlayer(level = 50, hardDiary = true)
                add(objs.pestle_and_mortar)
                add(objs.lava_scale)
                repeat(successes) { random.next = 0 }
                repeat(3 - successes) { random.next = 99 }

                val slot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
                player.withProtectedAccess { deps.crushing.start(this, slot) }

                assertEquals(expected[successes], player.count(objs.lava_scale_shard))
            }
        }

    @Test
    fun GameTestState.`automatic crushing follows immediate then two and three tick cadence`() =
        runLavaTest { deps ->
            preparePlayer(level = 50)
            add(objs.pestle_and_mortar)
            add(objs.lava_scale, 3)
            repeat(9) { random.next = 0 }

            val slot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertEquals(6, player.count(objs.lava_scale_shard))
            advance()
            assertEquals(6, player.count(objs.lava_scale_shard))
            advance()
            assertEquals(12, player.count(objs.lava_scale_shard))
            advance(2)
            assertEquals(12, player.count(objs.lava_scale_shard))
            advance()
            assertEquals(18, player.count(objs.lava_scale_shard))
            assertEquals(0, player.weakQueueList.count(herblore_queues.lavaScaleCrushing))
        }

    @Test
    fun GameTestState.`both held-use orders work and membership is the only access gate`() =
        runLavaTest { deps ->
            repeat(2) { reverse ->
                preparePlayer(level = 1)
                player.inv[2] = InvObj(objs.pestle_and_mortar)
                player.inv[7] = InvObj(objs.lava_scale)
                repeat(3) { random.next = 99 }
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                            deps.objs[objs.lava_scale],
                            7,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.lava_scale],
                            7,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                        )
                    }
                }
                assertEquals(3, player.count(objs.lava_scale_shard), "reverse=$reverse")
            }

            preparePlayer(members = false)
            add(objs.pestle_and_mortar)
            add(objs.lava_scale)
            val slot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertEquals(1, player.count(objs.lava_scale))
            assertEquals(0, player.count(objs.lava_scale_shard))
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState.`full inventory, missing tools and replacement work remain safe`() =
        runLavaTest { deps ->
            preparePlayer(level = 50)
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.lava_scale)
            for (slot in 1 until player.inv.size) {
                if (slot != 9) {
                    player.inv[slot] = InvObj(objs.coins)
                }
            }
            repeat(3) { random.next = 99 }
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            assertEquals(3, player.count(objs.lava_scale_shard))
            assertEquals(28, player.inv.count { it != null })

            preparePlayer(level = 50)
            add(objs.pestle_and_mortar)
            add(objs.lava_scale, 2)
            repeat(3) { random.next = 99 }
            val oldSlot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
            player.withProtectedAccess { deps.crushing.start(this, oldSlot) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.pestle_and_mortar.id }] = null
            advance(2)
            assertEquals(1, player.count(objs.lava_scale))
            assertEquals(3, player.count(objs.lava_scale_shard))
            assertEquals(0, player.weakQueueList.count(herblore_queues.lavaScaleCrushing))
        }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        members: Boolean = true,
        hardDiary: Boolean = false,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, level)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarBit(varbits.wilderness_diary_hard_complete, if (hardDiary) 1 else 0)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: org.rsmod.game.type.obj.ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runLavaTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crushing: LavaScaleCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
