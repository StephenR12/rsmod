package org.rsmod.content.skills.hunter.implings

import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Per-item used charge (0=fresh100), Jar generator oldid14744515. */
public class JarGenerator:PluginScript(){override fun ScriptContext.startup(){
    onOpHeld1(ImplingObjs.generator){generate(this,it.slot,impling=true)}
    onOpHeld3(ImplingObjs.generator){generate(this,it.slot,impling=false)}
    onOpHeld4(ImplingObjs.generator){val obj=player.inv[it.slot];if(obj?.id==ImplingObjs.generator.id)mes("Your jar generator has ${100-obj.vars.coerceIn(0,100)}% charge remaining.")}
}
    public fun generate(access:ProtectedAccess,slot:Int,impling:Boolean):Unit=with(access){
        if(!player.members||slot !in player.inv.indices)return
        val obj=player.inv[slot]?:return;if(obj.id!=ImplingObjs.generator.id)return
        val cost=if(impling)3 else 1;val remaining=100-obj.vars.coerceIn(0,100)
        if(remaining<cost){mes("Your jar generator does not have enough charge to create this jar.");return}
        val result=player.invTransaction(player.inv){val inv=select(player.inv);delete(inv,obj.id,1,slot);if(remaining>cost)add(inv,obj.id,1,vars=obj.vars+cost,slot=slot);add(inv,if(impling)ImplingObjs.empty.id else ImplingObjs.butterflyJar.id,1)}
        if(!result.success){mes("You need more inventory space to create a jar.");return}
        if(remaining==cost)mes("Your jar generator runs out of charge and disappears.")
    }
}
