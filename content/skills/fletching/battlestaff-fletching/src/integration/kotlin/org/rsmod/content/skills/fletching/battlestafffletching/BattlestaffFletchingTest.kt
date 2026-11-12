package org.rsmod.content.skills.fletching.battlestafffletching

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class BattlestaffFletchingTest {
    @Test
    fun GameTestState.`cache 233 pins the celastrus battlestaff contract`() =
        runBattlestaffFletchingTest {
            assertEquals(22935, objs.celastrus_bark.id)
            assertEquals(1391, objs.battlestaff.id)
            assertEquals(31043, objs.fletching_knife.id)
            assertEquals(1248, seqs.human_fletching.id)
            assertEquals("Fletch", objTypes[objs.celastrus_bark].iop[0])
        }

    @Test
    fun GameTestState.`one bark becomes one battlestaff after four ticks for 80 xp`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.start(this, quantity = 1) }
            assertEquals(seqs.human_fletching.id, player.pendingSequence.id)
            advance(3)
            assertEquals(0, player.count(objs.battlestaff))
            advance()

            assertEquals(0, player.count(objs.celastrus_bark))
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(800, player.statMap.getFineXP(stats.fletching) - startXp)
            assertEquals(0, player.weakQueueList.count(fletching_queues.battlestaffFletching))
        }

    @Test
    fun GameTestState
        .`ordinary knife repeats every four ticks and replaces bark in a full inventory`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark, 27)

            player.withProtectedAccess { deps.fletching.start(this, quantity = 3) }
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(0, player.inv.freeSpace())
            advance(3)
            assertEquals(1, player.count(objs.battlestaff))
            advance()
            assertEquals(2, player.count(objs.battlestaff))
            advance(4)
            assertEquals(3, player.count(objs.battlestaff))
        }

    @Test
    fun GameTestState.`fletching knife reduces only repeat actions to three ticks`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.fletching_knife)
            add(objs.celastrus_bark, 3)

            player.withProtectedAccess { deps.fletching.start(this, quantity = 3) }
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))
            advance(2)
            assertEquals(1, player.count(objs.battlestaff))
            advance()
            assertEquals(2, player.count(objs.battlestaff))
            advance(3)
            assertEquals(3, player.count(objs.battlestaff))

            preparePlayer()
            add(objs.celastrus_bark, 2)
            player.worn[Wearpos.LeftHand.slot] = InvObj(objs.fletching_knife)
            player.withProtectedAccess { deps.fletching.start(this, quantity = 2) }
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))
            advance(3)
            assertEquals(2, player.count(objs.battlestaff))
        }

    @Test
    fun GameTestState.`membership level tool and source failures are atomic`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.knife)
            add(objs.celastrus_bark)
            player.withProtectedAccess { deps.fletching.start(this) }
            assertMessageSent("You need to be on a members' world to fletch a battlestaff.")
            assertEquals(1, player.count(objs.celastrus_bark))

            preparePlayer()
            player.stats[stats.fletching] = 39
            add(objs.knife)
            add(objs.celastrus_bark)
            player.withProtectedAccess { deps.fletching.start(this) }
            assertMessageSent("You need a Fletching level of 40 to make a battlestaff.")
            assertEquals(1, player.count(objs.celastrus_bark))

            preparePlayer()
            add(objs.celastrus_bark)
            player.withProtectedAccess { deps.fletching.start(this) }
            assertMessageSent("You need a knife to fletch the celastrus bark.")
            assertEquals(1, player.count(objs.celastrus_bark))

            preparePlayer()
            add(objs.knife)
            player.withProtectedAccess { deps.fletching.start(this) }
            assertMessageSent("You do not have any celastrus bark to fletch.")
            assertEquals(0, player.weakQueueList.count(fletching_queues.battlestaffFletching))
        }

    @Test
    fun GameTestState.`removing the knife or bark interrupts without an extra transform`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark, 2)
            player.withProtectedAccess { deps.fletching.start(this, quantity = 2) }
            advance(4)
            player.inv[player.inv.indexOfFirst { it?.id == objs.knife.id }] = null
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))
            assertEquals(1, player.count(objs.celastrus_bark))

            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark)
            player.withProtectedAccess { deps.fletching.start(this, quantity = 1) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.celastrus_bark.id }] = null
            advance(4)
            assertEquals(0, player.count(objs.battlestaff))
            assertEquals(0, player.weakQueueList.count(fletching_queues.battlestaffFletching))
        }

    @Test
    fun GameTestState.`native Fletch op and reverse held use both start the batch`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark)
            val barkSlot = player.inv.indexOfFirst { it?.id == objs.celastrus_bark.id }
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, barkSlot, HeldOp.Op1)
            }
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))

            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark)
            val knifeSlot = player.inv.indexOfFirst { it?.id == objs.knife.id }
            val reverseBarkSlot = player.inv.indexOfFirst { it?.id == objs.celastrus_bark.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.celastrus_bark],
                    reverseBarkSlot,
                    objTypes[objs.knife],
                    knifeSlot,
                )
            }
            advance(4)
            assertEquals(1, player.count(objs.battlestaff))
        }

    @Test
    fun GameTestState.`starting battlestaves cancels another Fletching batch`() =
        runBattlestaffFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.celastrus_bark)
            player.withProtectedAccess {
                weakQueue(fletching_queues.ammunition, 20)
                deps.fletching.start(this)
            }

            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.weakQueueList.count(fletching_queues.battlestaffFletching))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runBattlestaffFletchingTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BattlestaffFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BattlestaffFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val fletching: BattlestaffFletching,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
    )
}
