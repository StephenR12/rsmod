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
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class CrystalShardCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins the crystal shard crushing contract`() =
        runCrystalTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(23962, objs.crystal_shard.id)
            assertEquals(23964, objs.crystal_dust.id)
            assertEquals(9016, varbits.song_of_the_elves.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertTrue(deps.objs[objs.crystal_shard].members)
            assertTrue(deps.objs[objs.crystal_shard].stackable)
            assertTrue(deps.objs[objs.crystal_dust].members)
            assertTrue(deps.objs[objs.crystal_dust].stackable)
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.pestle_and_mortar.id, objs.crystal_shard.id),
                )
            )
        }

    @Test
    fun GameTestState.`selected quantity is crushed all at once at ten dust per shard`() =
        runCrystalTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[5] = InvObj(objs.crystal_shard, 7)

            player.withProtectedAccess { deps.crushing.start(this, 5) }
            assertTrue(player.isAccessProtected)
            player.resumeActiveCoroutine(ResumePCountDialogInput(3))

            assertEquals(4, player.count(objs.crystal_shard))
            assertEquals(30, player.count(objs.crystal_dust))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`both held-use orders dispatch and cap the entered amount`() =
        runCrystalTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[2] = InvObj(objs.pestle_and_mortar)
                player.inv[7] = InvObj(objs.crystal_shard, 2)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                            deps.objs[objs.crystal_shard],
                            7,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.crystal_shard],
                            7,
                            deps.objs[objs.pestle_and_mortar],
                            2,
                        )
                    }
                }
                player.resumeActiveCoroutine(ResumePCountDialogInput(100))

                assertEquals(0, player.count(objs.crystal_shard), "reverse=$reverse")
                assertEquals(20, player.count(objs.crystal_dust), "reverse=$reverse")
                assertEquals(objs.crystal_dust.id, player.inv[7]?.id, "reverse=$reverse")
            }
        }

    @Test
    fun GameTestState.`membership and Song of the Elves completion are atomic gates`() =
        runCrystalTest { deps ->
            preparePlayer(members = false)
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[5] = InvObj(objs.crystal_shard)
            player.withProtectedAccess { deps.crushing.start(this, 5) }
            assertEquals(1, player.count(objs.crystal_shard))
            assertMessageSent("You need to be on a members' world to use this item.")

            preparePlayer(questState = CrystalShardCrushing.SONG_OF_THE_ELVES_COMPLETE - 2)
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[5] = InvObj(objs.crystal_shard)
            player.withProtectedAccess { deps.crushing.start(this, 5) }
            assertEquals(1, player.count(objs.crystal_shard))
            assertMessageSent("You need to have completed Song of the Elves to do this.")
        }

    @Test
    fun GameTestState.`full inventory and suspended-dialogue changes remain atomic`() =
        runCrystalTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[5] = InvObj(objs.crystal_shard, 2)
            for (slot in 1 until player.inv.size) {
                if (slot != 5) {
                    player.inv[slot] = InvObj(objs.coins)
                }
            }
            player.withProtectedAccess { deps.crushing.start(this, 5) }
            player.resumeActiveCoroutine(ResumePCountDialogInput(1))
            assertEquals(2, player.count(objs.crystal_shard))
            assertEquals(0, player.count(objs.crystal_dust))

            player.withProtectedAccess { deps.crushing.start(this, 5) }
            player.resumeActiveCoroutine(ResumePCountDialogInput(2))
            assertEquals(0, player.count(objs.crystal_shard))
            assertEquals(20, player.count(objs.crystal_dust))
            assertEquals(28, player.inv.count { it != null })

            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[5] = InvObj(objs.crystal_shard, 2)
            player.withProtectedAccess { deps.crushing.start(this, 5) }
            player.inv[5] = InvObj(objs.crystal_shard)
            player.resumeActiveCoroutine(ResumePCountDialogInput(1))
            assertEquals(1, player.count(objs.crystal_shard))
            assertEquals(0, player.count(objs.crystal_dust))
        }

    private fun GameTestScope.preparePlayer(
        members: Boolean = true,
        questState: Int = CrystalShardCrushing.SONG_OF_THE_ELVES_COMPLETE,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setVarBit(varbits.song_of_the_elves, questState)
        player.statMap.setXP(stats.herblore, 0)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestState.runCrystalTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crushing: CrystalShardCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
