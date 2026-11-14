package org.rsmod.content.generic.npcs.services

import jakarta.inject.Inject
import org.rsmod.api.config.refs.currencies
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onMissingNpcService
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.StandardGpShopOperations
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class Stock(val item: String, val quantity: Int, val restock: Int)

private data class WorldShop(
    val inventory: String,
    val title: String,
    val members: Boolean,
    val general: Boolean,
    val buy: Double,
    val sell: Double,
    val change: Double,
    val owners: Map<String, Int>,
    val goods: List<Stock>,
    val requirement: String,
    val minimum: Int,
)

private data class KaramjaPrices(
    val gloveTier: Int,
    val buy: Double,
    val sell: Double,
    val currency: String,
)

private data class ItemPrice(val item: String, val sell: Int?, val buy: Int?)

private val itemPrices =
    requireNotNull(WorldShop::class.java.getResourceAsStream("shop-prices.tsv"))
        .bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val fields = line.split('\t')
                val sell = fields[2].takeIf { it.isNotEmpty() }?.toInt()
                val buy = fields[3].takeIf { it.isNotEmpty() }?.let { if (it == "-") -1 else it.toInt() }
                require(sell == null || sell > 0) { "Invalid shop sale price: $line" }
                require(buy == null || buy >= -1) { "Invalid shop purchase price: $line" }
                fields[0] to ItemPrice(fields[1], sell, buy)
            }.toList().groupBy({ it.first }, { it.second })
        }

private val karamjaPrices =
    requireNotNull(WorldShop::class.java.getResourceAsStream("karamja-prices.tsv"))
        .bufferedReader()
        .useLines { lines ->
            lines.filterNot { it.startsWith('#') }.associate { line ->
                val (inventory, tier, buy, sell, currency) = line.split('\t')
                inventory to KaramjaPrices(tier.toInt(), buy.toDouble() / 10, sell.toDouble() / 10, currency)
            }
        }

private val gloves =
    listOf(
        objs.diary_karamja_gloves_1,
        objs.diary_karamja_gloves_2,
        objs.diary_karamja_gloves_3,
        objs.diary_karamja_gloves_4,
    )

private val catalog = listOf("shops.tsv", "quest-shops.tsv").flatMap { resource ->
    requireNotNull(WorldShop::class.java.getResourceAsStream(resource))
        .bufferedReader()
        .useLines { lines ->
            lines
                .filter { it.isNotBlank() && !it.startsWith('#') }
                .map { line ->
                    val fields = line.split('\t')
                    val owners =
                        fields[7].split(',').associate {
                            val (name, slot) = it.split(':')
                            name to slot.toInt()
                        }
                    val goods =
                        fields[8].split(',').map {
                            val (name, count, restock) = it.split(':')
                            Stock(name, count.toInt(), restock.toInt())
                        }
                    WorldShop(
                        fields[0],
                        fields[1],
                        fields[2].toBooleanStrict(),
                        fields[3].toBooleanStrict(),
                        fields[4].toDouble() / 10,
                        fields[5].toDouble() / 10,
                        fields[6].toDouble() / 10,
                        owners,
                        goods,
                        fields.getOrNull(10).orEmpty(),
                        fields.getOrNull(11)?.toInt() ?: 0,
                    )
                }
                .toList()
        }

}

internal object WorldShopQuestBits : VarBitReferences() {
    val byName = catalog.map { it.requirement }.filter { it.startsWith("b:") }.distinct().associateWith { find(it.substring(2)) }
}

internal object WorldShopQuestVarps : VarpReferences() {
    val byName = catalog.map { it.requirement }.filter { it.startsWith("p:") }.distinct().associateWith { find(it.substring(2)) }
}

internal object TomatoCrates : LocReferences() {
    val crates = listOf(find("rotten_tomato_crate"), find("pillory_rotten_tomato_crate"))
}

internal object WorldShopCurrency : CurrencyReferences() {
    val tokkul = find("world_shop_tokkul")
    val sticks = find("world_shop_trade_sticks")
    val tears = find("world_shop_mermaid_tears")
}

internal object WorldShopInvs : InvReferences() {
    val byName = catalog.associate { it.inventory to find(it.inventory) }
}

internal object WorldShopPeople : NpcReferences() {
    val byName = catalog.flatMap { it.owners.keys }.distinct().associateWith { find(it) }
}

