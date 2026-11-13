package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StarTrader : NpcReferences() {
    val forms = listOf(find("star_trader"), find("star_trader_1op"), find("star_trader_2op"))
}

internal object StarMet : VarBitReferences() {
    val trader = find("star_trader_met")
}

internal object StarTraderSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<StarTraderSpawns>("dusuri.toml")
    }
}

internal object StarCurrency : CurrencyReferences() {
    val dust = find("mining_star_dust")
}

internal object StarShop : InvReferences() {
    val shop = find("dusuri_star_stock")
}

internal object StarGoods : ObjReferences() {
    val fragment = find("star_fragment")
    val goods =
        listOf(StarItems.ring, fragment, find("star_reward_gem_bag"), find("star_pack_softclay"))
    val cost = listOf(2000, 3000, 300, 150)
    val plain =
        listOf(
            ProspectorOutfits.hat,
            ProspectorOutfits.top,
            ProspectorOutfits.legs,
            ProspectorOutfits.boots,
        )
    val gold =
        listOf(
            ProspectorOutfits.goldenHat,
            ProspectorOutfits.goldenTop,
            ProspectorOutfits.goldenLegs,
            ProspectorOutfits.goldenBoots,
        )
}

internal object DusuriStock : InvBuilder() {
    init {
        build("dusuri_star_stock") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            autoSize = true
            val amounts = intArrayOf(50, 50, 100, 1000)
            for ((i, item) in StarGoods.goods.withIndex()) stock += stock(item, amounts[i], 1)
        }
    }
}

public class Dusuri
@Inject
constructor(
    private val shops: Shops,
    private val operations: ShopOperationMap,
    private val restock: ShopRestockProcess,
    private val types: ObjTypeList,
) : PluginScript(), StandardShopOperations {
    override fun ScriptContext.startup() {
        operations.register(StarCurrency.dust, this@Dusuri)
        for (npc in StarTrader.forms) {
            onOpNpc1(npc) {
                if (!player.members) {
                    mes("Dusuri trades his rewards on members' worlds.")
                    return@onOpNpc1
                }
                vars[StarMet.trader] = 1
                if (choice2("See the stardust rewards.", true, "Leave.", false)) trade()
            }
            onOpNpc3(npc) { if (player.members && vars[StarMet.trader] != 0) trade() }
        }
        for ((i, piece) in StarGoods.plain.withIndex()) onOpHeldU(piece, StarGoods.fragment) {
            if (!player.members) return@onOpHeldU
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, piece.id, 1, it.firstSlot)
                delete(bag, StarGoods.fragment.id, 1, it.secondSlot)
                add(bag, StarGoods.gold[i].id, 1, slot = it.firstSlot)
            }
        }
    }

    private fun ProtectedAccess.trade() {
        shops.open(player, "Dusuri's Star Shop", StarShop.shop, 80.0, 100.0, 0.0, StarCurrency.dust)
    }

    private fun price(id: Int): Int? =
        StarGoods.goods.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { StarGoods.cost[it] }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        inv[slot]?.let { player.mes(types[it].desc) }
    }

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val item = shop.inv[slot] ?: return
        player.mes("${types[item].name} costs ${price(item.id)} stardust.")
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        val item = sideInv[slot] ?: return
        val cost = price(item.id)
        player.mes(
            if (cost == null) "Dusuri won't buy that."
            else "Dusuri will give you ${cost*4/5} stardust for ${types[item].name}."
        )
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        if (!player.members) return
        val item = shop.inv[slot] ?: return
        val cost = price(item.id) ?: return
        val good = types[item]
        val cash = sideInv.count(types[StarItems.dust])
        val affordable = minOf(request, item.count, cash / cost)
        val free = sideInv.freeSpace()
        val spendsAll = affordable * cost == cash
        val count =
            when {
                good.isStackable && good in sideInv ->
                    minOf(affordable, Int.MAX_VALUE - sideInv.count(good))
                good.isStackable -> if (free > 0 || spendsAll) affordable else 0
                spendsAll && affordable <= free + 1 -> affordable
                else -> minOf(affordable, free)
            }
        if (count <= 0) {
            player.mes(
                if (cash < cost) "You need more stardust."
                else if (item.count == 0) "Dusuri is out of stock."
                else "You need more inventory space."
            )
            return
        }
        val txn =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val stock = select(shop.inv)
                delete(bag, StarItems.dust.id, count * cost)
                delete(stock, item.id, count, slot = slot)
                add(bag, item.id, count)
            }
        if (txn.success) {
            if (shop.inv[slot] == null) shop.inv[slot] = InvObj(good, 0)
            restock += shop.inv
        }
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        if (!player.members) return
        val item = sideInv[slot] ?: return
        val cost =
            price(item.id)
                ?: run {
                    player.mes("Dusuri won't buy that.")
                    return
                }
        val good = types[item]
        val unitPrice = cost * 4 / 5
        val count =
            minOf(
                request,
                sideInv.count(good),
                Int.MAX_VALUE - shop.inv.count(good),
                (Int.MAX_VALUE - sideInv.count(types[StarItems.dust])) / unitPrice,
            )
        if (count <= 0) return
        val txn =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val stock = select(shop.inv)
                delete(bag, item.id, count, slot = slot)
                add(bag, StarItems.dust.id, count * unitPrice)
                add(stock, item.id, count)
            }
        if (txn.success) restock += shop.inv
    }
}
