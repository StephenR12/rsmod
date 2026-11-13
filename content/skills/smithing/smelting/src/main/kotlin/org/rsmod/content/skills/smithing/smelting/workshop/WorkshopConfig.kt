package org.rsmod.content.skills.smithing.smelting.workshop

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.movement.BlockWalk

public object WorkshopLocs : LocReferences() {
    val walls = listOf(find("elemental_workshop_oddwall_r"), find("elemental_workshop_oddwall_l"))
    val firstDown = find("elemental_workshop_spiralstairstop")
    val firstUp = find("elemental_workshop_spiralstairs")
    val book = find("elemental_workshop_bookcase")
    val beatenBook = find("elemental_workshop_2_bookcase")
    val workbench = find("elemental_workshop_workbench")
    val furnaces =
        listOf(
            find("elemental_workshop_furnace"),
            find("elemental_workshop_furnace_out"),
            find("elemental_workshop_furnace_lit"),
        )
    val hatch = find("elem2_stairs_door")
    val closedHatch = find("elem2_stairs_door_close")
    val openHatch = find("elem2_stairs_door_open")
    val firstReturn = find("elem2_stairs1")
    val lower = find("elem2_stairs_door_open_no_hatch")
    val upper = find("elem2_stairs2")
    val gantryUp = find("elem_gantry_stairs")
    val gantryDown = find("elem_gantry_stairs_top")
    val gantryReturn = find("elem2_stairs3")
    val move = find("elem2_lever_3way")
    val rotate = find("elem2_fire_lever_1")
    val lift = find("elem2_fire_lever_2")
    val press = find("elem2_earth_lever_1")
    val west = find("elem2_valve_1")
    val east = find("elem2_valve_2")
    val waterDoor = find("elem2_water_lever")
    val screw = find("elem2_corkscrew")
    val fan = find("elem2_air_lever")
    val doors =
        listOf(
            find("elem2_door_mind"),
            find("elem2_door_mindl"),
            find("elem2_door_mind_open"),
            find("elem2_door_mindl_open"),
        )
    val gun = find("elem_extractor_gun")
    val guns =
        listOf(
            gun,
            find("elem_extractor_gun_no_bar"),
            find("elem_extractor_gun_bar"),
            find("elem_extractor_gun_bar_mind"),
        )
    val hat = find("elem_extractor_hat")
    val controls = listOf(move, rotate, lift, press, west, east, waterDoor, screw, fan)
}

public object WorkshopObjs : ObjReferences() {
    val battered = find("elemental_workshop_shield_book")
    val slashed = find("elemental_workshop_shield_book_slashed")
    val beaten = find("elemental_workshop_helm_book")
    val key = find("elemental_workshop_key")
    val ore = find("elemental_workshop_ore")
    val bar = find("elemental_workshop_bar")
    val primed = find("elem_primed_bar")
    val mind = find("elem_mind_bar")
    val shield = find("elemental_shield")
    val helmet = find("elem_elemental_helm")
    val mindShield = find("elemental_mind_shield")
    val mindHelmet = find("elem_mind_helm")
    val catalyst = find("smithing_catalyst")
    val outputs = listOf(shield, helmet, mindShield, mindHelmet)
}

public object WorkshopBits : VarBitReferences() {
    val learned = find("elemental_workshop_book")
    val finished = find("elemental_workshop_finished")
    val furnace = find("elemental_workshop_fire")
    val quest = find("elemental_quest_2_main")
    val hatch = find("elemental_quest_2_hatch")
    val position = find("elemental_quest_2_jig_pos")
    val bar = find("elemental_quest_2_jig_state")
    val crane = find("elemental_quest_2_fire_state")
    val clawPosition = find("elemental_quest_2_fire_pos")
    val pressRepaired = find("elemental_quest_2_earth_fixed")
    val pipeRepaired = find("elemental_quest_2_water_state")
    val inlet = find("elemental_quest_2_water_valve_1")
    val drain = find("elemental_quest_2_water_valve_2")
    val door = find("elemental_quest_2_water_door")
    val water = find("elemental_quest_2_water_level")
    val smallCog = find("elemental_quest_2_air_cog1")
    val mediumCog = find("elemental_quest_2_air_cog2")
    val largeCog = find("elemental_quest_2_air_cog3")
    val fan = find("elemental_quest_2_air_fan_state")
    val mind = find("elemental_quest_2_mind_jig")
}

public object WorkshopCarts : NpcReferences() {
    val stages =
        listOf(
                "elem2_cart_npc_empty",
                "elem2_cart_npc_bar",
                "elem2_cart_npc_bar_hot",
                "elem2_cart_npc_flat_hot_bar",
                "elem2_cart_npc_flat_bar",
                "elem2_cart_npc_flat_bar_dry",
            )
            .map(::find)
}

internal object WorkshopCartEditor : NpcEditor() {
    init {
        for (cart in WorkshopCarts.stages) edit(cart) {
            moveRestrict = nomove
            wanderRange = 0
            blockWalk = BlockWalk.None
        }
    }
}
