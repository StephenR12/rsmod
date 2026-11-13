package org.rsmod.content.minigames.fightcaves

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object FightCaveLocs : LocReferences() {
    val entrance = find("tzhaar_fightcave_wall_entrance")
    val exit = find("tzhaar_fightcave_wall_exit")
}

internal object FightCaveNpcs : NpcReferences() {
    val tzKihA = find("tzhaar_fightcave_swarm_1a")
    val tzKihB = find("tzhaar_fightcave_swarm_1b")
    val tzKekA = find("tzhaar_fightcave_swarm_2a")
    val tzKekB = find("tzhaar_fightcave_swarm_2b")
    val tzKekSplit = find("tzhaar_fightcave_swarm_2spawn")
    val tokXilA = find("tzhaar_fightcave_swarm_3a")
    val tokXilB = find("tzhaar_fightcave_swarm_3b")
    val ytMejKotA = find("tzhaar_fightcave_swarm_4a")
    val ytMejKotB = find("tzhaar_fightcave_swarm_4b")
    val ketZekA = find("tzhaar_fightcave_swarm_5a")
    val ketZekB = find("tzhaar_fightcave_swarm_5b")
    val jad = find("tzhaar_fightcave_swarm_boss")
    val healer = find("tzhaar_fightcave_swarm_boss_cleric")

    val all =
        listOf(
            tzKihA,
            tzKihB,
            tzKekA,
            tzKekB,
            tzKekSplit,
            tokXilA,
            tokXilB,
            ytMejKotA,
            ytMejKotB,
            ketZekA,
            ketZekB,
            jad,
            healer,
        )
}

internal object FightCaveVarps : VarpReferences() {
    val totalJadKills = find("total_jad_kills")
}

internal object FightCaveSeqs : SeqReferences() {
    val tzKihAttack = find("thzaar_unarmed_attack")
    val tzKekAttack = find("thzaar_ring_attack")
    val tokXilAttack = find("magmaquris_spine_attack")
    val ytMejKotAttack = find("lizard_cleric_attack")
    val ketZekMagic = find("igniferum_ranged")
    val jadRanged = find("lordmagmus_smash")
    val jadMelee = find("lordmagmus_attack")
    val jadMagic = find("lordmagmus_fire")
}


internal object FightCaveAttackRange : org.rsmod.api.type.editors.npc.NpcEditor() {
    init {
        with(FightCaveNpcs) {
            for (type in listOf(tokXilA, tokXilB, ketZekA, ketZekB, jad))
                edit(type) { attackRange = 15 }
        }
    }
}
