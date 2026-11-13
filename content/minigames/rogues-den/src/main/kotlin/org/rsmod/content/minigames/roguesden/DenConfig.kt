package org.rsmod.content.minigames.roguesden

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object DenLocs : LocReferences() {
    val entrance = find("roguesden_mazeentrance")
    val safe = find("roguesden_walldecor_mazesafe")
    val passage = find("roguesden_passage_across")
    val up = find("roguesden_passage_up")
    val down = find("roguesden_passage_down")
    val gallery = find("roguesden_gallery_door")
    val doors = find("roguesden_obstacle_blocking_door_enter")
    val lock = find("roguesden_obstacle_door")
    val mosaic = find("roguesden_puzzle_door_mosaic")
    val bars = find("roguesden_obstacle_contortion_bars")
    val ledge = find("roguesden_obstacle_wall_hanging_ledge")
    val rail = find("roguesden_obstacle_floor_balancing_ledge")
    val traps =
        listOf(
                "floor_spikes",
                "wall_spikes",
                "wall_poison_gas",
                "floor_explosion",
                "floor_springtile",
                "wall_crusher",
            )
            .map { find("roguesden_trap_$it") }
}

internal object DenNpcs : NpcReferences() {
    val brian = find("roguesden_mazemaster")
    val guards = (1..3).map { find("roguesden_guard$it") }
}

internal object DenObjs : ObjReferences() {
    val gem = find("roguesden_gem")
    val powder = find("roguesden_flash_powder")
    val tiles = (1..4).map { find("roguesden_puzzle_mosaic_tile$it") }
    val kit = find("roguesden_kit")
    val crate = find("roguesden_crate")
    val prizes =
        listOf("helm", "body", "legs", "gloves", "boots").map { find("roguesden_$it") } + kit
}

internal object DenInterfaces : InterfaceReferences() {
    val mosaic = find("roguesden_mosaic")
}

internal object DenComponents : ComponentReferences() {
    val tiles = (1..4).map { find("roguesden_mosaic:tile$it") }
}

internal object DenSeqs : SeqReferences() {
    val bars = find("roguesden_contortion")
    val disarm = find("roguesden_player_disarm_floor")
    val crack = find("rogue_player_crack_safe_end")
}

internal object DenSpots : SpotanimReferences() {
    val stun = find("roguesden_flash_powder")
}

internal object DenTimer : TimerReferences() {
    val maze = find("rogues_den_maze")
}

internal object DenBits : VarBitReferences() {
    val follower = find("follower_space_track")
}

internal object DenVars : VarpReferences() {
    val intro = find("rogues_den_introduction")
}

internal object DenSave : VarpBuilder() {
    init {
        build("rogues_den_introduction") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object DenPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<RoguesDen>("den-npcs.toml")
    }
}

internal object DenGround : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<RoguesDen>("den-objects.toml")
    }
}
