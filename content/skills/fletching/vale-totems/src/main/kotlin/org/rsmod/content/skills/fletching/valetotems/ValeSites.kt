package org.rsmod.content.skills.fletching.valetotems

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid

internal enum class ValeWood(
    val log: Int,
    val level: Int,
    val xp: Double,
    val decorXp: Double,
    val ornaments: Set<Int>,
    val perVisit: List<Int>,
) {
    Oak(1521, 20, 12.5, 51.5, setOf(843, 54, 845, 56, 9442, 22251), listOf(2, 4, 6, 8, 10)),
    Willow(1519, 35, 31.5, 126.0, setOf(849, 60, 847, 58, 9444, 22254), listOf(2, 4, 6, 8, 10)),
    Maple(1517, 50, 51.0, 201.0, setOf(853, 64, 851, 62, 9448, 22257), listOf(2, 4, 6, 8, 10)),
    Yew(1515, 65, 82.0, 326.0, setOf(857, 68, 855, 66, 9452, 22260), listOf(3, 5, 8, 10, 13)),
    Magic(1513, 80, 156.0, 620.0, setOf(861, 72, 859, 70, 21952, 22263), listOf(3, 6, 9, 12, 15)),
    Redwood(19669, 90, 222.0, 725.0, setOf(31049, 22266), listOf(3, 6, 9, 12, 15));

    val id
        get() = ordinal + 1
}

internal object ValeSites {
    val coords =
        listOf(
            CoordGrid(1370, 3375),
            CoordGrid(1346, 3319),
            CoordGrid(1385, 3274),
            CoordGrid(1413, 3286),
            CoordGrid(1438, 3305),
            CoordGrid(1477, 3332),
            CoordGrid(1453, 3341),
            CoordGrid(1398, 3329),
        )
    val spirits = listOf("Buffalo", "Jaguar", "Eagle", "Snake", "Scorpion")
}

internal data class SiteVars(
    val base: VarBitType,
    val carved: VarBitType,
    val tiers: List<VarBitType>,
    val decorations: VarBitType,
    val spirits: List<VarBitType>,
    val visible: VarBitType,
    val durability: VarBitType,
    val points: VarBitType,
    val trail: VarBitType,
)

internal object ValeBits : VarBitReferences() {
    val intro = find("ent_totems_intro")
    val sun = find("vmq1")
    val buff = find("ent_totems_trail_xp_buff_actions")
    val sites =
        (1..8).map { i ->
            val p = "ent_totems_site_${i}_"
            SiteVars(
                find(p + "base"),
                find(p + "base_carved"),
                listOf("low", "mid", "top").map { find(p + it) },
                find(p + "decorations"),
                (1..3).map { find(p + "animal_" + it) },
                find(p + "all_multianimals"),
                find(p + "decay"),
                find(p + "points"),
                find(p + "trail_buff_active"),
            )
        }
}

internal object ValeVars : VarpReferences() {
    val points = find("ent_totems_research_points")
}

internal object ValeLocs : LocReferences() {
    val layers =
        (1..8).map { i ->
            listOf("base", "low", "mid", "top").map { find("ent_totems_site_${i}_$it") }
        }
    val offerings = (1..8).map { find("ent_totems_site_${it}_offerings") }
    val admire = (1..3).map { find("ent_totems_admire_loc_$it") }
    val footprints = (0..1).map { find("ent_totems_trail_part_$it") }
}

internal object ValeNpcs : NpcReferences() {
    val ranulph = find("ent_totems_intro_ranulph")
    val isadora = find("ent_totems_intro_child")
    val ents = (1..8).map { find("ent_totems_ent_destination_$it") }
}

internal object ValeEntEditor : org.rsmod.api.type.editors.npc.NpcEditor() {
    init {
        for (ent in ValeNpcs.ents) edit(ent) {
            moveRestrict = org.rsmod.game.movement.MoveRestrict.PassThru
            defaultMode = org.rsmod.game.entity.npc.NpcMode.None
        }
    }
}

internal object ValeTimer : TimerReferences() {
    val walk = find("vale_ent_visits")
}

internal object ValeSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ValeTotems>("vale-totems.toml")
        resourceFile<ValeTotems>("custodian-stalkers.toml")
    }
}
