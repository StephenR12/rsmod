package org.rsmod.content.skills.hunter.implings

import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

public object ElnockBits:VarBitReferences(){val net=find("ii_stored_net");val repellent=find("ii_stored_repellent");val jars=find("ii_stored_impling_jars");val purchased=find("ii_daily_jars_purchased");val strengthXpOff=find("ii_strength_xp_off")}
public object ElnockVars:VarpReferences(){val day=find("impling_last_jar_purchase_day")}
internal object ElnockVarpBuilder:VarpBuilder(){init{build("impling_last_jar_purchase_day"){transmitNever=true}}}
/** Elnock Inquisitor oldid14856949; ordinary supplies are atomic and the free kit is one-time. */
public class ElnockInquisitor {
    public suspend fun talk(access:ProtectedAccess)=with(access){
        if(!player.members)return
        if(vars[ImplingBits.freeKit]==0){
            val result=player.invTransaction(player.inv){val inv=select(player.inv);add(inv,ImplingObjs.net.id,1);add(inv,ImplingObjs.empty.id,7)}
            if(!result.success){mes("You need eight free inventory spaces for a butterfly net and seven jars.");return}
            vars[ImplingBits.freeKit]=1;mes("Elnock gives you a butterfly net and seven impling jars.");return
        }
        when(menu("How can Elnock help?","Trade implings and buy jars.","Store supplies.","Withdraw supplies.","Toggle wheat-pushing Strength XP.")){
            1->trade(this);2->exchange(this);3->withdraw(this);4->{vars[ElnockBits.strengthXpOff]=if(vars[ElnockBits.strengthXpOff]==0)1 else 0;mes("Strength experience from pushing wheat is now ${if(vars[ElnockBits.strengthXpOff]==0)"enabled" else "disabled"}.")}
        }
    }
    public suspend fun trade(access:ProtectedAccess)=with(access){
        if(!player.members)return
        when(menu("What would you like to trade?","Any captured impling for three jars.","Three gourmet, two earth, one essence for a magic net.","Three essence, two eclectic, one nature for a jar generator.","Buy a jar (2,000 coins, up to ten per day).","Three baby, two young, one gourmet for imp repellent.")){
            1->{val jar=ImplingObjs.jars.firstOrNull{it in player.inv}?:run{mes("Bring Elnock a captured impling.");return};tradeItems(this,listOf(jar.id to 1),ImplingObjs.empty.id,3)}
            2->tradeItems(this,listOf(ImplingObjs.jars[2].id to 3,ImplingObjs.jars[3].id to 2,ImplingObjs.jars[4].id to 1),ImplingObjs.magicNet.id,1)
            3->{
                val cost=listOf(ImplingObjs.jars[4].id to 3,ImplingObjs.jars[5].id to 2,ImplingObjs.jars[6].id to 1)
                val slot=player.inv.indices.firstOrNull{player.inv[it]?.id==ImplingObjs.generator.id}
                if(slot!=null){if(menu("Recharge your existing jar generator for the same implings?","Yes.","No.")!=1)return;val result=player.invTransaction(player.inv){val inv=select(player.inv);for((id,n)in cost)delete(inv,id,n);delete(inv,ImplingObjs.generator.id,1,slot);add(inv,ImplingObjs.generator.id,1,slot=slot)};if(!result.success)mes("You don't have the required implings.")}
                else if(player.invMap.values.any{ImplingObjs.generator in it})mes("Withdraw your existing jar generator before recharging it.")
                else tradeItems(this,cost,ImplingObjs.generator.id,1)
            }
            5->tradeItems(this,listOf(ImplingObjs.jars[0].id to 3,ImplingObjs.jars[1].id to 2,ImplingObjs.jars[2].id to 1),ImplingObjs.repellent.id,1)
            4->{val day=(System.currentTimeMillis()/86_400_000L).toInt();if(vars[ElnockVars.day]!=day){vars[ElnockVars.day]=day;vars[ElnockBits.purchased]=0};if(vars[ElnockBits.purchased]>=10){mes("Elnock has no more jars for sale today.");return};if(tradeItems(this,listOf(objs.coins.id to 2000),ImplingObjs.empty.id,1))vars[ElnockBits.purchased]++}
        }
    }
    public fun tradeItems(access:ProtectedAccess,cost:List<Pair<Int,Int>>,output:Int,count:Int):Boolean=with(access){
        val result=player.invTransaction(player.inv){val inv=select(player.inv);for((id,n)in cost)delete(inv,id,n);add(inv,output,count)}
        if(!result.success)mes("You don't have the required items or enough inventory space.")
        result.success
    }
    public suspend fun exchange(access:ProtectedAccess)=with(access){
        if(!player.members)return
        when(menu("What would you like Elnock to store?","A butterfly net.","An imp repellent.","All empty impling jars (up to 1,000).")){
            1->{if(vars[ElnockBits.net]!=0){mes("Elnock already has your net.");return};if(store(this,ImplingObjs.net.id,1))vars[ElnockBits.net]=1}
            2->{if(vars[ElnockBits.repellent]!=0){mes("Elnock already has your repellent.");return};if(store(this,ImplingObjs.repellent.id,1))vars[ElnockBits.repellent]=1}
            3->{val n=minOf(1000-vars[ElnockBits.jars],player.inv.indices.count{player.inv[it]?.id==ImplingObjs.empty.id});if(n>0&&store(this,ImplingObjs.empty.id,n))vars[ElnockBits.jars]+=n}
        }
    }
    private fun store(access:ProtectedAccess,id:Int,n:Int):Boolean=with(access){player.invTransaction(player.inv){delete(select(player.inv),id,n)}.success}
    public fun withdraw(access:ProtectedAccess)=with(access){
        if(!player.members)return
        if(vars[ElnockBits.net]!=0&&player.invTransaction(player.inv){add(select(player.inv),ImplingObjs.net.id,1)}.success)vars[ElnockBits.net]=0
        if(vars[ElnockBits.repellent]!=0&&player.invTransaction(player.inv){add(select(player.inv),ImplingObjs.repellent.id,1)}.success)vars[ElnockBits.repellent]=0
        val n=minOf(player.inv.indices.count{player.inv[it]==null},vars[ElnockBits.jars]);if(n>0&&player.invTransaction(player.inv){add(select(player.inv),ImplingObjs.empty.id,n)}.success)vars[ElnockBits.jars]-=n
    }
}