internal object WorldShopGoods : ObjReferences() {
    val sticks = find("village_trade_sticks")
    val tears = find("fossil_mermaid_tear")
    val byName =
        (catalog.flatMap { shop -> shop.goods.map { it.item } } + itemPrices.values.flatten().map { it.item })
            .distinct().associateWith { find(it) }
}

internal object WorldShopInventories : InvBuilder() {
    init {
        for (shop in catalog) build(shop.inventory) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            allStock = shop.general
            restock = true
            size = if (shop.general) maxOf(40, shop.goods.size) else shop.goods.size
            for (item in shop.goods) {
                stock +=
                    stock(WorldShopGoods.byName.getValue(item.item), item.quantity, item.restock)
            }
        }
    }
}

internal object ServiceSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ServiceSpawns>("services.toml")
    }
}

public class WorldShops
@Inject
constructor(
    private val shops: Shops,
    private val types: NpcTypeList,
    private val operations: ShopOperationMap,
    private val gp: StandardGpShopOperations,
) : PluginScript() {
    override fun ScriptContext.startup() {
        operations.register(
            WorldShopCurrency.tokkul,
            gp.withPayment(objs.tokkul, "Tokkul", "Tokkul"),
        )
        operations.register(
            WorldShopCurrency.sticks,
            gp.withPayment(WorldShopGoods.sticks, "trading stick", "trading sticks"),
        )
        operations.register(WorldShopCurrency.tears, gp.withPayment(WorldShopGoods.tears, "mermaid's tear", "mermaid's tears"))
        val tomatoes = catalog.single { it.title == "Shop of Distaste" }
        for (crate in TomatoCrates.crates) onOpLoc1(crate) { open(tomatoes, "Pelters' Veg Stall") }

        val offers = catalog.flatMap { shop -> shop.owners.map { (name, slot) -> (name to slot) to shop } }
            .groupBy({ it.first }, { it.second })
        onGameStartup {
            for ((owner, variants) in offers) {
                val (name, slot) = owner
                val npc = types[WorldShopPeople.byName.getValue(name)]
                onMissingNpcService(npc, slot) {
                    val shop = variants.lastOrNull { eligible(it) }
                    if (shop != null) open(shop)
                    else if (name.startsWith("tbwcu_gabooty")) mes("Ask Gabooty what he does here first.")
                    else mes("The shopkeeper cannot trade with you yet.")
                }
            }
        }
    }

    private fun ProtectedAccess.eligible(shop: WorldShop): Boolean = when {
        shop.requirement.isEmpty() -> true
        shop.requirement.startsWith("b:") -> vars[WorldShopQuestBits.byName.getValue(shop.requirement)] >= shop.minimum
        else -> vars[WorldShopQuestVarps.byName.getValue(shop.requirement)] >= shop.minimum
    }

    private fun ProtectedAccess.open(shop: WorldShop, title: String = shop.title) {
        if (shop.members && !player.members) {
            mes("You need to be on a members' world to use this shop.")
            return
        }
        val prices = karamjaPrices[shop.inventory]
        val discount = prices?.takeIf { price -> gloves.drop(price.gloveTier - 1).any { it in worn } }
        val currency = when (prices?.currency) {
            "world_shop_tokkul" -> WorldShopCurrency.tokkul
            "world_shop_trade_sticks" -> WorldShopCurrency.sticks
            else -> if (shop.inventory == "world_shop_mairin") WorldShopCurrency.tears else currencies.standard_gp
        }
        val explicit = itemPrices[shop.inventory].orEmpty()
        val buyPrices = if (currency == WorldShopCurrency.tears) shop.goods.associate { WorldShopGoods.byName.getValue(it.item).id to -1 } else explicit.mapNotNull { price -> price.buy?.let { WorldShopGoods.byName.getValue(price.item).id to it } }.toMap()
        val sellPrices = explicit.mapNotNull { price -> price.sell?.let { WorldShopGoods.byName.getValue(price.item).id to it } }.toMap()
        shops.open(
            player, title, WorldShopInvs.byName.getValue(shop.inventory),
            discount?.buy ?: shop.buy, discount?.sell ?: shop.sell, shop.change, currency,
            buyPrices = buyPrices, sellPrices = sellPrices,
        )
    }
}
