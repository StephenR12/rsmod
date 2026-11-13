package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList

@Execution(ExecutionMode.SAME_THREAD)
class TynanFishingSuppliesTest {
    @Test
    fun GameTestState.`native Tynan shop has dated stock and trade operation`() =
        runShopTest { deps ->
            assertEquals(6964, TynanNpcs.trader.id)
            assertEquals("Trade", deps.npcs[TynanNpcs.trader].op[2])
            assertTrue(deps.events.contains(NpcEvents.Op3::class.java, TynanNpcs.trader.id))
            val stock = deps.invs[TynanInvs.supplies].stock!!.filterNotNull()
            assertEquals(10, stock.size)
            assertEquals(500, stock.single { it.obj == HighLevelFishingObjs.sandworms.id }.count)
            assertEquals(
                50,
                stock.single { it.obj == HighLevelFishingObjs.sandworms.id }.restockCycles,
            )
            assertEquals(50, stock.single { it.obj == TynanObjs.sandwormPack.id }.count)
            assertEquals(5, stock.single { it.obj == TynanObjs.sandwormPack.id }.restockCycles)
        }

    @Test
    fun GameTestState.`normal packs open through native held operation and preserve other items`() =
        runShopTest { deps ->
            player.members = true
            player.inv[3] = InvObj(TynanObjs.sandwormPack)
            player.inv[4] = InvObj(TynanObjs.baitPack)
            player.inv[5] = InvObj(objs.coins, 75)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 3, HeldOp.Op1) }
            assertEquals(100, player.count(HighLevelFishingObjs.sandworms))
            assertEquals(0, player.count(TynanObjs.sandwormPack))
            player.withProtectedAccess { deps.held.interact(this, player.inv, 4, HeldOp.Op1) }
            assertEquals(100, player.count(objs.fishing_bait))
            assertEquals(0, player.count(TynanObjs.baitPack))
            assertEquals(75, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`pack opening is atomic in full inventory and members only`() =
        runShopTest { deps ->
            player.members = false
            player.inv[0] = InvObj(TynanObjs.sandwormPack)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(1, player.count(TynanObjs.sandwormPack))
            player.members = true
            for (slot in 1 until 28) player.inv[slot] = InvObj(objs.small_fishing_net)
            player.withProtectedAccess { deps.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(0, player.count(TynanObjs.sandwormPack))
            assertEquals(100, player.count(HighLevelFishingObjs.sandworms))
            assertEquals(27, player.count(objs.small_fishing_net))
        }

    private fun GameTestState.runShopTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            TynanFishingSupplies::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val held: HeldInteractions,
        val events: EventBus,
        val invs: InvTypeList,
        val npcs: NpcTypeList,
    )
}
