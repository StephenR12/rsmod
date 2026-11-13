package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor

internal object KnightAndRiftPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TolnasRift>("knights-and-rift.toml")
    }
}

internal object KnightAndRiftNpcs : org.rsmod.api.type.refs.npc.NpcReferences() {
    val soulbane_bear = find("soulbane_bear")
    val soulbane_goblin = find("soulbane_goblin")
    val soulbane_rat = find("soulbane_rat")
    val soulbane_rat2 = find("soulbane_rat2")
    val soulbane_unicorn = find("soulbane_unicorn")
    val soulbane_fear_beast = find("soulbane_fear_beast")
    val soulbane_conf_beast = find("soulbane_conf_beast")
    val soulbane_less_beast3 = find("soulbane_less_beast3")
    val aggressive_black_knight = find("aggressive_black_knight")
    val kr_aggressive_black_knight = find("kr_aggressive_black_knight")
    val aggressive_black_knight_f = find("aggressive_black_knight_f")
    val black_knight = find("black_knight")
    val black_knight_f = find("black_knight_f")
    val white_knight = find("white_knight")
    val white_knight_f = find("white_knight_f")
    val white_knight_yellow_plumes = find("white_knight_yellow_plumes")
    val white_knight_yellow_plumes_f = find("white_knight_yellow_plumes_f")
    val white_knight_green_plumes = find("white_knight_green_plumes")
    val white_knight_green_plumes_f = find("white_knight_green_plumes_f")
    val white_knight_blue_plumes = find("white_knight_blue_plumes")
    val white_knight_blue_plumes_f = find("white_knight_blue_plumes_f")
}

internal object KnightAndRiftStats : NpcEditor() {
    init {
        edit(KnightAndRiftNpcs.soulbane_bear) {
            hitpoints = 50
            attack = 38
            strength = 41
            defence = 38
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_goblin) {
            hitpoints = 50
            attack = 38
            strength = 41
            defence = 38
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_rat) {
            hitpoints = 50
            attack = 38
            strength = 41
            defence = 38
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_rat2) {
            hitpoints = 50
            attack = 38
            strength = 41
            defence = 38
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_unicorn) {
            hitpoints = 50
            attack = 38
            strength = 41
            defence = 38
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_fear_beast) {
            hitpoints = 57
            attack = 45
            strength = 48
            defence = 45
            magic = 1
            ranged = 1
            respawnRate = 30
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_conf_beast) {
            hitpoints = 64
            attack = 52
            strength = 55
            defence = 52
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.soulbane_less_beast3) {
            hitpoints = 71
            attack = 59
            strength = 62
            defence = 59
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 4
        }
        edit(KnightAndRiftNpcs.aggressive_black_knight) {
            hitpoints = 42
            attack = 25
            strength = 25
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 5
        }
        edit(KnightAndRiftNpcs.kr_aggressive_black_knight) {
            hitpoints = 42
            attack = 25
            strength = 25
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 5
        }
        edit(KnightAndRiftNpcs.aggressive_black_knight_f) {
            hitpoints = 42
            attack = 25
            strength = 25
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 5
        }
        edit(KnightAndRiftNpcs.black_knight) {
            hitpoints = 42
            attack = 25
            strength = 25
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 5
        }
        edit(KnightAndRiftNpcs.black_knight_f) {
            hitpoints = 42
            attack = 25
            strength = 25
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 25
            param[params.attackrate] = 5
        }
        edit(KnightAndRiftNpcs.white_knight) {
            hitpoints = 52
            attack = 27
            strength = 29
            defence = 21
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_f) {
            hitpoints = 52
            attack = 27
            strength = 29
            defence = 21
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_yellow_plumes) {
            hitpoints = 52
            attack = 30
            strength = 29
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_yellow_plumes_f) {
            hitpoints = 52
            attack = 30
            strength = 29
            defence = 25
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_green_plumes) {
            hitpoints = 52
            attack = 32
            strength = 29
            defence = 27
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_green_plumes_f) {
            hitpoints = 52
            attack = 32
            strength = 29
            defence = 27
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_blue_plumes) {
            hitpoints = 55
            attack = 32
            strength = 35
            defence = 27
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
        edit(KnightAndRiftNpcs.white_knight_blue_plumes_f) {
            hitpoints = 55
            attack = 32
            strength = 35
            defence = 27
            magic = 1
            ranged = 1
            respawnRate = 50
            param[params.attackrate] = 7
        }
    }
}
