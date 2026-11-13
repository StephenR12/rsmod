package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.hit.HitType
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WolfVars:VarpReferences(){val quest=find("fenk_quest")}
internal object WolfItems:ObjReferences(){val ring=find("ring_of_charos");val unlocked=find("ring_of_charos_unlocked");val stick=find("waa_stick")}
internal object WolfLocs:LocReferences(){
    val trap=find("waa_trapdoor");val open=find("waa_trapdoor_open");val ladder=find("waa_ladder")
    val stone=find("werewolf_steping_stone");val hurdles=listOf("mid","end","end_mirror").map{find("werewolf_hurdle_$it")}
    val pipe=find("waa_pipe");val climb=find("werewolf_skull_climb_1");val slide=find("werewolf_slide_center")
}
internal object WolfActors:NpcReferences(){
    val guard=find("waa_werewolf_guard");val boss=find("werewolf_trainer_start");val collector=find("werewolf_trainer_stick")
    val skullBoss=find("werewolf_skullballboss");val coach=find("werewolf_trainer_1");val ball=find("waa_skullball")
}
internal object WolfActorEdits:NpcEditor(){
    init { for (npc in listOf(WolfActors.ball,WolfActors.boss,WolfActors.collector,WolfActors.guard,WolfActors.skullBoss,WolfActors.coach)) edit(npc){defaultMode=NpcMode.None} }
}
internal object WolfMoves:SeqReferences(){
    val jump=find("human_jump_stones");val hurdle=find("human_jump_hurdle");val slide=find("zipline_slide")
    val bite=find("zipline_bite");val pipe=find("human_pipesqueeze");val climb=find("human_climbing")
    val kick=find("kick_skull_ball")
}
internal object WerewolfSpawns:MapNpcSpawnBuilder(){override fun onPackMapTask(){resourceFile<WerewolfSpawns>("werewolf-course.toml")}}
internal fun ProtectedAccess.wolfEntry(level:Int):Boolean{
    if(!player.members || vars[WolfVars.quest]<9){mes("You must complete Creature of Fenkenstrain first.");return false}
    if(WolfItems.ring !in worn && WolfItems.unlocked !in worn){mes("The werewolves will not let you train here without the Ring of Charos.");return false}
    if(stat(stats.agility)<level){mes("You need level $level Agility for this course.");return false}
    return true
}

