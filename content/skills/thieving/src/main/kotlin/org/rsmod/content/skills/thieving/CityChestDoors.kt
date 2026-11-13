package org.rsmod.content.skills.thieving

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.minigames.hasRossContract
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal data class ChestDoor(val type:LocType,val level:Int,val xp:Double,val insideOnLoc:Boolean=false,val pick:Boolean=false)
internal object ChestDoors : LocReferences() {
    val ross=find("con_contract_ardy_west_door_locked")
    val all = listOf(ChestDoor(find("picklock1"),1,3.8),ChestDoor(find("picklock2"),16,15.0),
        ChestDoor(ross,13,15.0),ChestDoor(find("picklock4"),46,37.5),
        ChestDoor(find("picklock5"),61,50.0,true),ChestDoor(find("toollock2"),23,22.5,false,true),
        ChestDoor(find("toollock3"),39,35.0,true,true))
}

/** A door has two approach squares. A successful entry awards XP, returning is free. */
public class CityChestDoors:PluginScript() {
    override fun ScriptContext.startup() {
        for(door in ChestDoors.all) {
            onOpLoc1(door.type) { through(it.loc,door,false) }
            onOpLoc2(door.type) { through(it.loc,door,true) }
            if(door.pick) onOpLocU(door.type,SpecialTheftObjs.lockpick) { through(it.loc,door,true) }
        }
    }
    private fun ProtectedAccess.through(loc:BoundLocInfo,door:ChestDoor,pick:Boolean) {
        if(!player.members || actionDelay > player.currentMapClock) return
        val wall=loc.coords
        val adjacent = when(loc.angleId) {
            0 -> wall.translate(-1,0)
            1 -> wall.translate(0,1)
            2 -> wall.translate(1,0)
            else -> wall.translate(0,-1)
        }
        val onWallSide = when(loc.angleId) {
            0 -> coords.x>=wall.x
            1 -> coords.z<=wall.z
            2 -> coords.x<=wall.x
            else -> coords.z>=wall.z
        }
        val entering = onWallSide != door.insideOnLoc
        if (entering && !(door.type == ChestDoors.ross && player.hasRossContract())) {
            if(!pick) { mes("This door is locked."); return }
            if(player.thievingLvl < door.level) { mes("You need level ${door.level} Thieving to pick this lock."); return }
            if(door.pick && SpecialTheftObjs.lockpick !in inv) { mes("You need a lockpick for this lock."); return }
            actionDelay = player.currentMapClock + 2
            if(door.type == ChestDoors.ross && random.of(100) < (88 - player.thievingLvl * 87 / 100)) queueHit(1,HitType.Typeless,player.hitpoints/10)
            if(!statRandom(stats.thieving,128,250,0)) { mes("You fail to pick the lock."); return }
            statAdvance(stats.thieving,door.xp)
        }
        // Keep the collision wall closed to other players: each entrant must pick their own lock.
        telejump(if(onWallSide) adjacent else wall)
    }
}
