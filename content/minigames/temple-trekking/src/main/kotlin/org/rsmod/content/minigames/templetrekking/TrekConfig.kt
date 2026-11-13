package org.rsmod.content.minigames.templetrekking

import org.rsmod.api.config.refs.params
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

internal object TrekNpcs : NpcReferences() {
    val escorts =
        listOf(
            find("templetrek_child_hard"),
            find("templetrek_oldman_hard"),
            find("templetrek_woman_med"),
            find("templetrek_man_med"),
            find("templetrek_retired_man_easy"),
            find("templetrek_retired_soldier_easy"),
            find("ramble_hard_ranger"),
            find("ramble_hard_fighter"),
            find("ramble_med_mage"),
            find("ramble_med_ranger"),
            find("ramble_easy_fighter"),
            find("ramble_easy_mage"),
        )
    val beasts =
        listOf(
            find("ttrek2_nail_beast_1"),
            find("ttrek2_nail_beast_2"),
            find("ttrek2_nail_beast_3"),
        )
    val snakes =
        listOf(find("templetrek_snake_1"), find("templetrek_snake_2"), find("templetrek_snake_3"))
    val carcasses =
        listOf(
            find("templetrek_snake_1_dead"),
            find("templetrek_snake_2_dead"),
            find("templetrek_snake_3_dead"),
        )
    val snails =
        listOf(
            find("templetrek_giantsnail_1"),
            find("templetrek_giantsnail_2"),
            find("templetrek_giantsnail_3"),
        )
    val lumber =
        listOf(
            find("ttrek2_zombie_diff_1_ver_1_1"),
            find("ttrek2_zombie_diff_4_ver_1_1"),
            find("ttrek2_zombie_diff_7_ver_1_2"),
        )
    val crank = find("templetrek_good_samaritan")
}

internal object TrekStats : NpcEditor() {
    init {
        edit(TrekNpcs.beasts[0]) {
            hitpoints = 55
            attack = 150
            strength = 6
            defence = 2
            magic = 1
            ranged = 1
            param[params.attackrate] = 8
        }
        edit(TrekNpcs.beasts[1]) {
            hitpoints = 65
            attack = 215
            strength = 16
            defence = 30
            magic = 1
            ranged = 1
            param[params.attackrate] = 8
        }
        edit(TrekNpcs.beasts[2]) {
            hitpoints = 75
            attack = 320
            strength = 26
            defence = 40
            magic = 1
            ranged = 1
            param[params.attackrate] = 8
        }
        edit(TrekNpcs.snakes[0]) {
            hitpoints = 120
            attack = 50
            strength = 50
            defence = 70
            magic = 30
            ranged = 30
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.snakes[1]) {
            hitpoints = 125
            attack = 70
            strength = 110
            defence = 80
            magic = 30
            ranged = 30
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.snakes[2]) {
            hitpoints = 130
            attack = 90
            strength = 170
            defence = 90
            magic = 30
            ranged = 30
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.snails[0]) {
            hitpoints = 125
            attack = 30
            strength = 30
            defence = 60
            magic = 30
            ranged = 70
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.snails[1]) {
            hitpoints = 150
            attack = 30
            strength = 30
            defence = 75
            magic = 30
            ranged = 110
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.snails[2]) {
            hitpoints = 160
            attack = 30
            strength = 30
            defence = 86
            magic = 30
            ranged = 160
            param[params.attackrate] = 4
        }
        edit(TrekNpcs.lumber[0]) {
            hitpoints = 10
            attack = 73
            strength = 6
            defence = 10
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.npc_com_xp_multiplier] = 0
        }
        edit(TrekNpcs.lumber[1]) {
            hitpoints = 13
            attack = 105
            strength = 16
            defence = 13
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.npc_com_xp_multiplier] = 0
        }
        edit(TrekNpcs.lumber[2]) {
            hitpoints = 16
            attack = 137
            strength = 26
            defence = 16
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.npc_com_xp_multiplier] = 0
        }
        for ((index, type) in TrekNpcs.escorts.withIndex()) edit(type) {
            hitpoints = listOf(15, 30, 50)[index % 6 / 2]
            attack = listOf(10, 30, 55)[index % 6 / 2]
            strength = attack
            defence = attack
        }
    }
}

internal object TrekScenery : LocReferences() {
    val evade =
        listOf(find("templetrek_swamp_evade_path"), find("templetrek_swamp_evade_path_brown"))
    val success =
        listOf(find("templetrek_swamp_success_path"), find("templetrek_swamp_success_path_brown"))
    val escape =
        listOf(find("templetrek_swamp_fail_path"), find("templetrek_swamp_fail_path_brown"))
    val bridge =
        listOf(
            find("templetrek_bridge_broken"),
            find("templetrek_bridge_fixed_1"),
            find("templetrek_bridge_fixed_2"),
            find("templetrek_bridge_fixed_3"),
        )
}

internal object TrekItems : ObjReferences() {
    val tokens =
        listOf("templetrek", "ramble").map { prefix ->
            listOf("blue", "yellow", "red").map { find("${prefix}_${it}_token_var") }
        }
    val tomes =
        listOf("fishing", "agility", "thieving", "slayer", "mining", "firemaking", "woodcutting")
            .map { skill -> (1..3).map { find("templetrek_tome_${skill}_level_$it") } }
    val nails = find("nail_beast_nail")
    val shell = find("templetrek_snail_shell")
    val perfect = find("dorgesh_snail_shell")
    val hide = find("templetrek_swamp_snake_hide")
}

internal object TrekQuest : VarBitReferences() {
    val aid = find("myreque_2_quest")
    val darkness = find("myq3_main_quest")
    val pet = find("follower_space_track")
    val selected = find("templetrek_reward_selected")
    val slot = find("templetrek_reward_token_slot")
}

internal object TrekVars : VarpReferences() {
    val origin = find("temple_trek_origin")
}

internal object TrekSave : VarpBuilder() {
    init {
        build("temple_trek_origin") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object TrekTimers : TimerReferences() {
    val tick = find("temple_trek_tick")
}

internal object TrekUi : InterfaceReferences() {
    val routes = find("templetrek_map")
    val reward = find("trek_rewards")
}

internal object TrekButtons : ComponentReferences() {
    val routes = (1..3).map { find("templetrek_map:templetrek_route_$it") }
    val options = listOf(find("trek_rewards:text"), find("trek_rewards:icons"))
    val buy = find("trek_rewards:buybutton")
}

internal object TrekPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TempleTrekking>("companions.toml")
    }
}
