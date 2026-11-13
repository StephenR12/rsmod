package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Product numbers follow native enum 4379. Each client-rendered row occupies thirteen children. */
class FoundryShop @Inject constructor() : PluginScript() {
    private val prices = listOf(2000, 300, 15, 200, 4000, 4000, 3500, 3500, 5000)
    private val moulds = FoundryMoulds.all.filter { it.cost > 0 }.sortedBy { it.struct }

    override fun ScriptContext.startup() {
        onOpHeldU(FoundryObjs.rewards[7], objs.ice_gloves) {
            if (
                !inv[it.firstSlot].isType(FoundryObjs.rewards[7]) ||
                    !inv[it.secondSlot].isType(objs.ice_gloves)
            )
                return@onOpHeldU
            inv[it.firstSlot] = InvObj(FoundryObjs.glovesIce)
            inv[it.secondSlot] = null
            mes("You combine the ice gloves with your smiths gloves.")
        }
        onIfModalButton(FoundryButtons.filter) {
            if (it.comsub in 1..11 && it.comsub % 2 == 1) {
                vars[FoundryBits.filter] = it.comsub / 2
                vars[FoundryVars.selected] = 0
            }
        }
        onOpHeld1(FoundryObjs.rewards[1]) {
            if (!inv[it.slot].isType(FoundryObjs.rewards[1])) return@onOpHeld1
            inv[it.slot] = InvObj(FoundryObjs.glass)
            statHeal(stats.hitpoints, 1, 0)
            statBoost(stats.smithing, 4, 0)
            for (stat in listOf(stats.attack, stats.ranged, stats.magic)) statSub(stat, 2, 0)
            mes("You drink Kovac's grog. It tastes like giants' feet.")
        }
        onOpHeld1(FoundryObjs.rewards[3]) { openOrePack(this, it.slot) }
        for (npc in listOf(FoundryNpcs.kovac, FoundryNpcs.multi)) {
            for ((index, item) in FoundryObjs.rewards.withIndex()) {
                if (index == 1 || index == 8) continue
                onOpNpcU(npc, item) {
                    val refund = prices[index] * 4 / 5
                    if (
                        choice2(
                            "Return it for $refund Foundry Reputation.",
                            true,
                            "Keep it.",
                            false,
                        )
                    ) {
                        if (invDel(inv, item, 1, it.invSlot).success)
                            vars[FoundryVars.points] += refund
                    }
                }
            }
        }
        onIfModalButton(FoundryButtons.shopList) { e ->
            if (e.comsub < 0) return@onIfModalButton
            val product = e.comsub / 13 + 1
            if (product !in 1..24) return@onIfModalButton
            vars[FoundryVars.selected] = product
            when (e.op) {
                IfButtonOp.Op2 -> purchase(this, product, 1)
                IfButtonOp.Op3 -> purchase(this, product, 5)
                IfButtonOp.Op4 -> purchase(this, product, 10)
                else -> Unit
            }
        }
        onIfModalButton(FoundryButtons.buy) { purchase(this, vars[FoundryVars.selected], 1) }
    }

    private fun openOrePack(access: ProtectedAccess, slot: Int): Unit =
        with(access) {
            val pack = FoundryObjs.rewards[3]
            if (!inv[slot].isType(pack)) return
            val counts = IntArray(5)
            // Documented player sample of 40,770 ore rolls. Mining level does not affect the table.
            repeat(30) {
                val roll = random.of(40770)
                val ore =
                    when {
                        roll < 33225 -> 0
                        roll < 37455 -> 1
                        roll < 40074 -> 2
                        roll < 40654 -> 3
                        else -> 4
                    }
                counts[ore]++
            }
            val tx =
                player.invTransaction(inv) {
                    val inventory = select(inv)
                    delete(inventory, pack.id, 1, slot)
                    for ((i, count) in counts.withIndex()) if (count > 0)
                        add(inventory, FoundryObjs.ores[i].id, count)
                }
            if (!tx.success) mes("Make room for all the ores before opening the pack.")
        }

    fun open(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members || vars[FoundryBits.quest] < 30) return
            vars[FoundryVars.selected] = 0
            vars[FoundryBits.filter] = 0
            ifOpenMainModal(FoundryInterfaces.shop)
            ifSetEvents(
                FoundryButtons.shopList,
                0 until 24 * 13,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
            ifSetEvents(FoundryButtons.filter, 1..11, IfEvent.Op1)
            ifSetEvents(FoundryButtons.buy, -1..-1, IfEvent.Op1)
        }

    fun purchase(access: ProtectedAccess, product: Int, quantity: Int): Boolean =
        with(access) {
            if (
                !player.members ||
                    vars[FoundryBits.quest] < 30 ||
                    product !in 1..24 ||
                    quantity !in listOf(1, 5, 10)
            )
                return false
            val mould = moulds.getOrNull(product - 10)
            if (mould != null && (quantity != 1 || mould.unlocked(player))) return false
            val cost = (mould?.cost ?: prices[product - 1]) * quantity
            if (vars[FoundryVars.points] < cost) {
                mes("You don't have enough Foundry Reputation.")
                return false
            }
            if (mould != null) vars[FoundryBits.unlocked[mould.part][5 - mould.index]] = 1
            else if (invAdd(inv, FoundryObjs.rewards[product - 1], quantity).failure) {
                mes("Make room in your inventory for the reward.")
                return false
            }
            vars[FoundryVars.points] -= cost
            mes("You spend $cost Foundry Reputation.")
            true
        }
}
