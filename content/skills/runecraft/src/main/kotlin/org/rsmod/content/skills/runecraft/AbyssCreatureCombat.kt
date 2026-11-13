package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.seq.SeqReferences

internal object AbyssCombatSeqs : SeqReferences() {
    val attacks =
        listOf("abyssal_leech_attack", "abyssal_pyramid_attack", "abyssal_walker_attack").map {
            find(it)
        }
    val blocks =
        listOf("abyssal_leech_defend", "abyssal_pyramid_defend", "abyssal_walker_defend").map {
            find(it)
        }
    val deaths =
        listOf("abyssal_leech_death", "abyssal_pyramid_death", "abyssal_walker_death").map {
            find(it)
        }
    val magic = find("abyssal_pyramid_ranged_attack")
}

internal object AbyssCreatureCombat : NpcEditor() {
    init {
        for ((index, creature) in AbyssCreatures.all.withIndex()) edit(creature) {
            huntMode = huntmodes.aggressive_melee
            huntRange = 5
            respawnRate = 5
            ranged = 1
            magic = 1
            param[params.attack_anim] = AbyssCombatSeqs.attacks[index]
            param[params.defend_anim] = AbyssCombatSeqs.blocks[index]
            param[params.death_anim] = AbyssCombatSeqs.deaths[index]
            param[params.npc_com_xp_multiplier] = 250
            when (index) {
                0 -> {
                    hitpoints = 10
                    attack = 95
                    strength = 5
                    defence = 25
                    param[params.attackrate] = 2
                    param[params.npc_attack_type] = categories.attacktype_slash
                    param[params.attack_melee] = 100
                    param[params.melee_strength] = 10
                    param[params.defence_stab] = 50
                    param[params.defence_slash] = 50
                    param[params.defence_crush] = 100
                    param[params.defence_magic] = 50
                    for (defence in rangedDefences) param[defence] = 10
                }
                1 -> {
                    hitpoints = 50
                    attack = 30
                    strength = 90
                    defence = 30
                    param[params.attackrate] = 4
                    param[params.npc_attack_type] = categories.attacktype_stab
                    param[params.attack_melee] = 15
                    param[params.melee_strength] = 5
                    param[params.defence_stab] = 70
                    param[params.defence_slash] = 70
                    param[params.defence_crush] = 70
                    param[params.defence_magic] = 150
                    for (defence in rangedDefences) param[defence] = 70
                }
                2 -> {
                    hitpoints = 95
                    attack = 5
                    strength = 100
                    defence = 95
                    param[params.attackrate] = 5
                    param[params.npc_attack_type] = categories.attacktype_crush
                    param[params.attack_melee] = 5
                    param[params.melee_strength] = 10
                    param[params.defence_stab] = 75
                    param[params.defence_slash] = 75
                    param[params.defence_crush] = 10
                    param[params.defence_magic] = 75
                    for (defence in rangedDefences) param[defence] = 75
                }
            }
        }
    }

    private val rangedDefences
        get() = listOf(params.defence_light, params.defence_standard, params.defence_heavy)
}
