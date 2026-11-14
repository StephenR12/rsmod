package org.rsmod.content.skills.woodcutting.forestry

import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.*
import org.rsmod.api.shops.onOmniShopButton
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object Forester : NpcReferences() {
    val forms =
        listOf("forestry_forester", "forestry_forester_nostorage", "forestry_forester_storage")
            .map(::find)
}

internal object ForestShopSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ForestShopSpawns>("forester.toml")
    }
}

internal object ShopUi : InterfaceReferences() {
    val main = find("omnishop_main")
    val side = find("omnishop_side")
}

internal object ShopButtons : ComponentReferences() {
    val list = find("omnishop_main:list")
    val bag = find("omnishop_side:items")
    val buy = (1..4).map { find("omnishop_main:button_$it") }
    val sell = (1..4).map { find("omnishop_side:button_$it") }
}

internal object ShopVars : VarpReferences() {
    val last = find("omnishop_lastshop")
    val selected = find("omnishop_selected_id")
    val side = find("omnishop_selected_side_id")
    val redraw = find("omnishop_redraw")
}

private data class Cost(val id: Int, val alternate: Int?, val count: Int)

private data class Stock(
    val id: Int,
    val cost: List<Cost>,
    val amount: Int,
    val buy: Int,
    val sell: Int,
    val buyable: Boolean,
    val sellable: Boolean,
)

