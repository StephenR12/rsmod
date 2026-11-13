package org.rsmod.content.skills.woodcutting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object NeitiznotStumpLocs:LocReferences(){val stump=find("iznot_shield_stump")}
public object NeitiznotStumpSeqs:SeqReferences(){val smith=find("human_smithing")}
public object NeitiznotStumpObjs:ObjReferences(){val split=find("arctic_pine_split");val shield=find("fremmenik_round_shield")}
/** Stump oldid14986529, Split log14424328, shield14841174; each action three cycles. */
public class NeitiznotStump @Inject constructor(private val types:ObjTypeList,private val xpMods:XpModifiers){
    public suspend fun select(access:ProtectedAccess,stump:BoundLocInfo):Unit=with(access){
        if(!player.members||!isWithinDistance(stump,1))return
        val count=player.inv.count(types[objs.arctic_pine_logs])
        if(count==0){mes("You need arctic pine logs to use this stump.");return}
        val input=skillMulti("What would you like to make?",listOf(NeitiznotStumpObjs.split,NeitiznotStumpObjs.shield),count,count,actionMode=0)
        val shield=when(input.component.packed){components.skillmulti_a.packed->false;components.skillmulti_b.packed->true;else->return}
        make(this,stump,shield,input.subcomponent)
    }
    public suspend fun make(access:ProtectedAccess,stump:BoundLocInfo,shield:Boolean,quantity:Int):Unit=with(access){
        if(quantity<=0||!canMake(this,stump,shield))return
        repeat(minOf(quantity,28)){
            if(!canMake(this,stump,shield))return
            faceLoc(stump)
            if(shield)anim(NeitiznotStumpSeqs.smith)else Woodcutting.findAxe(player,types)?.let{anim(types[it].axeWoodcuttingAnim)}
            delay(3)
            if(!canMake(this,stump,shield)){resetAnim();return}
            val result=player.invTransaction(player.inv){val inv=select(player.inv);delete(inv,objs.arctic_pine_logs.id,if(shield)2 else 1);if(shield){delete(inv,objs.rope.id,1);delete(inv,objs.bronze_nails.id,1)};add(inv,if(shield)NeitiznotStumpObjs.shield.id else NeitiznotStumpObjs.split.id,1)}
            if(!result.success){resetAnim();return}
            if(shield){statAdvance(stats.crafting,34.0);player.completeDiaryTask(DiaryTask.CraftNeitiznotShield);mes("You make a Fremennik round shield.")}
            else{statAdvance(stats.woodcutting,5.0*xpMods.get(player,stats.woodcutting));mes("You split the arctic pine log.")}
        }
        resetAnim()
    }
    private fun canMake(access:ProtectedAccess,stump:BoundLocInfo,shield:Boolean):Boolean=with(access){
        if(!player.members||stump.id!=NeitiznotStumpLocs.stump.id||!isWithinDistance(stump,1))return false
        if(player.stat(stats.woodcutting)<56){mes("You need a Woodcutting level of 56 to use this stump.");return false}
        if(shield){
            if(vars[varbits.fremennik_isles_progress]<290){mes("Mawnis must teach you how to make a Fremennik round shield first.");return false}
            val hammers=listOf(objs.hammer,objs.imcando_hammer,objs.imcando_hammer_offhand)
            if(hammers.none{it in player.inv}&&hammers.drop(1).none{it in player.worn}){mes("You need a hammer to make a shield.");return false}
            if(player.inv.count(types[objs.arctic_pine_logs])<2||objs.rope !in player.inv||objs.bronze_nails !in player.inv){mes("You need two arctic pine logs, a rope and a bronze nail.");return false}
        }else{
            if(Woodcutting.findAxe(player,types)==null){mes("You need an axe to split logs.");return false}
            if(objs.arctic_pine_logs !in player.inv)return false
        }
        true
    }
}
public class NeitiznotStumpScript @Inject constructor(private val stump:NeitiznotStump):PluginScript(){override fun ScriptContext.startup(){
    onOpLoc1(NeitiznotStumpLocs.stump){stump.select(this,it.loc)}
    onOpLocU(NeitiznotStumpLocs.stump,objs.arctic_pine_logs){stump.select(this,it.loc)}
}}
