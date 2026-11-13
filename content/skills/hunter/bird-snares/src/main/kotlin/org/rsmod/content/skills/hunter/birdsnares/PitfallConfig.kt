package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object PitfallLocs : LocReferences() {
    val empty = find("hunting_pitfall_invis_empty")
    val sources = (1..25).map { find("hunting_pitfall_$it") }
    val set = find("hunting_pitfall_invis_set")
    val catching = find("hunting_pitfall_invis_catching")
    val completed = find("hunter_pitfall_full")
    val full = listOf("larupia","graahk","kyatt","antelope_sunlight","antelope_moonlight").map { find("hunter_pitfall_full_$it") }
}
public object PitfallNpcs : NpcReferences() {
    val all = listOf("hunting_jaguar","hunting_leopard","hunting_snow_tiger","sunlight_antelope","moonlight_antelope").map(::find)
}
public object PitfallObjs : ObjReferences() {
    val stick = find("hunting_teasing_stick")
    val spear = find("hg_hunter_spear")
    val meats = listOf("hunting_larupia_meat","hunting_graahk_meat","hunting_kyatt_meat","hunting_antelopesun_meat","hunting_antelopemoon_meat").map(::find)
    val furs = listOf("hunting_fur_jaguar_perfect","hunting_fur_leopard_perfect","hunting_fur_tiger_perfect","hunting_antelopesun_fur","hunting_antelopemoon_fur").map(::find)
    val tatty = listOf("hunting_fur_jaguar_shabby","hunting_fur_leopard_shabby","hunting_fur_tiger_shabby").map(::find)
    val antlers = listOf(find("sunlight_antler"),find("moonlight_antler"))
    val splinters = find("sunfiresplinter")
}
public object PitfallSeqs : SeqReferences() {
    val tease = find("hunting_teasing_animal")
    val collapse = find("hunting_pitfall_collapse")
}
public object PitfallTimers : TimerReferences() { val lure = find("pitfall_lure_lifecycle") }
public object PitfallController : ControllerBuilder() { val trap = build("pitfall_trap") }
internal object PitfallVars : VarConBuilder() { val owner = build("pitfall_owner") }
public data class PitfallSpecies(val index:Int,val level:Int,val xp:Double,val maxHit:Int) {
    val npc:NpcType get()=PitfallNpcs.all[index]
    val meat:ObjType get()=PitfallObjs.meats[index]
    val fur:ObjType get()=PitfallObjs.furs[index]
    public fun inArea(c:CoordGrid):Boolean = c.level==0 && when(index) {
        0 -> c.x in 2530..2590 && c.z in 2870..2930
        1 -> c.x in 2750..2800 && c.z in 2980..3030
        2 -> c.x in 2680..2760 && c.z in 3760..3820
        3 -> c.x in 1720..1770 && c.z in 2980..3030
        else -> c.x in 1535..1585 && c.z in 9390..9440
    }
}
public object PitfallSpeciesList {
    val all = listOf(PitfallSpecies(0,31,180.0,5),PitfallSpecies(1,41,240.0,7),PitfallSpecies(2,55,300.0,7),PitfallSpecies(3,72,380.0,5),PitfallSpecies(4,91,450.0,5))
    val logs = listOf(objs.logs,objs.achey_tree_logs,objs.oak_logs,objs.willow_logs,objs.teak_logs,objs.maple_logs,objs.mahogany_logs,objs.yew_logs,objs.magic_logs)
}
internal object PitfallSpawns : MapNpcSpawnBuilder() { override fun onPackMapTask() { resourceFile<PitfallScript>("pitfall-creatures.toml") } }

internal object PitfallNpcEditor : NpcEditor() {
    init {
        for((index,npc) in PitfallNpcs.all.withIndex()) edit(npc) {
            defaultMode=NpcMode.Wander
            moveRestrict=MoveRestrict.Normal
            if(index>=3) respawnRate=45
        }
    }
}
