package org.rsmod.content.skills.crafting.hunterpouches

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HunterPouchDeliveryTest {
    @Test
    fun GameTestState.catchDeliveryUsesOpenPouchesAndHighestCapacityWithoutRequiringLootSlots() =
        game { d ->
            prepare()
            full()
            player.inv[0] = InvObj(objs.small_fur_pouch_open)
            player.inv[1] = InvObj(objs.large_fur_pouch_open)
            player.inv[2] = InvObj(objs.small_meat_pouch_open)
            player.withProtectedAccess {
                assertTrue(
                    d.storage.deliverCaught(
                        player,
                        listOf(InvObj(objs.fox_fur, 25), InvObj(objs.raw_pyre_fox, 14)),
                    )
                )
            }
            assertEquals(25, d.storage.count(player, HunterPouchFamily.Fur))
            assertEquals(14, d.storage.count(player, HunterPouchFamily.Meat))
            assertEquals(0, player.count(objs.fox_fur))
            assertEquals(0, player.count(objs.raw_pyre_fox))
            assertEquals(0, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.failedOrdinaryLootDeliveryRollsBackBothPouchFamilies() = game { d ->
        prepare()
        full()
        player.inv[0] = InvObj(objs.small_fur_pouch_open)
        player.inv[1] = InvObj(objs.small_meat_pouch_open)
        val loot = listOf(InvObj(objs.fox_fur), InvObj(objs.raw_pyre_fox), InvObj(objs.bones))
        player.withProtectedAccess { assertFalse(d.storage.deliverCaught(player, loot)) }
        assertEquals(0, d.storage.count(player, HunterPouchFamily.Fur))
        assertEquals(0, d.storage.count(player, HunterPouchFamily.Meat))
        player.inv[27] = null
        player.withProtectedAccess { assertTrue(d.storage.deliverCaught(player, loot)) }
        assertEquals(1, d.storage.count(player, HunterPouchFamily.Fur))
        assertEquals(1, d.storage.count(player, HunterPouchFamily.Meat))
        assertEquals(1, player.count(objs.bones))
    }

    @Test
    fun GameTestState.sameFamilyCapacityIsReservedAcrossItemsAndOverflowIsAtomic() = game { d ->
        prepare()
        full()
        player.inv[0] = InvObj(objs.small_meat_pouch_open)
        assertEquals(13, d.storage.storeAcquired(player, objs.raw_wild_kebbit, 13))
        val loot = listOf(InvObj(objs.raw_wild_kebbit), InvObj(objs.raw_pyre_fox))
        player.withProtectedAccess { assertFalse(d.storage.deliverCaught(player, loot)) }
        assertEquals(13, d.storage.count(player, HunterPouchFamily.Meat))
        player.inv[27] = null
        player.withProtectedAccess { assertTrue(d.storage.deliverCaught(player, loot)) }
        assertEquals(14, d.storage.count(player, HunterPouchFamily.Meat))
        assertEquals(1, player.count(objs.raw_pyre_fox))
    }

    @Test
    fun GameTestState.closedPouchesAndRawBirdMeatUseTheOrdinaryInventory() = game { d ->
        prepare()
        player.inv[0] = InvObj(objs.small_fur_pouch)
        player.inv[1] = InvObj(objs.small_meat_pouch_open)
        player.withProtectedAccess {
            assertTrue(
                d.storage.deliverCaught(
                    player,
                    listOf(InvObj(objs.fox_fur), InvObj(objs.raw_bird_meat)),
                )
            )
        }
        assertEquals(1, player.count(objs.fox_fur))
        assertEquals(1, player.count(objs.raw_bird_meat))
        assertEquals(0, d.storage.count(player, HunterPouchFamily.Fur))
        assertEquals(0, d.storage.count(player, HunterPouchFamily.Meat))
    }

    @Test
    fun GameTestState.nativeMeatEmptyWorksNearABankWithoutAnOpenBankInterface() = game { d ->
        prepare()
        player.inv[0] = InvObj(objs.small_meat_pouch_open)
        d.storage.storeAcquired(player, objs.raw_pyre_fox, 3)
        placeMapLoc(CoordGrid(3202, 3200), d.locs.getValue(26707))
        player.withProtectedAccess { d.held.interact(this, inv, 0, HeldOp.Op2) }
        assertEquals(3, player.count(objs.raw_pyre_fox))
        assertEquals(0, d.storage.count(player, HunterPouchFamily.Meat))
    }

    @Test
    fun GameTestState.meatEmptyAtGuildTopFloorIsPartialAndPreservesTheRemainder() = game { d ->
        prepare()
        full()
        player.inv[0] = InvObj(objs.small_meat_pouch_open)
        d.storage.storeAcquired(player, objs.raw_pyre_fox, 3)
        player.telejump(CoordGrid(1556, 3046, 2))
        player.inv[27] = null
        val descriptor = d.registry.find(objs.small_meat_pouch_open.id)!!
        player.withProtectedAccess { assertEquals(1, d.storage.emptyToInventory(this, descriptor)) }
        assertEquals(1, player.count(objs.raw_pyre_fox))
        assertEquals(2, d.storage.count(player, HunterPouchFamily.Meat))
    }

    @Test
    fun GameTestState.meatEmptyRejectsRemoteLocationsAndMorUlRekEvenNearItsBank() = game { d ->
        prepare()
        player.inv[0] = InvObj(objs.small_meat_pouch_open)
        d.storage.storeAcquired(player, objs.raw_pyre_fox, 2)
        val descriptor = d.registry.find(objs.small_meat_pouch_open.id)!!
        player.withProtectedAccess { assertEquals(0, d.storage.emptyToInventory(this, descriptor)) }
        val c = CoordGrid(2444, 5170)
        allocZoneCollision(c)
        player.telejump(c)
        placeMapLoc(c.translate(2, 0), d.locs.getValue(26707))
        player.withProtectedAccess { assertEquals(0, d.storage.emptyToInventory(this, descriptor)) }
        assertEquals(2, d.storage.count(player, HunterPouchFamily.Meat))
        assertEquals(0, player.count(objs.raw_pyre_fox))
    }

    @Test
    fun GameTestState.furEmptyKeepsItsBankOnlyRuleAtTheGuildAndNearBanks() = game { d ->
        prepare()
        player.inv[0] = InvObj(objs.small_fur_pouch_open)
        d.storage.storeAcquired(player, objs.fox_fur, 2)
        player.telejump(CoordGrid(1556, 3046, 2))
        val descriptor = d.registry.find(objs.small_fur_pouch_open.id)!!
        player.withProtectedAccess { assertEquals(0, d.storage.emptyToInventory(this, descriptor)) }
        assertEquals(2, d.storage.count(player, HunterPouchFamily.Fur))
    }

    private fun GameTestScope.full() {
        for (i in 0..27) player.inv[i] = InvObj(objs.knife)
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.telejump(CoordGrid(3200, 3200))
        for (dx in -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(
            player.coords.translate(dx, dz)
        )
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, Module, HunterPouchCraftingScript::class, testBody = body)

    private object Module : AbstractModule() {
        override fun configure() {
            install(HunterPouchModule())
        }
    }

    data class Deps
    @Inject
    constructor(
        val storage: HunterPouchStorage,
        val registry: HunterPouchStorageRegistry,
        val held: HeldInteractions,
        val locs: LocTypeList,
    )
}
