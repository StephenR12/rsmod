package org.rsmod.content.skills.hunter.falconry

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object FalconryNpcs : NpcReferences() {
    val matthias = find("hunting_npc_falconer")
    val matthiasNoBird = find("hunting_npc_falconer_nobird")
    val spotted = find("huntingbeast_speedy")
    val dark = find("huntingbeast_silent")
    val dashing = find("huntingbeast_speedy2")
    val onSpotted = find("hunting_falcon_onspeedy")
    val onDark = find("hunting_falcon_onsilent")
    val onDashing = find("hunting_falcon_onspeedy2")
}
public object FalconryObjs : ObjReferences() {
    val glove = find("falcon_gloves")
    val falcon = find("falcon_on_gloves")
}
public object FalconrySeqs : SeqReferences() { val launch = find("human_falconer_attack") }
public object FalconryVars : VarpReferences() { val invested = find("falconry_permanent_access") }
public object FalconryTimers : TimerReferences() { val lifecycle = find("falconry_lifecycle") }
internal object FalconryVarpBuilder : VarpBuilder() {
    init { build("falconry_permanent_access") { transmitNever = true } }
}
internal object FalconrySpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<FalconryScript>("falconry-npcs.toml") }
}

public data class FalconrySpecies(val npc: NpcType, val caught: NpcType, val fur: ObjType, val level: Int, val xp: Double, val low: Int, val high: Int, val meat: ObjType? = null) {
    public fun chance(level: Int): Double = SkillingSuccessRate.successRate(low, high, level.coerceIn(1, 99), 99).coerceIn(0.0, 1.0)
}
/** Dated Spotted/Dark/Dashing kebbit chart revisions 14967823/14797040/14799013. */
public object FalconrySpeciesList {
    val all = listOf(
        FalconrySpecies(FalconryNpcs.spotted, FalconryNpcs.onSpotted, objs.spotted_kebbit_fur, 43, 104.0, 26, 310),
        FalconrySpecies(FalconryNpcs.dark, FalconryNpcs.onDark, objs.dark_kebbit_fur, 57, 132.0, 0, 253),
        FalconrySpecies(FalconryNpcs.dashing, FalconryNpcs.onDashing, objs.dashing_kebbit_fur, 69, 156.0, 0, 205, objs.raw_dashing_kebbit),
    )
}
