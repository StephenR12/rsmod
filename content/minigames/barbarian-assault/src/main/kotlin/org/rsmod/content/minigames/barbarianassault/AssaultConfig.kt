package org.rsmod.content.minigames.barbarianassault

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

internal enum class Role {
    Attacker,
    Collector,
    Defender,
    Healer,
}

internal enum class Penance {
    Fighter,
    Runner,
    Ranger,
    Healer,
    Queen,
}

internal object AssaultLocs : LocReferences() {
    val entrance = find("barbassault_recruitment_entrance")
    val exit = find("barbassault_recruitment_exit")
    val doors = (1..10).map { find("barbassault_door_" + it.toString().padStart(2, '0')) }
    val quick = find("barbassault_door_no_friends")
    val board = find("barbassault_blackboard")
    val machines =
        listOf("attacker", "defender", "healer").map { find("barassault_vending_machine_$it") }
    val traps =
        listOf(
            find("barbassault_trap"),
            find("barbassault_trap_1use"),
            find("barbassault_trap_broken"),
        )
    val launcher = find("barbassault_turret_bottom")
    val gameExit = find("barbassault_game_exit")
    val hopper = find("barbassault_egg_hopper")
    val poison = listOf(find("barbassault_crater_poision"), find("barbassault_crater_poision_big"))
    val lava = listOf(find("barbassault_crater_lava"), find("barbassault_crater_lava_big"))
    val mushroom = find("barbassault_petrified_mushroom")
    val spring = find("barbassault_natural_spring")
    val horn = find("barbassault_hugehorn")
}

internal object AssaultNpcs : NpcReferences() {
    val captain = find("barbassault_barbarian_captain")
    val commander = find("barbassault_barbarian_commander")
    val monsters =
        Penance.entries.take(4).map { kind ->
            (1..9).map { find("barbassault_pen_" + kind.name.lowercase() + "_lv$it") }
        }
    val queen = find("barbassault_pen_queen_new")
    val cannon = find("barbassault_egg_launcher_cannon")
    val northCannon = find("barbassault_egg_launcher_cannon_north")
}

internal object AssaultBits : VarBitReferences() {
    val level = listOf("att", "col", "def", "heal").map { find("barbassult_rolelevel_$it") }
    val base = Role.entries.map { find("barbassault_points_" + it.name.lowercase() + "_base") }
    val extra = Role.entries.map { find("barbassault_points_" + it.name.lowercase() + "_extra") }
    val queenExtra = find("barbassault_queenkills_extra")
    val queenHigh = find("barbassault_queenkills_extra_2")
    val eggs =
        listOf("poison", "explosive", "stun", "omega").map { find("barbassault_eggcount_$it") }
}

internal object AssaultVars : VarpReferences() {
    val wave = find("assault_nextwave")
    val playing = find("assault_playing")
    val queens = find("assault_queencredit")
    val granite = find("assault_boughtgranite")
}

internal object AssaultVarConfig : VarpBuilder() {
    init {
        for (name in
            listOf(
                "assault_nextwave",
                "assault_playing",
                "assault_queencredit",
                "assault_boughtgranite",
            )) build(name) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object AssaultTimers : TimerReferences() {
    val wave = find("assault_wave_tick")
}

internal object AssaultInterfaces : InterfaceReferences() {
    val roles = listOf("att", "col", "def", "heal").map { find("barbassault_over_$it") }
    val tutorial = find("barbassault_tutorial")
    val shop = find("barbassault_reward_shop")
}

internal object AssaultComponents : ComponentReferences() {
    val wave = Role.entries.map { find(hud(it) + "_wave#_text") }
    val shout = Role.entries.map { find(hud(it) + "_shout_text") }
    val hear = Role.entries.map { find(hud(it) + "_hear_detail_text") }

    private fun hud(role: Role): String {
        val name = listOf("att", "col", "def", "heal")[role.ordinal]
        return "barbassault_over_$name:barbassault_" + role.name.lowercase()
    }

    val chapters =
        listOf(
            find("barbassault_tutorial:barbassault_tut_01"),
            find("barbassault_tutorial:barbassault_tut_02"),
            find("barbassault_tutorial:barbassault_tut_03"),
            find("barbassault_tutorial:barbassault_tut_04"),
            find("barbassault_tutorial:barbassault_tut_05"),
            find("barbassault_tutorial:barbassault_tut_06"),
            find("barbassault_tutorial:barbassault_tut_07"),
            find("barbassault_tutorial:barbassault_tut_08"),
            find("barbassault_tutorial:barbassault_tut_09"),
            find("barbassault_tutorial:barbassault_tut_10"),
        )
}

internal object AssaultGear : ObjReferences() {
    val bait =
        listOf(
            find("barbassault_runner_food"),
            find("barbassault_runner_food2"),
            find("barbassault_runner_food3"),
        )
    val poison = (1..3).map { find("barbassault_poision_0$it") }
    val eggs =
        (1..4).map { find("barbassault_egg_0$it") } +
            listOf(
                find("barbassault_egg_poison"),
                find("barbassault_egg_poison_spikes"),
                find("barbassault_egg_heated_poison_spikes"),
            )
    val spikes = find("barbassault_spikes")
    val horns =
        listOf(
            find("barbassault_att_horn_01"),
            find("barbassault_horn_collector"),
            find("barbassault_defender_horn"),
            find("barbassault_heal_horn_01"),
        )
    val vials = listOf("full", "3q", "half", "1q", "empty").map { find("barbassault_vial_04_$it") }
}

internal object AssaultPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BarbarianAssault>("outpost.toml")
    }
}

