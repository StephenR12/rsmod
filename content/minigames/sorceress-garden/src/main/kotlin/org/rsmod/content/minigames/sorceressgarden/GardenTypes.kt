package org.rsmod.content.minigames.sorceressgarden

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.map.CoordGrid

internal enum class Garden(
    val level: Int,
    val gate: CoordGrid,
    val inside: CoordGrid,
    val eject: CoordGrid,
    val fruitCount: Int,
    val juiceXp: Int,
    val weights: List<Int>,
) {
    Winter(
        1,
        CoordGrid(2903, 5470),
        CoordGrid(2902, 5470),
        CoordGrid(2907, 5470),
        5,
        350,
        listOf(16, 14, 13, 11),
    ),
    Spring(
        25,
        CoordGrid(2920, 5473),
        CoordGrid(2921, 5473),
        CoordGrid(2916, 5473),
        4,
        1350,
        listOf(16, 14, 13, 12, 11, 9),
    ),
    Autumn(
        45,
        CoordGrid(2913, 5463),
        CoordGrid(2913, 5462),
        CoordGrid(2913, 5467),
        3,
        2350,
        listOf(16, 14, 13, 12, 11, 10, 9, 8, 6),
    ),
    Summer(
        65,
        CoordGrid(2910, 5480),
        CoordGrid(2910, 5481),
        CoordGrid(2910, 5476),
        2,
        3000,
        listOf(16, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5),
    );

    fun contains(c: CoordGrid): Boolean =
        c.level == 0 &&
            when (this) {
                Winter -> c.x in 2886..2902 && c.z in 5465..5487
                Spring -> c.x in 2921..2937 && c.z in 5457..5479
                Autumn -> c.x in 2896..2919 && c.z in 5446..5462
                Summer -> c.x in 2904..2927 && c.z in 5481..5497
            }
}

internal object GardenNpcs : NpcReferences() {
    val apprentice = find("arabian_sorceress_apprentice")
    val contacts =
        listOf(find("contact_osman_multi"), find("osman"), find("bcs_selim"), find("bcs_selim_vis"))
    val guards =
        Garden.entries.flatMap { g ->
            (1..if (g == Garden.Spring) 8 else 6).map {
                find("100_osman_${g.name.lowercase()}_elemental_$it")
            }
        }
}

internal object GardenLocs : LocReferences() {
    val gates = Garden.entries.map { find("100_osman_door_${it.name.lowercase()}") }
    val trees = Garden.entries.map { find("100_osman_pomy_tree_${it.name.lowercase()}") }
    val patches = Garden.entries.map { find("osman_herb_patch_${it.name.lowercase()}") }
    val fountain = find("100_osman_fountain")
    val shelves = find("sorceress_shelves")
}

internal object GardenObjs : ObjReferences() {
    val fruits = Garden.entries.map { find("osman_squirk_${it.name.lowercase()}") }
    val juices = Garden.entries.map { find("osman_squirk_j_${it.name.lowercase()}") }
    val herbs =
        listOf(
            objs.guam_leaf,
            objs.marrentill,
            objs.tarromin,
            objs.harralander,
            objs.irit_leaf,
            objs.avantoe,
            objs.ranarr_weed,
            objs.kwuarm,
            objs.cadantine,
            objs.lantadyme,
            objs.dwarf_weed,
        )
}

internal object GardenBits : VarBitReferences() {
    val contact = find("contact")
    val unclaimedKeris = find("contact_never_had_keris")
    val introduction = find("osman_want_pomes")
    val apprentice = find("osman_apprentice")
    val sands = find("bcs")
}

internal object GardenVars : VarpReferences() {
    val prince = find("princequest")
}

internal object GardenSeqs : SeqReferences() {
    val point = find("100_osman_seasons_elemental_point")
    val drink = find("100_osman_fountain_drink")
}

internal object GardenPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SorceressGarden>("garden-npcs.toml")
    }
}

internal object GardenGuards : NpcEditor() {
    init {
        for (type in GardenNpcs.guards) edit(type) {
            defaultMode = NpcMode.None
            timer = 1
        }
    }
}