public class ForestryShop
@Inject
constructor(
    private val rows: DbRowTypeList,
    private val items: ObjTypeList,
    private val gear: ForestryGear,
) : PluginScript() {
    private val stock =
        listOf(3284, 3355).associateWith { id ->
            rows.getValue(id).data.getValue(5).map { value ->
                val data = rows.getValue(value as Int).data
                fun field(column: Int, default: Int) =
                    (data[column]?.firstOrNull() as? Int) ?: default
                val price =
                    data.getValue(7).chunked(2).map { (row, count) ->
                        val currencies = rows.getValue(row as Int).data.getValue(0)
                        Cost(
                            currencies.first() as Int,
                            currencies.getOrNull(1) as? Int,
                            count as Int,
                        )
                    }
                Stock(
                    field(0, -1),
                    price,
                    field(11, 1),
                    field(8, 1000),
                    field(9, (rows.getValue(id).data[7]?.firstOrNull() as? Int) ?: 1000),
                    field(17, 1) != 0,
                    field(18, 1) != 0,
                )
            }
        }
    private val quantities = intArrayOf(1, 5, 10, 50)

    override fun ScriptContext.startup() {
        for (npc in Forester.forms) {
            onOpNpc1(npc) {
                vars[ForestryBits.met] = 1
                if (choice2("See the Forestry Shop.", true, "Leave.", false)) open()
            }
            onOpNpc3(npc) { open() }
            onOpNpc4(npc) {
                if (
                    vars[ForestryBits.iron] == 2 &&
                        choice2(
                            "Store logs for the Forestry Shop permanently.",
                            true,
                            "Leave.",
                            false,
                        )
                )
                    storeLogs()
            }
        }
        for (row in stock.keys) {
            onOmniShopButton(row, ShopButtons.list) { e ->
                val index = e.comsub / 12
                if (e.comsub < 0 || index !in goods().indices) return@onOmniShopButton
                vars[ShopVars.selected] = index
                if (e.op.slot == 1) runClientScript(7146, row, -1, index, 0, 0, 0, -1, "", 1)
                if (e.op.slot in 2..5) buy(index, quantities[e.op.slot - 2])
            }
            onOmniShopButton(row, ShopButtons.bag) { e ->
                if (inv[e.comsub]?.id != e.obj?.id || inv[e.comsub] == null) return@onOmniShopButton
                vars[ShopVars.side] = e.comsub
                if (e.op.slot in 2..5) sell(e.comsub, quantities[e.op.slot - 2])
            }
            for (i in quantities.indices) {
                onOmniShopButton(row, ShopButtons.buy[i]) {
                    buy(vars[ShopVars.selected], quantities[i])
                }
                onOmniShopButton(row, ShopButtons.sell[i]) {
                    sell(vars[ShopVars.side], quantities[i])
                }
            }
        }
        onIfClose(ShopUi.main) {
            player.invMap[GearInv.kit]?.let(player::stopInvTransmit)
            player.invMap[GearInv.forester]?.let(player::stopInvTransmit)
        }
    }

    private fun ProtectedAccess.shop() = if (vars[ForestryBits.iron] == 2) 3355 else 3284

    private fun ProtectedAccess.goods() = stock.getValue(shop())

    private fun ProtectedAccess.open() {
        ifOpenMainSidePair(ShopUi.main, ShopUi.side)
        vars[ShopVars.last] = shop()
        vars[ShopVars.selected] = -1
        vars[ShopVars.side] = -1
        invTransmit(inv)
        invTransmit(gear.kit(player))
        if (shop() == 3355) invTransmit(gear.shopLogs(player))
        runClientScript(7140, shop(), -1)
        runClientScript(7246, shop())
        ifSetEvents(
            ShopButtons.list,
            0 until goods().size * 12,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
        )
        ifSetEvents(
            ShopButtons.bag,
            inv.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
        )
        for (c in ShopButtons.buy + ShopButtons.sell) ifSetEvents(c, -1..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.buy(index: Int, request: Int) {
        val item = goods().getOrNull(index) ?: return
        if (!item.buyable || !player.members && item.id !in setOf(28136, 28177)) return
        val kit = gear.kit(player)
        val stored = gear.shopLogs(player)
        repeat(if (item.id == 28136) 1 else request) {
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    for (price in item.cost) {
                        val cost = price.count * item.buy / 1000
                        val fromKit =
                            if (price.id == 28134 && gear.hasKit(player))
                                minOf(cost, kit.count(items.getValue(price.id)))
                            else 0
                        if (fromKit > 0) delete(select(kit), price.id, fromKit)
                        val fromStore =
                            if (shop() == 3355 && price.alternate != null)
                                minOf(cost - fromKit, stored.count(items.getValue(price.alternate)))
                            else 0
                        if (fromStore > 0) delete(select(stored), price.alternate!!, fromStore)
                        if (cost > fromKit + fromStore)
                            delete(bag, price.id, cost - fromKit - fromStore)
                    }
                    add(bag, item.id, item.amount)
                }
            if (!tx.success) {
                mes("You need the listed materials and room for your purchase.")
                return
            }
            vars[ShopVars.redraw] += 1
        }
    }

    private fun ProtectedAccess.sell(slot: Int, request: Int) {
        if (!player.members) return
        val obj = inv[slot] ?: return
        val item = goods().firstOrNull { it.id == obj.id } ?: return
        if (!item.sellable) return
        repeat(request) {
            val current = inv.indexOfFirst { it?.id == obj.id }
            if (current < 0) return
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, item.id, item.amount, slot = current)
                    for (price in item.cost) {
                        val refund =
                            (price.count.toLong() * item.sell.coerceAtLeast(100) / 1000)
                                .toInt()
                                .coerceAtLeast(if (price.count > 0) 1 else 0)
                        if (refund > 0) add(bag, price.id, refund)
                    }
                }
            if (!tx.success) {
                mes("Make room for the returned materials.")
                return
            }
            vars[ShopVars.redraw] += 1
        }
    }

    private fun ProtectedAccess.storeLogs() {
        val dest = gear.shopLogs(player)
        for (slot in inv.indices) {
            val item = inv[slot] ?: continue
            val log = if (items[item].isCert) items.uncert(items[item]).id else item.id
            if (log !in setOf(1521, 1519, 6333, 1517, 6332, 10810, 1515, 1513, 19669)) continue
            invMoveFromSlot(
                inv,
                dest,
                slot,
                item.count,
                uncert = items[item].isCert,
                strict = false,
            )
        }
    }
}
