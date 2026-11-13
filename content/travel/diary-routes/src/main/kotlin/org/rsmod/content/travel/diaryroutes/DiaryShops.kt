package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DiaryShopInvs : InvReferences() {
    val desert = find("shantayshop")
    val legends = find("legendsguildshop")
    val clothing = find("clotheshop")
    val fish = find("fishingshop2")
    val yrsa = find("viking_clothes_shop")
    val hunting = find("hunting_shop_yanille")
    val candles = find("candleshop")
    val warrens = find("warrens_general_store")
}

internal object DiaryShopNpcs : NpcReferences() {
    val shantay = find("shantay")
    val siegfried = find("siegfried_erkle")
    val thessalia = find("thessalia_normal")
    val aleck = find("hunting_shop_owner_yanille")
    val candleMaker = find("candle_maker")
    val shopkeeper = find("warrens_shopkeeper")
}

internal object DiaryGoods : ObjReferences() {
    data class Goods(val obj: ObjType, val count: Int, val restock: Int)

    val desert = listOf(
        Goods(find("water_skin4"),100,10), Goods(find("water_skin0"),100,10),
        Goods(find("jug_water"),10,100), Goods(find("bowl_water"),10,100), Goods(find("bucket_water"),10,100),
        Goods(find("knife"),10,100), Goods(find("desert_shirt"),10,500), Goods(find("desert_robe"),10,500),
        Goods(find("desert_boots"),10,500), Goods(find("bronze_bar"),10,500), Goods(find("feather"),500,50),
        Goods(find("pack_feather"),50,20), Goods(find("hammer"),10,500), Goods(find("pack_bucket"),15,10),
        Goods(find("bucket_empty"),0,100), Goods(find("bowl_empty"),0,100), Goods(find("jug_empty"),0,100),
        Goods(find("pack_jug_empty"),0,50), Goods(find("shantay_pass"),500,1), Goods(find("rope"),20,200)
    )
    val legends = listOf(Goods(find("mithril_seed"),6,50), Goods(find("dusty_key"),5,100),
        Goods(find("melzarkey"),3,300), Goods(find("dragonshield_b"),1,500), Goods(find("cape_of_legends"),3,500))

    val clothing =
        listOf(
            Goods(find("white_apron"), 3, 100),
            Goods(find("leather_armour"), 12, 100),
            Goods(find("leather_gloves"), 10, 100),
            Goods(find("leather_boots"), 10, 100),
            Goods(find("brown_apron"), 1, 100),
            Goods(find("pink_skirt"), 5, 100),
            Goods(find("black_skirt"), 3, 100),
            Goods(find("blue_skirt"), 2, 100),
            Goods(find("red_cape"), 4, 100),
            Goods(find("silk"), 5, 500),
            Goods(find("priest_gown"), 3, 100),
            Goods(find("priest_robe"), 3, 100),
        )
    val hunting =
        listOf(
            Goods(find("hunting_butterfly_net"), 5, 10),
            Goods(find("butterfly_jar"), 100, 10),
            Goods(find("magic_imp_box"), 30, 50),
            Goods(find("noose_wand"), 50, 5),
            Goods(find("hunting_ojibway_bird_snare"), 50, 5),
            Goods(find("hunting_box_trap"), 25, 10),
            Goods(find("hunting_teasing_stick"), 5, 10),
            Goods(find("torch_unlit"), 20, 10),
            Goods(find("hunting_snare"), 10, 10),
            Goods(find("pack_ojibway_bird_snare"), 3, 10),
            Goods(find("pack_box_trap"), 3, 10),
            Goods(find("pack_magic_imp_box"), 3, 10),
        )
    val warrens =
        listOf(
            Goods(find("pot_empty"), 3, 100),
            Goods(find("jug_empty"), 2, 100),
            Goods(find("pack_jug_empty"), 5, 15),
            Goods(find("shears"), 2, 100),
            Goods(find("bucket_empty"), 2, 100),
            Goods(find("pack_bucket"), 10, 10),
            Goods(find("tinderbox"), 2, 100),
            Goods(find("chisel"), 2, 100),
            Goods(find("hammer"), 5, 100),
            Goods(find("spade"), 5, 100),
            Goods(find("knife"), 4, 100),
        )
    val fish = listOf(
        Goods(find("net"),5,100),
        Goods(find("fishing_rod"),5,100),
        Goods(find("harpoon"),2,400),
        Goods(find("lobster_pot"),2,400),
        Goods(find("fishing_bait"),1200,1),
        Goods(find("pack_fishing_bait"),80,5),
        Goods(find("big_net"),5,100),
        Goods(find("raw_shrimp"),0,300),
        Goods(find("raw_sardine"),0,600),
        Goods(find("raw_herring"),0,900),
        Goods(find("raw_mackerel"),0,1000),
        Goods(find("raw_cod"),0,1100),
        Goods(find("raw_anchovies"),0,1200),
        Goods(find("raw_tuna"),0,2300),
        Goods(find("raw_lobster"),0,2600),
        Goods(find("raw_bass"),0,2700),
        Goods(find("raw_swordfish"),0,2900),
        Goods(find("raw_shark"),0,3500)
    )
    val yrsa = listOf(
        Goods(find("viking_top_blue"),5,100),
        Goods(find("viking_top_red"),5,100),
        Goods(find("viking_top_darkbrown"),5,100),
        Goods(find("viking_top_green"),5,100),
        Goods(find("viking_top_beige"),5,100),
        Goods(find("vikingrobetop"),5,100),
        Goods(find("vikingrobebottom"),5,100),
        Goods(find("vikinghat"),5,100),
        Goods(find("vikingboots"),5,100),
        Goods(find("vikinggloves"),5,100),
        Goods(find("viking_cloak_brown"),5,100),
        Goods(find("viking_cloak_lightbrown"),5,100),
        Goods(find("viking_cloak_darkbrown"),5,100),
        Goods(find("viking_cloak_green"),5,100),
        Goods(find("viking_cloak_red"),5,100),
        Goods(find("viking_cloak_grey"),5,100),
        Goods(find("viking_cloak_tangerine"),5,100),
        Goods(find("viking_cloak_ocean"),5,100),
        Goods(find("viking_cloak_purple"),5,100),
        Goods(find("viking_cloak_pink"),5,100),
        Goods(find("viking_cloak_black"),5,100)
    )
    val candles = listOf(Goods(find("unlit_candle"), 10, 100))
}

