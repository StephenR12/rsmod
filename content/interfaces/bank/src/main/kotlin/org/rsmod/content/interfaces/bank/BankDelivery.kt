package org.rsmod.content.interfaces.bank

import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.content.interfaces.bank.configs.BankConstants
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType

/** Deliver to the main tab, retaining an existing stack or placeholder in its original tab. */
public fun Player.deliverToBank(
    bank: Inventory,
    item: ObjType,
    count: Int,
    payment: ObjType? = null,
    paymentCount: Int = 0,
    paymentSlot: Int? = null,
): Boolean = deliverToBank(bank, listOf(InvObj(item, count)), payment, paymentCount, paymentSlot)

/**
 * Deliver an activity catch and take its fee as one transaction, including on a nearly full bank.
 */
public fun Player.deliverToBank(
    bank: Inventory,
    items: List<InvObj>,
    payment: ObjType? = null,
    paymentCount: Int = 0,
    paymentSlot: Int? = null,
): Boolean {
    if (
        items.isEmpty() ||
            items.any { it.count <= 0 } ||
            paymentCount < 0 ||
            (paymentCount != 0 && payment == null)
    )
        return false
    val memberLimit = bankCapacity.takeIf { it > 0 } ?: BankConstants.default_capacity
    val capacity =
        when {
            members || memberLimit < BankConstants.default_capacity -> memberLimit
            else -> memberLimit - 400
        }.coerceAtMost(bank.size)
    val occupied = bank.occupiedSpace()
    if (occupied > capacity) return false
    val start = BankTab.tabs.sumOf { vars[it.sizeVarBit] }
    val result =
        invTransaction(inv, bank) {
            if (payment != null && paymentCount > 0)
                delete(select(inv), payment.id, paymentCount, slot = paymentSlot)
            val dest = select(bank)
            for (item in items) insert {
                into = dest
                obj = item.id
                strictCount = item.count
                vars = item.vars
                slot = start.coerceAtMost(bank.size - 1)
                this.capacity = capacity
            }
        }
    if (!result.success) return false
    val size = bank.indices.indexOfLast { bank[it] != null } + 1
    set(this, BankTab.Main.sizeVarBit, (size - start).coerceAtLeast(0))
    return true
}
