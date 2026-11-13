package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object FarmShopInvs : InvReferences() {
    val tools = (1..4).map { find("farming_shop_$it") }
    val seeds = find("seed_stall")
}

internal object FarmShopNpcs : NpcReferences() {
    val farmers = (1..4).map { find("farming_shopkeeper_$it") }
    val olivia = find("seed_merchant")
}

internal data class FarmStock(val obj: ObjType, val count: Int, val restock: Int)

internal object FarmShopGoods : ObjReferences() {
    val tools =
        listOf(
            FarmStock(find("rake"), 500, 10),
            FarmStock(find("dibber"), 500, 10),
            FarmStock(find("secateurs"), 500, 10),
            FarmStock(find("spade"), 500, 10),
            FarmStock(find("gardening_trowel"), 500, 120),
            FarmStock(find("watering_can_0"), 500, 10),
            FarmStock(find("plantpot_compost"), 500, 10),
            FarmStock(find("pack_plant_pot_compost"), 30, 2),
            FarmStock(find("bucket_compost"), 500, 10),
            FarmStock(find("pack_compost"), 10, 50),
            FarmStock(find("sack_empty"), 500, 10),
            FarmStock(find("pack_sack"), 30, 2),
            FarmStock(find("basket_empty"), 500, 10),
            FarmStock(find("pack_basket"), 30, 2),
            FarmStock(find("bucket_empty"), 100, 10),
            FarmStock(find("pack_bucket"), 15, 10),
            FarmStock(find("potato"), 0, 100),
            FarmStock(find("onion"), 0, 100),
            FarmStock(find("cabbage"), 0, 100),
            FarmStock(find("tomato"), 0, 100),
            FarmStock(find("sweetcorn"), 0, 100),
            FarmStock(find("strawberry"), 0, 100),
            FarmStock(find("watermelon"), 0, 100),
            FarmStock(find("hammerstone_hops"), 0, 100),
            FarmStock(find("asgarnian_hops"), 0, 100),
            FarmStock(find("yanillian_hops"), 0, 100),
            FarmStock(find("krandorian_hops"), 0, 100),
            FarmStock(find("wildblood_hops"), 0, 100),
            FarmStock(find("jute_fibre"), 0, 100),
            FarmStock(find("barley"), 0, 100),
            FarmStock(find("plant_cure"), 100, 10),
        )
    val seeds =
        listOf(
            FarmStock(find("potato_seed"), 20, 100),
            FarmStock(find("onion_seed"), 10, 100),
            FarmStock(find("cabbage_seed"), 5, 100),
            FarmStock(find("tomato_seed"), 0, 100),
            FarmStock(find("sweetcorn_seed"), 0, 100),
            FarmStock(find("strawberry_seed"), 0, 100),
            FarmStock(find("watermelon_seed"), 0, 100),
            FarmStock(find("barley_seed"), 20, 100),
            FarmStock(find("jute_seed"), 5, 100),
            FarmStock(find("rosemary_seed"), 20, 100),
            FarmStock(find("marigold_seed"), 20, 100),
            FarmStock(find("hammerstone_hop_seed"), 20, 100),
            FarmStock(find("asgarnian_hop_seed"), 10, 100),
            FarmStock(find("yanillian_hop_seed"), 5, 100),
            FarmStock(find("krandorian_hop_seed"), 0, 100),
            FarmStock(find("wildblood_hop_seed"), 0, 100),
        )
}

internal object FarmShopStock : InvEditor() {
    init {
        val shops =
            FarmShopInvs.tools.associateWith { FarmShopGoods.tools } +
                (FarmShopInvs.seeds to FarmShopGoods.seeds)
        for ((inventory, goods) in shops) {
            edit(inventory) {
                scope = InvScope.Shared
                stack = InvStackType.Always
                autoSize = true
                restock = true
                for (item in goods) stock +=
                    stock(item.obj, count = item.count, restockCycles = item.restock)
            }
        }
    }
}

public class FarmingShops
@Inject
constructor(private val shops: Shops, private val locTypes: LocTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        val sources =
            setOf(
                "sink",
                "farm_sink",
                "farm_water_barrel1",
                "farm_water_barrel2",
                "water_barrel1",
                "water_barrel2",
                "fai_varrock_cooking_kitchen_sink",
                "fai_varrock_posh_sink",
                "fai_varrock_waterpump",
                "rimmington_sink",
                "draynor_fountain",
            )
        for (source in locTypes.values.filter { it.internalName in sources }) {
            onOpLocU(source, org.rsmod.api.config.refs.objs.gricollers_can) {
                vars[TitheRewardVars.water] = 1000
                mes("You fill Gricoller's can with water.")
            }
            for (can in FarmingObjs.wateringCans.dropLast(1)) {
                onOpLocU(source, can) {
                    if (
                        invReplaceSlot(inv, it.invSlot, 1, FarmingObjs.wateringCans.last()).success
                    ) {
                        mes("You fill the watering can with water.")
                    }
                }
            }
        }
        val farmers = listOf("Sarah", "Vanessa", "Richard", "Alice")
        FarmShopNpcs.farmers.forEachIndexed { index, npc ->
            onOpNpc3(npc) {
                if (player.members) {
                    shops.open(
                        player,
                        "${farmers[index]}'s Farming shop",
                        FarmShopInvs.tools[index],
                        70.0,
                        100.0,
                        1.0,
                    )
                    if (index == 0) player.completeDiaryTask(DiaryTask.BrowseSarahFarmingShop)
                }
            }
        }
        onOpNpc3(FarmShopNpcs.olivia) {
            if (player.members)
                shops.open(player, "Draynor Seed Market", FarmShopInvs.seeds, 60.0, 120.0, 3.0)
        }
    }
}
