package org.rsmod.content.skills.magic.spell.utility.arena

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ArenaRewards @Inject internal constructor(
    private val points:ArenaPoints,private val enums:EnumTypeList,private val types:ObjTypeList
) : PluginScript() {
    private val stock get() = enums.getValue(2753).primitiveMap

    override fun ScriptContext.startup(){
        onOpNpc4(ArenaNpcs.rewards) { open() }
        onOpNpc1(ArenaNpcs.rewards) {
            if(choice2("Browse the rewards.",true,"Buy an arena book for 200 coins.",false)) open()
            else if(player.members) {
                val result=player.invTransaction(inv) { val bag=select(inv);delete(bag,objs.coins.id,200);add(bag,ArenaItems.book.id,1) }
                if(!result.success) mes("You need 200 coins and room for the book.")
            }
        }
        for(hat in ArenaItems.hats){
            onOpHeld1(hat) { report() }
            onOpWorn2(hat) { report() }
        }
        onIfModalButton(ArenaButtons.rewards) {
            val item = stock[it.comsub] as? Int ?: return@onIfModalButton
            if(it.op==IfButtonOp.Op2){mes(types.getValue(item).desc);return@onIfModalButton}
            vars[ArenaBits.selected]=it.comsub
            vars[ArenaVars.quantity]=1
        }
        for((i,button) in ArenaButtons.quantities.withIndex()) onIfModalButton(button) {
            val id=stock[vars[ArenaBits.selected]] as? Int ?: return@onIfModalButton
            val max=maximum(id)
            vars[ArenaVars.quantity]=minOf(listOf(1,5,10,Int.MAX_VALUE)[i],max.coerceAtLeast(1))
        }
        onIfModalButton(ArenaButtons.confirm) { buy() }
    }

    private fun ProtectedAccess.report() {
        points.refreshHat(player)
        mes(ArenaRoom.entries.joinToString(", ") { "${it.name}: ${points.balance(player,it)}" } + ".")
    }
    private fun ProtectedAccess.open() {
        if(!player.members)return
        vars[ArenaBits.selected]=63
        vars[ArenaVars.quantity]=1
        for(room in ArenaRoom.entries) vars[ArenaVars.displays[room.ordinal]]=points.balance(player,room)
        ifOpenMainModal(ArenaUi.shop)
        ifSetEvents(ArenaButtons.rewards,0 until stock.size,IfEvent.Op1,IfEvent.Op2)
    }
    private fun cost(room:ArenaRoom,id:Int)= enums.getValue(room.priceEnum).primitiveMap.getValue(id) as Int
    private fun ProtectedAccess.maximum(id:Int):Int = ArenaRoom.entries.mapNotNull { room ->
        val price=cost(room,id)
        if(price>0) points.balance(player,room)/price else null
    }.minOrNull() ?: 0
    private fun ProtectedAccess.buy(){
        val id=stock[vars[ArenaBits.selected]] as? Int ?: return
        val count=vars[ArenaVars.quantity]
        if(count<=0 || !player.members || count>maximum(id)){
            mes("You do not have enough pizazz points for this reward.")
            return
        }
        if(id==6926 && (count!=1 || vars[varbits.magictraining_bonespeaches]!=0)) {mes("You can only learn this spell once.");return}
        if(id==objs.rune_pouch.id && (count!=1 || listOf(objs.rune_pouch,objs.rune_pouch_l,objs.divine_rune_pouch,objs.divine_rune_pouch_l).any {it in inv || it in bank})){
            mes("You can only have one rune pouch.");return
        }
        if(id!=6926){
            val required=enums.getValue(2759).primitiveMap[id] as? Int
            val result=player.invTransaction(inv) {
                val bag=select(inv)
                if(required!=null) delete(bag,required,count)
                add(bag,id,count)
            }
            if(!result.success){mes(if(required!=null && invTotal(inv,types.getValue(required))<count) "Bring the previous wand to upgrade it." else "You need more inventory space.");return}
        }else vars[varbits.magictraining_bonespeaches]=1
        for(room in ArenaRoom.entries) points.change(player,room,-cost(room,id)*count)
        points.refreshHat(player)
        mes(if(id==6926) "You have unlocked Bones to Peaches." else "You buy ${types.getValue(id).name}.")
    }
}
