package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.degradable.DegradableEquipmentCatalog
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

/** Cache-233 NPC repair pricing and atomic inventory replacement used by Bob. */
class BobRepairService @Inject constructor(private val objTypes: ObjTypeList) {
    private val definitions = DegradableEquipmentCatalog.byType

    fun quoteAll(access: ProtectedAccess): BobRepairPlan =
        with(access) {
            val quotes = player.inv.indices.mapNotNull { slot -> quoteSlot(slot) }
            BobRepairPlan(quotes)
        }

    fun quoteSlot(access: ProtectedAccess, slot: Int): BobRepairPlan =
        with(access) { BobRepairPlan(listOfNotNull(quoteSlot(slot))) }

    fun repair(access: ProtectedAccess, plan: BobRepairPlan): BobRepairResult =
        with(access) {
            if (plan.quotes.isEmpty()) {
                return BobRepairResult.NothingToRepair
            }
            val current = plan.quotes.mapNotNull { quoteSlot(it.slot) }
            if (current != plan.quotes) {
                return BobRepairResult.InventoryChanged
            }
            if (player.inv.count(objTypes[objs.coins]) < plan.totalCost) {
                return BobRepairResult.InsufficientCoins(plan.totalCost)
            }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coins.id, plan.totalCost)
                    for (quote in plan.quotes) {
                        delete(inventory, quote.original.id, 1, slot = quote.slot)
                        add(inventory, quote.repaired.id, 1, slot = quote.slot)
                    }
                }
            check(transaction.success) {
                "Bob repair transaction failed after inventory and coin validation: ${transaction.err}"
            }
            BobRepairResult.Repaired(plan.quotes.size, plan.totalCost)
        }

    private fun ProtectedAccess.quoteSlot(slot: Int): BobRepairQuote? {
        val item = player.inv[slot] ?: return null
        val definition = definitions[item.id] ?: return null
        val remaining = definition.remainingPoints(item)
        val cost = (definition.maxPoints - remaining) * definition.coinsPerMissingPoint
        if (cost <= 0) {
            return null
        }
        return BobRepairQuote(
            slot = slot,
            original = item,
            repaired = definition.repaired,
            cost = cost,
        )
    }

    companion object {
        const val BARROWS_MAX_POINTS: Int = DegradableEquipmentCatalog.BARROWS_MAX_POINTS
        const val MOON_MAX_POINTS: Int = DegradableEquipmentCatalog.MOON_MAX_POINTS
        const val MOON_COINS_PER_POINT: Int = DegradableEquipmentCatalog.MOON_COINS_PER_POINT
    }
}

data class BobRepairPlan(val quotes: List<BobRepairQuote>) {
    val totalCost: Int = quotes.sumOf(BobRepairQuote::cost)
}

data class BobRepairQuote(
    val slot: Int,
    val original: InvObj,
    val repaired: ObjType,
    val cost: Int,
)

sealed class BobRepairResult {
    data class Repaired(val itemCount: Int, val cost: Int) : BobRepairResult()

    data class InsufficientCoins(val cost: Int) : BobRepairResult()

    data object NothingToRepair : BobRepairResult()

    data object InventoryChanged : BobRepairResult()
}
