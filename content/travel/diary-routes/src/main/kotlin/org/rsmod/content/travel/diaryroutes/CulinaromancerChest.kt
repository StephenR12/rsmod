package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object KitchenChestLoc : LocReferences() { val chest = find("hundred_goodchest_base") }
internal object KitchenChestBits : VarBitReferences() {
    val quest=find("hundred_main_quest_var")
    val guests=find("hundred_subquest_tally")
}
internal object KitchenChestInv : InvReferences() {
    val food=(1..10).map { find("hundred_foodchest$it") }
    val equipment=(1..10).map { find("hundred_rewardchest$it") }
}
internal data class KitchenStock(val food:Boolean,val item:ObjType,val stages:List<Pair<Int,Int>?>)
internal object KitchenGoods : ObjReferences() {
    val gloves=(1..10).map { find("hundred_gauntlets_level_$it") }
    val stock=CulinaromancerChest::class.java.getResourceAsStream("culinaromancer-stock.tsv")!!.bufferedReader().useLines { lines ->
        lines.drop(2).map { line ->
            val cols=line.split('\t')
            KitchenStock(cols[0]=="food", find(cols[1]), cols.drop(2).map { entry ->
                if(entry=="-") null else entry.split('/').let { (count,ticks)-> count.toInt() to ticks.toInt() }
            })
        }.toList()
    }
}

internal object KitchenChestStock:InvEditor() {
    init { for(food in listOf(true,false)) {
        val types=if(food) KitchenChestInv.food else KitchenChestInv.equipment
        for((stage,type) in types.withIndex()) edit(type) {
            scope=InvScope.Shared
            stack=InvStackType.Always
            autoSize=true
            restock=true
            for(line in KitchenGoods.stock) if(line.food==food) {
                val (count,ticks)=line.stages[stage] ?: continue
                stock+=stock(line.item,count,ticks)
            }
        }
    } }
}
internal object KitchenGloveSales:ObjEditor() {
    init { for(glove in KitchenGoods.gloves) edit(glove) { param[params.shop_sale_restricted]=true } }
}
public class CulinaromancerChest @Inject constructor(private val shops:Shops):PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(KitchenChestLoc.chest) { if(accessible()) ifOpenMainSidePair(interfaces.bank_main,interfaces.bank_side) }
        onOpLoc3(KitchenChestLoc.chest) { if(accessible()) ifOpenMainModal(interfaces.ge_collection_box) }
        onOpLoc4(KitchenChestLoc.chest) { open(true) }
        onOpLoc5(KitchenChestLoc.chest) { open(false) }
        onEvent<ObjTradeEvents.Purchased> {
            if(shop.id==KitchenChestInv.equipment.last().id && obj.id==KitchenGoods.gloves.last().id && player.vars[KitchenChestBits.quest]>=5) {
                player.completeDiaryTask(DiaryTask.BuyCulinaromancerGloves)
            }
        }
    }
    private fun ProtectedAccess.accessible()=player.members && player.vars[KitchenChestBits.quest] in 3..5
    private fun ProtectedAccess.open(food:Boolean) {
        if(!accessible()) return
        val stage=if(player.vars[KitchenChestBits.quest]==5) 9 else player.vars[KitchenChestBits.guests].coerceIn(0,8)
        val stock=if(food) KitchenChestInv.food[stage] else KitchenChestInv.equipment[stage]
        val discount=player.vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).complete] != 0
        shops.open(player,"Culinaromancer's Chest",stock,40.0,if(discount)104.0 else 130.0,2.0)
    }
}
