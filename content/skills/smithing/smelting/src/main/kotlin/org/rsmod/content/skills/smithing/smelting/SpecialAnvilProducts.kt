package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.SkillDiaryAreas
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object SpecialAnvilObjs:ObjReferences(){val left=find("dragonshield_a");val right=find("dragonshield_b");val shield=find("dragon_sq_shield");val helmet=find("dwarf_goldrock_helmet")}
public object SpecialAnvilBits:VarBitReferences(){val rock=find("dwarfrock_quest")}
public enum class SpecialAnvilProduct{DragonShield,GoldHelmet}
/** Dragon sq shield oldid14994522; Gold helmet oldid14907651. */
public class SpecialAnvilProducts @Inject constructor(private val smithing:AnvilSmithing,private val types:ObjTypeList){
    public suspend fun select(access:ProtectedAccess,anvil:BoundLocInfo,product:SpecialAnvilProduct):Unit=with(access){
        if(!canMake(this,anvil,product))return
        val output=if(product==SpecialAnvilProduct.DragonShield)SpecialAnvilObjs.shield else SpecialAnvilObjs.helmet
        val maximum=if(product==SpecialAnvilProduct.GoldHelmet)1 else minOf(player.inv.count(types[SpecialAnvilObjs.left]),player.inv.count(types[SpecialAnvilObjs.right]))
        val input=skillMulti("What would you like to smith?",listOf(output),maximum,maximum,actionMode=0)
        if(input.component.packed==components.skillmulti_a.packed)make(this,anvil,product,input.subcomponent)
    }
    public suspend fun make(access:ProtectedAccess,anvil:BoundLocInfo,product:SpecialAnvilProduct,quantity:Int):Unit=with(access){
        if(quantity<=0||!canMake(this,anvil,product))return
        clearFurnaceQueues()
        repeat(minOf(quantity,28)){
            if(!canMake(this,anvil,product))return
            faceLoc(anvil);anim(AnvilSeqs.smith);delay(smithing.actionTicks(player))
            if(!canMake(this,anvil,product)){resetAnim();return}
            val output=if(product==SpecialAnvilProduct.DragonShield)SpecialAnvilObjs.shield else SpecialAnvilObjs.helmet
            val result=player.invTransaction(player.inv){val inv=select(player.inv);if(product==SpecialAnvilProduct.DragonShield){delete(inv,SpecialAnvilObjs.left.id,1);delete(inv,SpecialAnvilObjs.right.id,1)}else delete(inv,objs.gold_bar.id,3);add(inv,output.id,1)}
            if(!result.success){resetAnim();return}
            statAdvance(stats.smithing,if(product==SpecialAnvilProduct.DragonShield)75.0 else 30.0)
            val c=anvil.coords
            if(product==SpecialAnvilProduct.DragonShield&&c.level==0&&((c.x==2499&&c.z==3332)||(c.x==2501&&c.z==3330)))player.completeDiaryTask(DiaryTask.SmithWestArdougneDragonSquare)
            if(product==SpecialAnvilProduct.GoldHelmet&&SkillDiaryAreas.resourceArea(c))player.completeDiaryTask(DiaryTask.SmithResourceGoldHelmet)
        }
        resetAnim()
    }
    private fun canMake(access:ProtectedAccess,anvil:BoundLocInfo,product:SpecialAnvilProduct):Boolean=with(access){
        if(!player.members||!isWithinDistance(anvil,1)||(AnvilLocs.ordinary+AnvilLocs.doric).none{it.id==anvil.id})return false
        if(anvil.id==AnvilLocs.doric.id&&vars[AnvilQuestVars.doric]<100){mes("You need to complete Doric's Quest to use this anvil.");return false}
        val level=if(product==SpecialAnvilProduct.DragonShield)60 else 50
        if(player.stat(stats.smithing)<level){mes("You need a Smithing level of $level to smith this item.");return false}
        val hammers=listOf(objs.hammer,objs.imcando_hammer,objs.imcando_hammer_offhand)
        if(hammers.none{it in player.inv}&&hammers.drop(1).none{it in player.worn}){mes("You need a hammer to smith items.");return false}
        if(product==SpecialAnvilProduct.DragonShield){if(SpecialAnvilObjs.left !in player.inv||SpecialAnvilObjs.right !in player.inv){mes("You need both shield halves.");return false}}
        else{
            // The dated quest-helper's first helmet-making stage is80 (native varbit299).
            if(vars[SpecialAnvilBits.rock]<80){mes("You haven't learned how to make a gold helmet yet.");return false}
            if(player.invMap.values.any{SpecialAnvilObjs.helmet in it}){mes("You already have a gold helmet.");return false}
            if(player.inv.count(types[objs.gold_bar])<3){mes("You need three gold bars.");return false}
        }
        true
    }
}
public class SpecialAnvilScript @Inject constructor(private val products:SpecialAnvilProducts):PluginScript(){override fun ScriptContext.startup(){
    for(anvil in AnvilLocs.ordinary+AnvilLocs.doric){
        onOpLocU(anvil,SpecialAnvilObjs.left){products.select(this,it.loc,SpecialAnvilProduct.DragonShield)}
        onOpLocU(anvil,SpecialAnvilObjs.right){products.select(this,it.loc,SpecialAnvilProduct.DragonShield)}
        onOpLocU(anvil,objs.gold_bar){products.select(this,it.loc,SpecialAnvilProduct.GoldHelmet)}
    }
}}
