package org.rsmod.content.minigames.pestcontrol

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid

internal enum class Lander(
    val requirement: Int,
    val score: Int,
    val dock: CoordGrid,
    val deck: CoordGrid,
    val diary: DiaryTask,
) {
    Novice(40, 3, CoordGrid(2657, 2639), CoordGrid(2661, 2639), DiaryTask.NovicePestControl),
    Intermediate(
        70,
        4,
        CoordGrid(2644, 2644),
        CoordGrid(2640, 2644),
        DiaryTask.IntermediatePestControl,
    ),
    Veteran(100, 5, CoordGrid(2638, 2653), CoordGrid(2634, 2653), DiaryTask.VeteranPestControl),
}

internal object PestLocs : LocReferences() {
    val gangplanks = listOf("", "_2", "_3").map { find("pest_lander_gangplank$it") }
    val ladders = listOf("", "_2", "_3").map { find("pest_lander_ladder$it") }
    val gates = listOf(find("pest_wall_gate_fixed"), find("pest_wall_gate_fixed_mirror"))
    val tower = find("pest_watchtower_ladder")
    val towerTop = find("pest_watchtower_ladder_top")
}

internal object PestNpcs : NpcReferences() {
    val knights = (1..4).map { find("pest_voidknight_$it") }
    val defenders = (1..4).map { find("pest_voidknight_control$it") }
    val port = find("pest_squire_ship_portsarim")
    val outpost = find("pest_squire_ship_island")
    val portal =
        Lander.entries.associateWith { tier ->
            (1..4).map {
                find("pest_portal_${it}_active" + if (tier == Lander.Novice) "_bt1" else "")
            }
        }
    val shield =
        Lander.entries.associateWith { tier ->
            (1..4).map {
                find("pest_portal_shield_${it}_active" + if (tier == Lander.Novice) "_bt1" else "")
            }
        }
    val pests =
        (1..5).map { lvl ->
            listOf(
                    "shifter_${lvl}a",
                    "defiler_${lvl}a",
                    "torcher_${lvl}a",
                    "spinner_$lvl",
                    "ravager_$lvl",
                    "brawler_$lvl",
                    "splatter_$lvl",
                )
                .map { find("pest_$it") }
        }
}

internal object PestTypes : NpcEditor() {
    init {
        for (land in listOf(Lander.Novice, Lander.Intermediate)) for (type in
            (PestNpcs.portal.getValue(land) + PestNpcs.shield.getValue(land))) edit(type) {
            hitpoints = if (land == Lander.Novice) 200 else 250
            defence = 120
        }
        for (type in PestNpcs.defenders) edit(type) { hitpoints = 200 }
    }
}

internal object PestTimers : TimerReferences() {
    val game = find("pest_control_tick")
}

internal object PestVars : VarpReferences() {
    val points = find("pest_commendations")
    val returnBoat = find("pest_boat_return")
}

internal object PestVarBuilder : VarpBuilder() {
    init {
        for (name in listOf("pest_commendations", "pest_boat_return")) build(name) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object PestBits : VarBitReferences() {
    val activity = find("pest_activity_counter")
    val ca = listOf("easy", "medium", "hard").map { find("ca_tier_status_$it") }
}

internal object PestHud : InterfaceReferences() {
    val boat = find("pest_lander_overlay")
    val game = find("pest_status_overlay")
}

internal object PestText : ComponentReferences() {
    val waiting = find("pest_lander_overlay:pest_lander_over_pl")
    val boatTime = find("pest_lander_overlay:pest_lander_over_time")
    val points = find("pest_lander_overlay:pest_lander_over_points")
    val level = find("pest_lander_overlay:pest_lander_over_level")
    val health = find("pest_status_overlay:pest_status_over_health")
    val time = find("pest_status_overlay:pest_status_over_time")
    val damage = find("pest_status_overlay:pest_status_over_dam")
    val portal = (1..4).map { find("pest_status_overlay:pest_status_porttxt$it") }
    val shield =
        listOf("purple", "blue", "yellow", "red").map { find("pest_status_overlay:${it}_shield") }
}

internal object PestStock : ObjReferences() {
    val gear =
        listOf(
                "pest_void_knight_top",
                "pest_void_knight_robes",
                "pest_void_knight_gloves",
                "pest_void_knight_mace",
                "game_pest_melee_helm",
                "game_pest_archer_helm",
                "game_pest_mage_helm",
                "pest_seal_8",
            )
            .map { find(it) }
}

internal object PestOutpostPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<PestControl>("outpost.toml")
    }
}
