package org.rsmod.content.skills.agility

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
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SepulchreActors : NpcReferences() {
    val stranger = find("hallowed_lobby_npc")
}

internal object SepulchreSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SepulchreSpawns>("sepulchre.toml")
    }
}

internal object SepulchreCurrency : CurrencyReferences() {
    val marks = find("hallowed_marks")
}

internal object SepulchreInvs : InvReferences() {
    val goods = find("hallowed_rewardshop")
}

internal object SepulchreStock : ObjReferences() {
    val prices =
        listOf(
                "hallowed_teleport" to 1,
                "hallowed_token" to 10,
                "hallowed_grapple" to 100,
                "hallowed_focus" to 100,
                "hallowed_symbol" to 100,
                "hallowed_hammer" to 100,
                "hallowed_ring" to 250,
                "dark_dye" to 300,
                "dark_acorn" to 3000,
                "hallowed_sack" to 100,
            )
            .map { find(it.first) to it.second }
}

internal object SepulchreShopBuilder : InvBuilder() {
    init {
        build("hallowed_rewardshop") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            autoSize = true
            for ((index, entry) in SepulchreStock.prices.withIndex()) stock +=
                stock(entry.first, count = if (index == 0) 1000 else 100, restockCycles = 1)
        }
    }
}

public class SepulchreStranger
@Inject
constructor(
    private val storage: HallowedToolStorage,
    private val shops: Shops,
    private val ops: ShopOperationMap,
    private val restock: ShopRestockProcess,
    private val types: ObjTypeList,
) : PluginScript(), StandardShopOperations {
    override fun ScriptContext.startup() {
        ops.register(SepulchreCurrency.marks, this@SepulchreStranger)
        onOpNpc1(SepulchreActors.stranger) {
            if (!player.members || vars[SepulchreBits.quest] < 138) return@onOpNpc1
            if (vars[SepulchreBits.intro] == 0 || vars[SepulchreBits.freeShards] == 0) {
                mes(
                    "Reach the end of each floor before the time runs out. The magical obelisks will bring you back here."
                )
                vars[SepulchreBits.intro] = 1
                if (
                    vars[SepulchreBits.freeShards] == 0 &&
                        invAdd(inv, SepulchreItems.shard, 5).success
                ) {
                    vars[SepulchreBits.freeShards] = 1
                    mes(
                        "The stranger gives you five hallowed crystal shards to return to the Sepulchre."
                    )
                }
            } else
                when (
                    menu(
                        "Mysterious Stranger",
                        true,
                        listOf(
                            "Trade hallowed marks.",
                            "Ask about private access.",
                            "Access tool storage.",
                            "Tell me about the Sepulchre.",
                        ),
                    )
                ) {
                    0 -> open()
                    1 -> privateAccess()
                    2 -> storage.open(this)
                    3 ->
                        mes(
                            "A deeper floor opens every ten Agility levels. Coffins hold hallowed marks if you can reach them in time."
                        )
                }
        }
        onOpNpc3(SepulchreActors.stranger) { if (vars[SepulchreBits.intro] != 0) open() }
    }

    private suspend fun ProtectedAccess.privateAccess() {
        if (vars[SepulchreBits.ownPrivate] == 0) {
            if (
                !choice2(
                    "Buy private access for 200 hallowed marks.",
                    true,
                    "No, thank you.",
                    false,
                )
            )
                return
            if (invDel(inv, SepulchreItems.marks, 200).failure) {
                mes("You need 200 hallowed marks.")
                return
            }
            vars[SepulchreBits.ownPrivate] = 1
            vars[SepulchreBits.usePrivate] = 1
        } else vars[SepulchreBits.usePrivate] = if (vars[SepulchreBits.usePrivate] == 0) 1 else 0
        mes(
            if (vars[SepulchreBits.usePrivate] != 0) "Your next run will use private chambers."
            else "Your next run will use the public chambers."
        )
    }

    private fun ProtectedAccess.open() {
        if (player.members)
            shops.open(
                player,
                "Mysterious Hallowed Goods",
                SepulchreInvs.goods,
                0.0,
                1.0,
                0.0,
                SepulchreCurrency.marks,
            )
    }

    private fun price(id: Int) = SepulchreStock.prices.firstOrNull { it.first.id == id }?.second

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val item = shop.inv[slot] ?: return
        player.mes("${types[item].name} costs ${price(item.id)} hallowed marks.")
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        player.mes("The stranger does not buy items.")
    }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        inv[slot]?.let { player.mes(types[it].desc) }
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        player.mes("The stranger does not buy items.")
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val item = shop.inv[slot] ?: return
        val cost = price(item.id) ?: return
        val funds = sideInv.count(types[SepulchreItems.marks])
        val type = types[item]
        val available = minOf(request, item.count, funds / cost)
        if (available <= 0) {
            player.mes(
                if (item.count == 0) "That item is out of stock."
                else "You need more hallowed marks."
            )
            return
        }
        val space = sideInv.freeSpace() + (if (available * cost == funds) 1 else 0)
        val amount =
            if (type.isStackable && (space > 0 || sideInv.count(type) > 0)) available
            else minOf(space, available)
        if (amount <= 0) {
            player.mes("You need more inventory space.")
            return
        }
        val result =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val stock = select(shop.inv)
                delete(bag, SepulchreItems.marks.id, amount * cost)
                delete(stock, item.id, amount)
                add(bag, item.id, amount)
            }
        if (result.success) {
            if (shop.inv[slot] == null) shop.inv[slot] = InvObj(type, 0)
            restock += shop.inv
        }
    }
}