internal object AssaultNpcStats : NpcEditor() {
    init {
        for (type in AssaultNpcs.monsters.flatten() + AssaultNpcs.queen) edit(type) {
            param[org.rsmod.api.config.refs.params.npc_com_xp_multiplier] = 0
        }
        edit(AssaultNpcs.monsters[0][0]) {
            hitpoints = 28
            attack = 26
            strength = 26
            defence = 25
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][1]) {
            hitpoints = 29
            attack = 28
            strength = 29
            defence = 27
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][2]) {
            hitpoints = 32
            attack = 33
            strength = 32
            defence = 34
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][3]) {
            hitpoints = 37
            attack = 36
            strength = 38
            defence = 37
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][4]) {
            hitpoints = 38
            attack = 41
            strength = 43
            defence = 44
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][5]) {
            hitpoints = 49
            attack = 50
            strength = 48
            defence = 48
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][6]) {
            hitpoints = 50
            attack = 56
            strength = 55
            defence = 52
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][7]) {
            hitpoints = 55
            attack = 60
            strength = 61
            defence = 62
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[0][8]) {
            hitpoints = 56
            attack = 71
            strength = 73
            defence = 66
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][0]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][1]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][2]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][3]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][4]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][5]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][6]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][7]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[1][8]) {
            hitpoints = 5
            attack = 1
            strength = 1
            defence = 1
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[2][0]) {
            hitpoints = 20
            attack = 22
            strength = 1
            defence = 21
            magic = 1
            ranged = 23
        }
        edit(AssaultNpcs.monsters[2][1]) {
            hitpoints = 28
            attack = 27
            strength = 1
            defence = 29
            magic = 1
            ranged = 24
        }
        edit(AssaultNpcs.monsters[2][2]) {
            hitpoints = 29
            attack = 32
            strength = 1
            defence = 33
            magic = 1
            ranged = 34
        }
        edit(AssaultNpcs.monsters[2][3]) {
            hitpoints = 34
            attack = 41
            strength = 1
            defence = 42
            magic = 1
            ranged = 40
        }
        edit(AssaultNpcs.monsters[2][4]) {
            hitpoints = 41
            attack = 44
            strength = 1
            defence = 46
            magic = 1
            ranged = 45
        }
        edit(AssaultNpcs.monsters[2][5]) {
            hitpoints = 50
            attack = 51
            strength = 1
            defence = 54
            magic = 1
            ranged = 52
        }
        edit(AssaultNpcs.monsters[2][6]) {
            hitpoints = 50
            attack = 63
            strength = 1
            defence = 61
            magic = 1
            ranged = 62
        }
        edit(AssaultNpcs.monsters[2][7]) {
            hitpoints = 54
            attack = 69
            strength = 1
            defence = 68
            magic = 1
            ranged = 70
        }
        edit(AssaultNpcs.monsters[2][8]) {
            hitpoints = 58
            attack = 79
            strength = 1
            defence = 80
            magic = 1
            ranged = 78
        }
        edit(AssaultNpcs.monsters[3][0]) {
            hitpoints = 27
            attack = 9
            strength = 8
            defence = 9
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][1]) {
            hitpoints = 32
            attack = 13
            strength = 15
            defence = 14
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][2]) {
            hitpoints = 37
            attack = 20
            strength = 18
            defence = 19
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][3]) {
            hitpoints = 43
            attack = 22
            strength = 23
            defence = 21
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][4]) {
            hitpoints = 49
            attack = 26
            strength = 27
            defence = 25
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][5]) {
            hitpoints = 55
            attack = 30
            strength = 32
            defence = 28
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][6]) {
            hitpoints = 60
            attack = 33
            strength = 36
            defence = 34
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][7]) {
            hitpoints = 67
            attack = 40
            strength = 38
            defence = 36
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.monsters[3][8]) {
            hitpoints = 76
            attack = 40
            strength = 44
            defence = 38
            magic = 1
            ranged = 1
        }
        edit(AssaultNpcs.queen) {
            hitpoints = 250
            attack = 260
            strength = 92
            defence = 132
            magic = 1
            ranged = 116
        }
    }
}
