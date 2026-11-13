package org.rsmod.content.skills.hunter.implings

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object PuroLocs:LocReferences(){
    val circle=find("ii_magic_wheat_m");val zanaris=find("ii_magic_wheat_m_zanaris")
    val wheat=listOf("ii_maze_wheat_no_block1","ii_maze_wheat_block1","ii_maze_wheat_block2","ii_maze_wheat_block_moveable1","ii_maze_wheat_invis").map{find(it)}
}
public object PuroVars:VarpReferences(){val returnTile=find("puro_return_tile");val affinity=find("puro_affinity_until");val xpInitialised=find("puro_strength_xp_initialised")}
public object PuroControllers:ControllerBuilder(){val circle=build("puro_crop_circle_rotation")}
public object PuroAreas:AreaReferences(){val exit=find("puro_puro_vortex_exit")}
public object PuroSeqs:SeqReferences(){val push=find("human_push")}
internal object PuroVarpBuilds:VarpBuilder(){init{for(name in listOf("puro_return_tile","puro_affinity_until","puro_strength_xp_initialised"))build(name){transmitNever=true}}}
internal object PuroAreaBuilds:AreaBuilder(){init{build("puro_puro_vortex_exit")}}
internal object PuroExitMap:MapAreaBuilder(){override fun onPackMapTask(){area(PuroAreas.exit){polygon{for((x,z)in listOf(2591 to 4319,2592 to 4319,2592 to 4320,2591 to 4320))vertex(VertexCoord.from(CoordGrid(x,z)))}}}}

/** Crop circle oldid14819805, Puro-Puro/Magical wheat dated pre-October2025. */
@Singleton
public class PuroPuro @Inject constructor(private val locs:LocRepository,private val controllers:ControllerRepository){
    private var rotation=0
    private var circle:LocInfo?=null
    public fun startup(){
        rotate()
        val controller=Controller(PuroControllers.circle,fields[0]);controller.aiTimer(1500);controllers.add(controller,Int.MAX_VALUE)
        // Zanaris is permanent; the vanilla scenery does not include the dynamic centre tile.
        locs.add(CoordGrid(2427,4446),PuroLocs.zanaris,Int.MAX_VALUE,LocAngle.West,LocShape.CentrepieceStraight)
    }
    public fun rotate(){
        circle?.let{locs.del(it,Int.MAX_VALUE)}
        circle=locs.add(fields[rotation],PuroLocs.circle,Int.MAX_VALUE,LocAngle.West,LocShape.CentrepieceStraight)
        rotation=(rotation+1)%fields.size
    }
    public fun enter(access:ProtectedAccess,loc:BoundLocInfo):Unit=with(access){
        if(!player.members)return
        if(player.statBase(stats.hunter)<17){mes("You need a Hunter level of 17 to enter Puro-Puro.");return}
        vars[PuroVars.returnTile]=player.coords.packed
        if(loc.id==PuroLocs.circle.id)vars[PuroVars.affinity]=now()+1800
        if(vars[PuroVars.xpInitialised]==0){vars[ElnockBits.strengthXpOff]=1;vars[PuroVars.xpInitialised]=1}
        telejump(CoordGrid(2593,4320))
    }
    public fun exit(access:ProtectedAccess):Unit=with(access){
        val packed=vars[PuroVars.returnTile]
        val tile=if(packed!=0)CoordGrid(packed)else CoordGrid(2427,4444)
        telejump(if(ImplingHunting.puro(tile))CoordGrid(2427,4444)else tile)
    }
    public suspend fun push(access:ProtectedAccess,loc:BoundLocInfo):Unit=with(access){
        if(!player.members||!ImplingHunting.puro(player.coords))return
        val origin=player.coords
        val fast=statRandom(stats.strength,0,100,0)
        val medium=!fast&&statRandom(stats.strength,10,256,0)
        val ticks=if(fast)6 else if(medium)8 else 10
        val xp=if(fast)4.0 else if(medium)2.0 else 0.0
        val affinity=vars[PuroVars.affinity]>now()
        val left=loc.x;val right=loc.x+loc.adjustedWidth-1;val bottom=loc.z;val top=loc.z+loc.adjustedLength-1
        val destination=when{
            origin.x<left->CoordGrid(right+1,origin.z.coerceIn(bottom,top))
            origin.x>right->CoordGrid(left-1,origin.z.coerceIn(bottom,top))
            origin.z<bottom->CoordGrid(origin.x.coerceIn(left,right),top+1)
            else->CoordGrid(origin.x.coerceIn(left,right),bottom-1)
        }
        faceLoc(loc);anim(PuroSeqs.push);delay(ticks-if(affinity)3 else 0)
        if(player.coords!=origin)return
        telejump(destination)
        if(vars[ElnockBits.strengthXpOff]==0&&xp>0)statAdvance(stats.strength,xp)
    }
    public companion object{
        public fun now():Int=(System.currentTimeMillis()/1000L).toInt()
        public val fields=listOf(2953 to 3444,2582 to 3104,3115 to 3273,2978 to 3216,3141 to 3461,1302 to 3711,1738 to 3533,3810 to 2852,2195 to 3402,2819 to 3470,2435 to 3472,2808 to 3200,3703 to 2975,2896 to 3406,1660 to 3636,3160 to 3299,1396 to 2902,2647 to 3348,3212 to 3345,2538 to 3845,1674 to 3049).map{(x,z)->CoordGrid(x,z)}
    }
}
public class PuroPuroScript @Inject constructor(private val puro:PuroPuro):PluginScript(){override fun ScriptContext.startup(){
    onGameStartup{puro.startup()}
    onAiConTimer(PuroControllers.circle){puro.rotate();controller.aiTimer(1500)}
    onOpLoc1(PuroLocs.circle){puro.enter(this,it.loc)}
    onOpLoc1(PuroLocs.zanaris){puro.enter(this,it.loc)}
    onArea(PuroAreas.exit){puro.exit(this)}
    for(wheat in PuroLocs.wheat)onOpLoc5(wheat){puro.push(this,it.loc)}
}}
