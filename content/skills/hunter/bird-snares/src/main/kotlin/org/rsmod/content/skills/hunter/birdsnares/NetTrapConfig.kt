package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.hunt.HuntModeBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.hunt.HuntModeReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object NetTrapLocs : LocReferences() {
    val trees = listOf("swamp", "orange", "red", "black", "mountain").map { find("hunting_sapling_up_$it") }
    val open = listOf("swamp", "orange", "red", "black", "mountain").map { find("hunting_sapling_set_$it") }
    val setting = listOf("swamp", "orange", "red", "black", "mountain").map { find("hunting_sapling_setting_$it") }
    val catching = listOf("green", "orange", "red", "black", "mountain").map { find("hunting_sapling_catching_$it") }
    val full = listOf("green", "orange", "red", "black", "mountain").map { find("hunting_sapling_full_$it") }
    val failing = listOf("swamp", "orange", "red", "black", "mountain").map { find("hunting_sapling_failing_$it") }
    val broken = listOf("swamp", "orange", "red", "black", "mountain").map { find("hunting_sapling_failed_$it") }
}
public object NetTrapNpcs : NpcReferences() {
    val all = listOf("green", "orange", "red", "black", "mountain").map { find("salamander_$it") }
}
public object NetTrapObjs : ObjReferences() {
    val rope = find("rope")
    val net = find("net")
    val products = listOf("green_salamander", "orange_salamander", "red_salamander", "black_salamander", "immature_mountain_salamander").map(::find)
    val grownTecu = find("mountain_salamander")
    val bait = listOf("green", "orange", "red", "black", "mountain").map { find("salamander_tar_$it") }
}
public object NetTrapHunts : HuntModeReferences() {
    val all = listOf("swamp", "orange", "red", "black", "mountain").map { find("net_trap_$it") }
}
public object NetTrapController : ControllerBuilder() { val trap = build("net_trap") }
internal object NetTrapVars : VarConBuilder() {
    val owner = build("net_trap_owner")
    val phase = build("net_trap_phase")
    val species = build("net_trap_species")
    val smoked = build("net_trap_smoked")
    val baited = build("net_trap_baited")
}
public data class NetTrapSpecies(val index: Int, val level: Int, val xp: Double, val low: Int, val high: Int) {
    val npc: NpcType get() = NetTrapNpcs.all[index]
    val product: ObjType get() = NetTrapObjs.products[index]
    val tree: LocType get() = NetTrapLocs.trees[index]
    val open: LocType get() = NetTrapLocs.open[index]
    val full: LocType get() = NetTrapLocs.full[index]
    val broken: LocType get() = NetTrapLocs.broken[index]
}
/** Net trap oldid14975913, published curves and exact level/XP table. */
public object NetTrapSpeciesList {
    val all = listOf(NetTrapSpecies(0,29,152.0,52,360), NetTrapSpecies(1,47,224.0,16,288), NetTrapSpecies(2,59,272.0,0,240), NetTrapSpecies(3,67,319.2,0,212), NetTrapSpecies(4,79,344.0,1,212))
}
internal object NetTrapHuntBuilder : HuntModeBuilder() {
    init {
        for ((index, name) in listOf("swamp", "orange", "red", "black", "mountain").withIndex()) build("net_trap_$name") {
            type = HuntType.Scenery
            checkLoc { loc = NetTrapLocs.open[index] }
            checkVis = HuntVis.LineOfSight
            findKeepHunting = true
            rate = 3
            findNewMode = NpcMode.OpLoc1
        }
    }
}
internal object NetTrapNpcEditor : NpcEditor() {
    init {
        for ((index, npc) in NetTrapNpcs.all.withIndex()) edit(npc) {
            defaultMode = NpcMode.Wander
            moveRestrict = MoveRestrict.Normal
            huntMode = NetTrapHunts.all[index]
            huntRange = 2
        }
    }
}
internal object NetTrapSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<NetTrapScript>("net-trap-creatures.toml") }
}
