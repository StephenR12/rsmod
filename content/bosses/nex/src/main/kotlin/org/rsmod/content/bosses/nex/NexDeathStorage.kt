package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Singleton
public class NexDeathStorage
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val objTypes: ObjTypeList,
    private val marketPrices: MarketPrices,
) {
    private val riskUntil = ConcurrentHashMap<String, Int>()
    private val stored = ConcurrentHashMap<String, List<StoredItem>>()

    public fun markAtRisk(player: Player) {
        riskUntil[key(player)] = player.currentMapClock + RISK_WINDOW
    }

    @OptIn(InternalApi::class)
    public fun onDeath(player: Player) {
        val key = key(player)
        val expiry = riskUntil.remove(key) ?: return
        if (player.currentMapClock > expiry) {
            return
        }
        // The completion event is published from inside the standard protected death sequence.
        // Queue a lenient continuation so item capture starts once that sequence yields.
        protectedAccess.launchLenient(player) { capture(key) }
    }

    public suspend fun reclaim(access: ProtectedAccess) {
        with(access) {
            val key = key(player)
            val items = stored[key]
            if (items.isNullOrEmpty()) {
                mes("The chest is empty. It has no items to return to you.")
                return
            }
            val wornItems = items.filter { it.wornSlot != null }
            if (wornItems.any { worn[it.wornSlot!!] != null }) {
                mes("Remove your worn equipment before reclaiming your items.")
                return
            }
            val inventoryItems = items.filter { it.wornSlot == null }.map { it.obj }
            val fit = invMoveAll(inv, inventoryItems, strict = false, autoCommit = false)
            if (!fit.success) {
                mes("You do not have enough inventory space to reclaim your items.")
                return
            }
            if (!invTakeFee(RECLAIM_FEE)) {
                mes("You need 100,000 coins in your inventory to reclaim your items.")
                return
            }
            wornItems.forEach { worn[it.wornSlot!!] = it.obj }
            if (inventoryItems.isNotEmpty()) {
                check(invMoveAll(inv, inventoryItems, strict = false).success) {
                    "Nex death storage fit check succeeded but committed reclaim failed."
                }
            }
            rebuildAppearance()
            stored.remove(key)
            mes("You pay 100,000 coins and reclaim your items from the chest.")
        }
    }

    public fun hasStoredItems(player: Player): Boolean = !stored[key(player)].isNullOrEmpty()

    public fun storedItemCount(player: Player): Int =
        stored[key(player)]?.sumOf { it.obj.count } ?: 0

    private fun ProtectedAccess.capture(key: String) {
        val previous = stored.remove(key)
        val carried =
            buildList {
                    inv.forEach { obj -> if (obj != null) add(StoredItem(obj, null)) }
                    worn.forEachIndexed { slot, obj -> if (obj != null) add(StoredItem(obj, slot)) }
                }
                .sortedByDescending(::marketPrice)
                .toMutableList()
        if (carried.isEmpty()) {
            return
        }
        val kept = mutableListOf<InvObj>()
        repeat(ITEMS_KEPT) {
            val item = carried.firstOrNull() ?: return@repeat
            kept += item.obj.copy(count = 1)
            if (item.obj.count == 1) {
                carried.removeAt(0)
            } else {
                carried[0] = item.copy(obj = item.obj.copy(count = item.obj.count - 1))
            }
        }
        invClear(inv)
        invClear(worn)
        if (kept.isNotEmpty()) {
            check(invMoveAll(inv, kept, strict = false).success) {
                "Could not return kept-on-death items after Nex death."
            }
        }
        rebuildAppearance()
        if (carried.isNotEmpty()) {
            stored[key] = carried
            mes("Your lost items have been stored in the chest outside Nex's chamber.")
        }
        if (!previous.isNullOrEmpty()) {
            mes("Your previous Nex death storage was destroyed by this unsafe death.")
        }
    }

    private fun marketPrice(item: StoredItem): Long {
        val type = objTypes[item.obj]
        return (marketPrices[type] ?: type.cost).toLong()
    }

    private fun key(player: Player): String = player.username.lowercase()

    private data class StoredItem(val obj: InvObj, val wornSlot: Int?)

    public companion object {
        public const val RECLAIM_FEE: Int = 100_000
        public const val ITEMS_KEPT: Int = 3
        public const val RISK_WINDOW: Int = 20
    }
}
