package org.rsmod.content.minigames.colosseum

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object ColosseumLocs : LocReferences() {
    val outsideEntrance = find("colosseum_entrance_outside")
    val lobbyExit = find("colosseum_exit_lobby")
    val arenaEntrance = find("colosseum_entrance")
    val arenaExit = find("colosseum_exit")
    val reward = find("colosseum_reward")
    val scoreboard = find("colosseum_scoreboard")
}

internal object ColosseumNpcs : NpcReferences() {
    val jaguar = find("colosseum_jaguar_warrior")
    val shaman = find("colosseum_standard_mager")
    val minotaur = find("colosseum_minotaur")
    val routefindingMinotaur = find("colosseum_minotaur_routefind")
    val ranger = find("colosseum_warbander_ranged_female")
    val mager = find("colosseum_warbander_mage_male")
    val meleer = find("colosseum_warbander_melee_male")
    val javelin = find("colosseum_javelin_colossus")
    val manticore = find("colosseum_manticore")
    val shockwave = find("colosseum_shockwave_colossus")
    val sol = find("colosseum_sol_p1")
    val doomScorpion = find("colosseum_doom_scorpion")
    val bees = find("colosseum_modifier_bees")
    val healingTotem = find("colosseum_healing_totem")
    val solarFlare = find("colosseum_solar_flare")

    val monsters =
        listOf(
            jaguar,
            shaman,
            minotaur,
            routefindingMinotaur,
            ranger,
            mager,
            meleer,
            javelin,
            manticore,
            shockwave,
            sol,
        )
    val all = monsters + listOf(doomScorpion, bees, healingTotem, solarFlare)
}

internal object ColosseumSeqs : SeqReferences() {
    val minotaurMelee = find("npc_minotaur_boss_attack_melee")
    val minotaurMagic = find("npc_minotaur_boss_attack_magic")
    val jaguarAttack = find("npc_jaguar_ranger_claws_attack")
    val rangerAttack = find("npc_fremennik_warbander_archer_att_colosseum")
    val magerAttack = find("npc_fremennik_warbander_mage_zaros_vertical_casting_walkmerge")
    val meleerAttack = find("npc_fremennik_warbander_melee_human_sword_stab")
    val shamanAttack = find("npc_serpent_mager_casting")
    val manticoreCharge = find("npc_manticore_01_triple_charge")
    val manticoreThrow = find("npc_manticore_01_triple_throw")
    val solMelee = find("npc_colossi_finalboss_01_melee_attack")
    val solMeleeTelegraph = find("npc_colossi_finalboss_01_melee_attack_telegraph")
    val solGrapple = find("npc_colossi_finalboss_01_grapple_attack_telegraph")
    val solShieldSlam = find("npc_colossi_finalboss_01_shieldslam_telegraph")
    val solTriple = find("npc_colossi_finalboss_tripleattack")
    val javelinAttack = find("npc_colossi_javelin_01_range_attack")
    val javelinArtillery = find("npc_colossi_javelin_01_artillery_attack")
    val shockwaveAttack = find("npc_colossi_shockwave_01_clapattack")
}

internal object ColosseumObjs : ObjReferences() {
    val splinters = find("sunfiresplinter")
    val tonalztics = find("tonalztics_of_ralos_uncharged")
    val echoCrystal = find("echo_crystal")
    val sunfireArmour = find("set_sunfire_fanatic")
    val quiver = find("dizanas_quiver_uncharged")
    val pet = find("solhereditpet")
}

internal object ColosseumVarps : VarpReferences() {
    val glory = find("colosseum_glory")
    val totalWaves = find("total_colosseum_waves_completed")
    val currentGlory = find("colosseum_current_glory")
    val waveStart = find("colosseum_wave_start_time")
    val waveDamage = find("colosseum_wave_damage_taken")
    val lastModifierGlory = find("colosseum_last_modifier_glory")
    val lastWaveDuration = find("colosseum_last_wave_duration")
    val totalDuration = find("colosseum_total_duration")
    val killtime = find("colosseum_killtime")
    val totalSolKills = find("total_sol_kills")
}

internal object ColosseumVarBits : VarBitReferences() {
    val selectedModifier = find("colosseum_selected_modifier")
    val blasphemy = find("colosseum_modifier_blasphemy_stacks_client")
    val reentry = find("colosseum_modifier_reentry_stacks_client")
    val myopia = find("colosseum_modifier_myopia_stacks_client")
    val frailty = find("colosseum_modifier_frailty_stacks_client")
    val solarflare = find("colosseum_modifier_solarflare_stacks_client")
    val relentless = find("colosseum_modifier_relentless_stacks_client")
    val volatility = find("colosseum_modifier_volatility_stacks_client")
    val mantimayhem = find("colosseum_modifier_mantimayhem_stacks_client")
    val doom = find("colosseum_doom_stacks_client")
    val lootOpened = find("colosseum_loot_opened")
    val highestWave = find("colosseum_highest_wave")
}
