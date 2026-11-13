package org.rsmod.content.skills.hunter.implings

import jakarta.inject.Inject
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.BlockWalk
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.util.CompactableIntArray
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ImplingScript @Inject constructor(private val hunting:ImplingHunting,private val population:ImplingPopulation,private val elnock:ElnockInquisitor):PluginScript(){
    override fun ScriptContext.startup(){
        for(species in ImplingSpeciesList.all){
            for(type in species.wild+species.maze){onOpNpc1(type){hunting.catch(this,it.npc,species)};onAiTimer(type){population.expire(npc)}}
            onOpHeld3(species.jar){hunting.open(this,it.slot,species)}
        }
        for(type in ImplingNpcs.precursors)onAiTimer(type){population.roll(npc)}
        onOpNpc1(ImplingNpcs.elnock){elnock.talk(this)}
        onOpNpc2(ImplingNpcs.elnock){elnock.trade(this)}
        onOpNpc3(ImplingNpcs.elnock){elnock.exchange(this)}
        onOpNpc4(ImplingNpcs.elnock){elnock.withdraw(this)}
    }
}
internal object ImplingNpcEditor:NpcEditor(){init{
    for((index,npc)in ImplingNpcs.maze.withIndex())edit(npc){defaultMode=NpcMode.Wander;moveRestrict=MoveRestrict.BlockedNormal;blockWalk=BlockWalk.None;wanderRange=100;respawnRate=if(index in 3..4)50 else 7;timer=3000}
    for(npc in ImplingNpcs.wild+ImplingNpcs.crystal)edit(npc){defaultMode=NpcMode.Wander;moveRestrict=MoveRestrict.BlockedNormal;blockWalk=BlockWalk.None;wanderRange=100;timer=3000}
    for(npc in ImplingNpcs.precursors)edit(npc){defaultMode=NpcMode.Wander;moveRestrict=MoveRestrict.BlockedNormal;blockWalk=BlockWalk.None;wanderRange=100;timer=200;multiVarBit=0;multiVarp=0;multiNpcDefault=-1;multiNpc=CompactableIntArray(intArrayOf(-1))}
}}
internal object ImplingSpawns:MapNpcSpawnBuilder(){override fun onPackMapTask(){resourceFile<ImplingScript>("impling-spawns.toml")}}