public class WerewolfCourse @Inject constructor(private val locs:LocRepository,private val ground:ObjRepository):PluginScript(){
    private data class Lap(var step:Int=0,var stick:Obj?=null,var picked:Boolean=false)
    private val laps=WeakHashMap<Player,Lap>()
    private val stones=listOf(CoordGrid(3538,9875),CoordGrid(3538,9877),CoordGrid(3540,9877),CoordGrid(3540,9879),CoordGrid(3540,9881))
    override fun ScriptContext.startup(){
        onOpNpc1(WolfActors.guard){if(wolfEntry(25))mes("You may enter through the trapdoor.")}
        onOpLoc1(WolfLocs.trap){if(wolfEntry(25))locs.change(it.loc,WolfLocs.open,30)}
        onOpLoc1(WolfLocs.open){if(wolfEntry(25))telejump(CoordGrid(3549,9865))}
        onOpLoc2(WolfLocs.open){locs.change(it.loc,WolfLocs.trap,1)}
        onOpLoc1(WolfLocs.ladder){clearLap(player);telejump(CoordGrid(3543,3463))}
        onOpNpc1(WolfActors.boss){if(wolfEntry(60))mes("Jump across the stones. Fetch the stick and give it to the trainer beyond the zip line.")}
        onOpLoc1(WolfLocs.stone){jumpStone(it.loc)}
        onApLoc1(WolfLocs.stone){if(isWithinApRange(it.loc,2))jumpStone(it.loc)}
        for(hurdle in WolfLocs.hurdles) onOpLoc1(hurdle){
            val lap=laps[player] ?: return@onOpLoc1
            val index=listOf(9893,9896,9899).indexOf(it.loc.z)
            if(index<0 || lap.step!=5+index || coords.z>=it.loc.z)return@onOpLoc1
            crossObstacle(CoordGrid(coords.x.coerceIn(3537,3542),it.loc.z+1),WolfMoves.hurdle,2)
            lap.step++;statAdvance(stats.agility,20.0)
        }
        onOpLoc1(WolfLocs.pipe){crawl(it.loc)}
        onApLoc1(WolfLocs.pipe){if(isWithinApRange(it.loc,1))crawl(it.loc)}
        onOpObj3(WolfItems.stick){
            val lap=laps[player] ?: return@onOpObj3
            if(lap.step!=9 || lap.picked || lap.stick!==it.obj || WolfItems.stick in inv)return@onOpObj3
            val take=invAdd(inv,WolfItems.stick,autoCommit=false)
            if(take.failure){mes("Make room in your inventory for the stick.");return@onOpObj3}
            if(ground.del(it.obj)){take.commitAll();lap.stick=null;lap.picked=true}
        }
        onOpLoc1(WolfLocs.climb){climbSlope(it.loc)}
        onApLoc1(WolfLocs.climb){if(isWithinApRange(it.loc,1))climbSlope(it.loc)}
        onOpLoc1(WolfLocs.slide){slide()}
        onApLoc1(WolfLocs.slide){if(isWithinApRange(it.loc,1))slide()}
        onOpNpc1(WolfActors.collector){finish()}
        onOpNpcU(WolfActors.collector,WolfItems.stick){finish()}
        onPlayerLogout { clearLap(player) }
        onPlayerLogin { if(WolfItems.stick in player.inv) player.invDel(player.inv,WolfItems.stick) }
    }
    private suspend fun ProtectedAccess.crawl(loc:BoundLocInfo){
            val lap=laps[player] ?: return
            if(lap.step!=8 || loc.z!=9905 || coords.z>9905)return
            crossObstacle(CoordGrid(loc.x,9910),WolfMoves.pipe,3)
            lap.step++;statAdvance(stats.agility,15.0)
    }
    private suspend fun ProtectedAccess.climbSlope(loc:BoundLocInfo){
            val lap=laps[player] ?: return
            if(lap.step!=9 || coords.x<3532)return
            crossObstacle(CoordGrid(3530,loc.z),WolfMoves.climb,3)
            lap.step++;statAdvance(stats.agility,25.0)
    }
    private suspend fun ProtectedAccess.slide(){
            val lap=laps[player] ?: return
            if(lap.step!=10)return
            if(worn[Wearpos.Hat.slot]!=null){mes("Remove your headgear before gripping the zip line.");return}
            anim(WolfMoves.bite);delay(1)
            val low=minOf(stat(stats.agility),stat(stats.strength))
            val kilograms=(player.runWeight/1000).coerceIn(0,64)
            val chance=(100-(80-low).coerceAtLeast(0)-(kilograms-2).coerceAtLeast(0)).coerceIn(20,100)
            val length=if(random.of(100)<chance)3 else random.of(3)
            crossObstacle(CoordGrid(3528,if(length==3)9873 else 9894-length*6),WolfMoves.slide,6)
            statAdvance(stats.agility,140.0+20*length)
            if(length<3){takeInstantHit(HitType.Typeless,random.of(30)+1);telejump(CoordGrid(3530,9873))}
            lap.step++
    }
    private suspend fun ProtectedAccess.jumpStone(loc:BoundLocInfo){
            val index=stones.indexOf(loc.coords)
            if(index<0)return
            if(index==0){
                if(!wolfEntry(60) || coords.z>9875)return
                clearLap(player)
                laps[player]=Lap()
            }
            val lap=laps[player] ?: return
            if(lap.step!=index)return
            crossObstacle(if(index==4) CoordGrid(3540,9883) else loc.coords,WolfMoves.jump,1)
            lap.step++;statAdvance(stats.agility,10.0)
            if(index==4){
                val tile=CoordGrid(listOf(3538,3541,3544)[random.of(3)],9910)
                lap.stick=ground.add(WolfItems.stick,tile,165,player,reveal=166)
                mes("The Agility Boss throws a stick to the far end of the course.")
            }
    }
    private fun ProtectedAccess.finish(){
        val lap=laps[player]
        if(lap?.step!=11 || !lap.picked || WolfItems.stick !in inv){mes("Bring me the stick from your run through the course.");return}
        if(invDel(inv,WolfItems.stick).failure)return
        recordLap(AgilityLap.Werewolf)
        statAdvance(stats.agility,380.0)
        laps.remove(player)
        mes("You hand the stick back to the trainer.")
    }
    private fun clearLap(player:Player){
        laps.remove(player)?.stick?.let(ground::del)
        if(WolfItems.stick in player.inv)player.invDel(player.inv,WolfItems.stick)
    }
}
