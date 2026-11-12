package org.rsmod.content.minigames.cox

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object CoxLocs : LocReferences() {
    val entrance = find("raids_entrance_steps")
    val exit = find("raids_exit_steps")
    val bossBarrier = find("raids_olm_barrier")
    val rewardChest = find("raids_reward_chest")
}

internal object CoxNpcs : NpcReferences() {
    val tekton = find("raids_tekton_fighting_standard")
    val vasa = find("raids_vasanistirio_walking")
    val vasaCrystal = find("raids_vasanistirio_crystal")
    val vanguardMelee = find("raids_vanguard_melee")
    val vanguardRanged = find("raids_vanguard_ranged")
    val vanguardMagic = find("raids_vanguard_magic")
    val vespula = find("raids_vespula_flying")
    val vespulaPortal = find("raids_vespula_portal")
    val babyMuttadile = find("raids_dogodile_junior")
    val muttadile = find("raids_dogodile")
    val meatTree = find("raids_dogodile_meat_tree")
    val shamanA = find("raids_lizardshaman_a")
    val shamanB = find("raids_lizardshaman_b")
    val mysticA = find("raids_skeletonmystic_a")
    val mysticB = find("raids_skeletonmystic_b")
    val mysticC = find("raids_skeletonmystic_c")
    val guardianLeft = find("raids_stoneguardians_left")
    val guardianRight = find("raids_stoneguardians_right")
    val iceDemon = find("raids_icedemon_combat")
    val olmRight = find("olm_hand_right")
    val olmHead = find("olm_head")
    val olmLeft = find("olm_hand_left")

    val all =
        listOf(
            tekton,
            vasa,
            vasaCrystal,
            vanguardMelee,
            vanguardRanged,
            vanguardMagic,
            vespula,
            vespulaPortal,
            babyMuttadile,
            muttadile,
            meatTree,
            shamanA,
            shamanB,
            mysticA,
            mysticB,
            mysticC,
            guardianLeft,
            guardianRight,
            iceDemon,
            olmRight,
            olmHead,
            olmLeft,
        )
}

internal object CoxSeqs : SeqReferences() {
    val tektonAttack = find("tekton_hammer_crush")
    val vasaAttack = find("vasa_attack")
    val vanguardMelee = find("vanguard_attack_melee")
    val vanguardRanged = find("vanguard_attack_ranged")
    val vanguardMagic = find("vanguard_attack_magic")
    val vespulaAttack = find("vespula_attack_ranged_flying")
    val olmHeadAttack = find("olm_head_attack_front_enraged")
    val olmLeftHeal = find("olm_hand_left_cast_heal")
    val olmLeftShock = find("olm_hand_left_cast_earthshock")
}

internal object CoxObjs : ObjReferences() {
    val xericsAid = find("raids_vial_xericaid_strong_4")
    val prayerEnhance = find("raids_vial_prayer_strong_4")
    val overload = find("raids_vial_overload_strong_4")
    val twistedBow = find("twisted_bow")
    val elderMaul = find("elder_maul")
    val kodaiInsignia = find("kodai_insignia")
    val ancestralHat = find("ancestral_hat")
    val ancestralTop = find("ancestral_robe_top")
    val ancestralBottom = find("ancestral_robe_bottom")
    val dragonClaws = find("dragon_claws")
    val bulwark = find("dinhs_bulwark")
    val dexterousScroll = find("raids_prayerscroll")
    val arcaneScroll = find("raids_prayerscroll_augury")
}

internal object CoxVarBits : VarBitReferences() {
    val partySize = find("raids_client_partysize")
    val progress = find("raids_client_progress")
    val inDungeon = find("raids_client_indungeon")
    val partyScore = find("raids_client_partyscore")
    val challengeMode = find("raids_challenge_mode")
    val died = find("raids_died")
}

internal object CoxVarps : VarpReferences() {
    val playerScore = find("raids_playerscore")
    val completions = find("total_completed_xericchambers")
    val challengeCompletions = find("total_completed_xericchambers_challenge")
}
