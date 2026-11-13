package org.rsmod.content.generic.locs.staircase

import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object KeldagrimStairTypes : LocReferences(){
    val lower=find("dwarf_keldagrim_stairs_lower")
    val upper=find("dwarf_keldagrim_stairs_upper")
}

/** The lower flight covers two tiles. The upper model sits over the higher of those two. */
public class KeldagrimStairs : PluginScript(){
    override fun ScriptContext.startup(){
        onOpLoc1(KeldagrimStairTypes.lower){telejump(landing(it.loc,true))}
        onOpLoc1(KeldagrimStairTypes.upper){telejump(landing(it.loc,false))}
    }
    private fun landing(loc:BoundLocInfo,up:Boolean):CoordGrid{
        val angle=loc.angleId
        val dx=listOf(0,1,0,-1)[angle]
        val dz=listOf(1,0,-1,0)[angle]
        val distance=if(up && dx+dz>0 || !up && dx+dz<0)2 else 1
        val sign=if(up)1 else -1
        return CoordGrid(loc.x+dx*distance*sign,loc.z+dz*distance*sign,loc.level+sign)
    }
}
