package org.rsmod.content.bosses.wintertodt

import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid

public object WinterLocs : LocReferences() {
    val regularTree = find("tree")
    val door = find("wint_door")
    val roots = find("wint_roots")
    val herb = find("wint_herb_roots")
    val unlit = find("wint_brazier")
    val broken = find("wint_brazier_broken")
    val lit = find("wint_brazier_lit")
    val hammer = find("wint_chest_hammer")
    val knife = find("wint_chest_knife")
    val axe = find("wint_chest_axe")
    val tinderbox = find("wint_chest_tinderbox")
    val concoction = find("wint_chest_vial")
    val bank = find("wint_bankchest")
    val cart = find("wint_reward_pile")
    val carts = listOf("empty", "1", "2", "3", "4", "full").map { find("wint_reward_pile_$it") }
    val warning = find("wint_area_attack_icicle")
    val snow = find("wint_area_attack_snow")
}

public object WinterObjs : ObjReferences() {
    val root = find("wint_bruma_root")
    val kindling = find("wint_bruma_kindling")
    val herb = find("wint_herb")
    val unf = find("wint_vial")
    val potions = (1..4).map { find("wint_potion$it") }
    val torch = find("wint_torch")
    val offhand = find("wint_torch_offhand")
    val fletchingKnife = find("fletching_knife")
    val phoenix = find("phoenixpet")
    val tome = find("tome_of_fire_uncharged")
    val gloves = find("pyromancer_gloves")
    val pages = find("wint_burnt_page")
    val extra = find("wint_reroll_box")
    val pyro = listOf("top", "hood", "bottom", "boots").map { find("pyromancer_$it") }
    val magicSeed = find("magic_tree_seed")
    val torstolSeed = find("torstol_seed")
    val dynamite = find("lovakengj_dynamite_fused")
    val saltpetre = find("hosidius_saltpetre")
}

public object WinterNpcs : NpcReferences() {
    val pyro = find("wint_wizard")
    val down = find("wint_wizard_down")
    val ignisia = find("wint_master_pyromancer")
    val brewma = find("wint_brewma")
}

public object WinterSeqs : SeqReferences() {
    val feed = find("human_pickuptable")
    val light = find("wint_light_brazier_bruma")
    val smith = find("human_smithing")
    val fletch = find("human_fletching")
    val mix = find("human_herbing_vial")
    val eat = find("human_eat")
}

public object WinterBits : VarBitReferences() {
    val warmth = find("wint_warmth")
    val rewards = find("wint_reward_pool")
    val respawn = find("wint_transmit_respawndelay")
}

public object WinterVars : VarpReferences() {
    val round = find("wintertodt_player_round")
    val points = find("wintertodt_player_points")
    val session = find("wintertodt_player_session")
    val lastCold = find("wintertodt_last_cold_tick")
    val kills = find("total_wintertodt_kills")
}

public object WinterTimers : TimerReferences() {
    val warmth = find("wintertodt_warmth_lifecycle")
}

public object WinterAreas : AreaReferences() {
    val prison = find("wintertodt_prison")
}

public object WinterInterfaces : InterfaceReferences() {
    val hud = find("wint_status")
}

public object WinterComponents : ComponentReferences() {
    val points = find("wint_status:points")
    val status = find("wint_status:status")
    val warmth = find("wint_status:warmth")
    val energy = find("wint_status:energy")
    val pyros = (1..4).map { find("wint_status:pyro$it") }
    val braziers = (1..4).map { find("wint_status:brazier$it") }
}

public object WinterControllers : ControllerBuilder() {
    val round = build("wintertodt_round")
}

internal object WinterVarBuilds : VarpBuilder() {
    init {
        for (name in
            listOf(
                "wintertodt_player_round",
                "wintertodt_player_points",
                "wintertodt_player_session",
                "wintertodt_last_cold_tick",
            )) build(name) { transmitNever = true }
    }
}

internal object WinterAreaBuilds : AreaBuilder() {
    init {
        build("wintertodt_prison")
    }
}

internal object WinterAreaMap : MapAreaBuilder() {
    override fun onPackMapTask() {
        area(WinterAreas.prison) {
            polygon {
                for ((x, z) in
                    listOf(1600 to 3968, 1663 to 3968, 1663 to 4031, 1600 to 4031)) vertex(
                    VertexCoord.from(CoordGrid(x, z))
                )
            }
        }
    }
}

internal object WinterNpcEdits : NpcEditor() {
    init {
        for (type in
            listOf(WinterNpcs.pyro, WinterNpcs.down, WinterNpcs.brewma, WinterNpcs.ignisia)) edit(
            type
        ) {
            wanderRange = 0
        }
    }
}