internal object DiaryShopStock : InvEditor() {
    init {
        for ((shop, goods) in
            mapOf(
                DiaryShopInvs.desert to DiaryGoods.desert,
                DiaryShopInvs.legends to DiaryGoods.legends,
                DiaryShopInvs.clothing to DiaryGoods.clothing,
                DiaryShopInvs.fish to DiaryGoods.fish,
                DiaryShopInvs.yrsa to DiaryGoods.yrsa,
                DiaryShopInvs.hunting to DiaryGoods.hunting,
                DiaryShopInvs.candles to DiaryGoods.candles,
            )) {
            edit(shop) {
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

internal object WarrensStock : InvBuilder() {
    init {
        build("warrens_general_store") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (item in DiaryGoods.warrens) stock +=
                stock(item.obj, count = item.count, restockCycles = item.restock)
        }
    }
}

public class DiaryShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(CatherbyCitizens.harry) { harry() }
        onApNpc3(CatherbyCitizens.harry) { if (isWithinApRange(it.npc,2)) harry() }
        onOpNpc3(MakeoverNpcs.yrsa) { yrsa() }
        onApNpc3(MakeoverNpcs.yrsa) { if (isWithinApRange(it.npc,2)) yrsa() }
        onOpNpc3(GateNpcs.guard) { shops.open(player,"Shantay Pass Shop.",DiaryShopInvs.desert,70.0,100.0,1.0) }
        onOpNpc3(DiaryShopNpcs.shantay) { shops.open(player,"Shantay Pass Shop.",DiaryShopInvs.desert,70.0,100.0,1.0) }
        onOpNpc3(DiaryShopNpcs.siegfried) { legends() }
        onOpNpc1(DiaryShopNpcs.siegfried) { if (choice2("Trade.",true,"Leave.",false)) legends() }
        onOpNpc3(DiaryShopNpcs.thessalia) { clothing() }
        onApNpc3(DiaryShopNpcs.thessalia) { if (isWithinApRange(it.npc, 2)) clothing() }
        onOpNpc5(DiaryShopNpcs.aleck) { hunting() }
        onApNpc5(DiaryShopNpcs.aleck) { if (isWithinApRange(it.npc, 2)) hunting() }
        onOpNpc3(DiaryShopNpcs.shopkeeper) { warrens() }
        onApNpc3(DiaryShopNpcs.shopkeeper) { if (isWithinApRange(it.npc, 2)) warrens() }
        onOpNpc3(DiaryShopNpcs.candleMaker) { candles() }
        onApNpc3(DiaryShopNpcs.candleMaker) { if (isWithinApRange(it.npc, 2)) candles() }
        onEvent<ObjTradeEvents.Purchased> {
            if (shop.id == DiaryShopInvs.candles.id && obj.id == DiaryGoods.candles.single().obj.id)
                player.completeDiaryTask(DiaryTask.BuyCatherbyCandle)
        }
    }

    private fun ProtectedAccess.harry() {
        if (player.members) shops.open(player,"Harry's Fishing Shop.",DiaryShopInvs.fish,70.0,100.0,1.0)
    }
    private fun ProtectedAccess.yrsa() {
        if (player.members && vars[MakeoverVars.trials] >= 10) shops.open(player,"Yrsa's Accoutrements.",DiaryShopInvs.yrsa,70.0,130.0,3.0)
        else mes("Yrsa only trades with members of the Fremennik.")
    }

    private fun ProtectedAccess.legends() {
        if (!player.members || vars[varps.legends_quest] < 75) {
            mes("The shop is open to members of the Legends' Guild.")
            return
        }
        shops.open(player,"Legends Guild Shop of Useful Items.",DiaryShopInvs.legends,50.0,150.0,2.0)
    }

    private fun ProtectedAccess.clothing() {
        shops.open(player, "Thessalia's Fine Clothes.", DiaryShopInvs.clothing, 55.0, 100.0, 1.0)
        player.completeDiaryTask(DiaryTask.BrowseThessalia)
    }

    private fun ProtectedAccess.hunting() {
        if (!player.members) return
        shops.open(player, "Aleck's Hunter Emporium.", DiaryShopInvs.hunting, 70.0, 120.0, 2.0)
        player.completeDiaryTask(DiaryTask.BrowseAleck)
    }

    private fun ProtectedAccess.warrens() {
        if (!player.members) return
        shops.open(player, "Warrens General Store", DiaryShopInvs.warrens, 40.0, 130.0, 3.0)
        player.completeDiaryTask(DiaryTask.BrowseWarrens)
    }

    private fun ProtectedAccess.candles() {
        if (player.members)
            shops.open(player, "Candle Shop", DiaryShopInvs.candles, 40.0, 130.0, 3.0)
    }
}
