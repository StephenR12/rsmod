package org.rsmod.content.minigames.inferno

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object InfernoLocs : LocReferences() {
    val entrance = find("inferno_entrance")
    val exit = find("inferno_exit")
}

internal object InfernoNpcs : NpcReferences() {
    val nibbler = find("inferno_nibbler")
    val bat = find("inferno_creature_harpie")
    val blob = find("inferno_creature_splitter")
    val blobMage = find("inferno_creature_splitter_mage")
    val blobRange = find("inferno_creature_splitter_range")
    val blobMelee = find("inferno_creature_splitter_melee")
    val melee = find("inferno_creature_melee")
    val ranger = find("inferno_creature_ranger")
    val mager = find("inferno_creature_mager")
    val jad = find("inferno_jad")
    val jadHealer = find("inferno_jad_healer")
    val finalRanger = find("inferno_ranger_finalwave")
    val finalMager = find("inferno_mager_finalwave")
    val finalJad = find("inferno_jad_finalwave")
    val finalJadHealer = find("inferno_jad_healer_finalwave")
    val zuk = find("inferno_tzkalzuk_placeholder")
    val shield = find("inferno_moving_safespot")
    val zukHealer = find("inferno_zuk_healer")

    val combatants =
        listOf(
            nibbler,
            bat,
            blob,
            blobMage,
            blobRange,
            blobMelee,
            melee,
            ranger,
            mager,
            jad,
            jadHealer,
            finalRanger,
            finalMager,
            finalJad,
            finalJadHealer,
            zukHealer,
        )

    val all = combatants + listOf(zuk, shield)
}

internal object InfernoSeqs : SeqReferences() {
    val nibblerAttack = find("jalnib_attack")
    val batAttack = find("jalmejrah_attack")
    val blobMagic = find("jalak_attack_magic")
    val blobMelee = find("jalak_attack_melee")
    val blobRanged = find("jalak_attack_ranged")
    val meleeAttack = find("jalimkot_attack")
    val rangerMelee = find("jalxil_attack_melee")
    val rangerRanged = find("jalxil_attack_ranged")
    val magerMagic = find("jalakxil_attack_magic")
    val magerResurrect = find("jalakxil_resurrect")
    val magerMelee = find("jalakxil_attack_melee")
    val jadMelee = find("jaltokjad_attack_melee")
    val jadMagic = find("jaltokjad_attack_magic")
    val jadRanged = find("jaltokjad_attack_ranged")
    val zukAttack = find("zuk_attack")
}

internal object InfernoVarBits : VarBitReferences() {
    val sacrificedFireCape = find("inferno_sacrificed_firecape")
    val playerInInferno = find("player_in_inferno")
    val pillar1Health = find("inferno_safespot1_health")
    val pillar2Health = find("inferno_safespot2_health")
    val pillar3Health = find("inferno_safespot3_health")
    val zukHealth = find("inferno_zuk_hp")
    val zukBaseHealth = find("inferno_zuk_base_hp")
}

internal object InfernoVarps : VarpReferences() {
    val totalZukKills = find("total_zuk_kills")
}
