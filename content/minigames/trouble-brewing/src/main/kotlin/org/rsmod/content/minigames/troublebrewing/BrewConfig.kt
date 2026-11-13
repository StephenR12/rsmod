package org.rsmod.content.minigames.troublebrewing

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object BrewLocs : LocReferences() {
    val door = find("brew_waiting_room_door")
    val portal = find("brew_exit_portal")
    val tools = find("brew_parts_table_full")
    val pump = find("brew_water_pump")
    val kettle = find("brew_flower_kettle")
    val hopper = listOf(find("brew_hopper_red"), find("brew_hopper_blue"))
    val flower = listOf(find("brew_blue_flowers"), find("brew_red_flowers"))
    val logstore = find("brew_log_store")
    val boilers =
        listOf(
            find("brew_still_boiler"),
            find("brew_still_boiler_mirror"),
            find("brew_still_boiler_corner"),
            find("brew_still_boiler_corner_mirror"),
        )
    val logboilers =
        listOf(
            find("brew_still_boiler_logs"),
            find("brew_still_boiler_logs_mirror"),
            find("brew_still_boiler_corner_logs"),
            find("brew_still_boiler_corner_logs_mirror"),
        )
    val fires =
        listOf(
            find("brew_still_boiler_fire"),
            find("brew_still_boiler_fire_mirror"),
            find("brew_still_boiler_corner_fire"),
            find("brew_still_boiler_corner_fire_mirror"),
        )
    val jungle =
        listOf(find("brew_jungle_tree_1"), find("brew_jungle_tree_2"), find("brew_jungle_tree_3"))
    val scrapey = find("brew_scrapey_tree")
    val mound = find("brew_sweetgrub_mound")
    val nut = find("brew_bitternut_tree")
    val conveyor = find("brew_conveyer_belt")
    val bottle = find("brew_conveyer_belt_bottle")
    val crates = listOf(find("brew_crate_blue"), find("brew_crate_red"))
    val stairs =
        listOf(
            find("brew_copper_ladder_1"),
            find("brew_copper_ladder_1_reverse"),
            find("brew_copper_ladder_top"),
            find("brew_copper_ladder_top_reverse"),
        )
    val steam = find("brew_steam_pump")
}

internal object BrewNpcs : NpcReferences() {
    val crew = listOf(find("san_fan"), find("fancy_dan"))
    val jimmy = find("honest_jimmy")
    val monkey = find("brew_monkey")
}

internal object BrewObjs : ObjReferences() {
    val hats = listOf(find("brew_blue_pirate_hat"), find("brew_red_pirate_hat"))
    val flowers = listOf(find("brew_blue_flower"), find("brew_red_flower"))
    val colours = listOf(find("brew_bowl_blue"), find("brew_bowl_red"))
    val nuts = find("brew_bitternut")
    val bark = find("brew_scrapey_bark")
    val grubs = find("brew_sweetgrubs")
    val wood = find("brew_scrapey_logs")
    val monkey = find("brew_monkey")
    val monkeys = listOf(find("brew_blue_monkey"), find("brew_red_monkey"))
    val rum = listOf(find("brew_blue_rum"), find("brew_red_rum"))
    val pieces = find("brew_piece_of_eight")
    val yeast = find("brew_hyper_yeast")
    val clothing =
        listOf("blue", "green", "red", "brown", "black", "purple", "grey").flatMap { c ->
            listOf(find("brew_uniform_$c"), find("brew_tricorn_$c"), find("brew_navy_slacks_$c"))
        }
    val flags = (1..6).map { find("brew_flag_$it") }
}

internal object BrewVars : VarpReferences() {
    val pieces = find("brew_pieces")
    val quest = find("fever_quest")
    val crew = find("brewing_active_crew")
}

internal object BrewBits : VarBitReferences() {
    val follower = find("follower_space_track")
    val score = find("brew_player_reward")
    val bottles = listOf(find("brew_san_bottle_1"), find("brew_dan_bottle_1"))
}

internal object BrewSave : VarpBuilder() {
    init {
        build("brewing_active_crew") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object BrewTimer : TimerReferences() {
    val tick = find("brewing_round_timer")
}

internal object BrewUI : InterfaceReferences() {
    val wait = find("brew_waiting_room_overlay")
    val game = find("brew_overlay")
}

internal object BrewHud : ComponentReferences() {
    val wait = find("brew_waiting_room_overlay:time_text")
    val time = find("brew_overlay:brew_time_display")
    val scores = listOf(find("brew_overlay:blue_score"), find("brew_overlay:red_score"))
}

internal object BrewPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TroubleBrewing>("brewers.toml")
    }
}
