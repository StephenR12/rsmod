@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.varp.VarpReferences

typealias varps = BaseVarps

object BaseVarps : VarpReferences() {
    val skillcape_hitpoints_boost_ready_at = find("skillcape_hitpoints_boost_ready_at")
    val skillcape_prayer_boost_ready_at = find("skillcape_prayer_boost_ready_at")
    val toggle_list = find("toggle_list")
    val vengeance_anim_toggles = find("vengeance_anim_toggles")
    /*
     * These "generic" temporary-state varps are used across multiple interfaces to track temporary
     * state. Unlike varps tied to a specific piece of content with child varbits, these are more
     * general-purpose.
     */
    val if1 = find("if1", 59242162)
    val if2 = find("if2", 59469143)
    val if3 = find("if3", 59696124)

    val canoeing_menu = find("canoeing_menu", 153212296)

    val com_mode = find("com_mode", 9764025)
    // Note: This varp seems to only be transmitted while wielding melee weapons and correlates to
    // Controlled (0), Accurate (1), Aggressive (2), and Defensive (3).
    // Though it may be the case that it actually represents the current "XP" type being granted.
    val com_stance = find("com_stance", 10444968)
    val option_nodef = find("option_nodef", 39044574)
    val sa_energy = find("sa_energy", 68098142)
    val sa_attack = find("sa_attack", 68325123)
    val slayer_count = find("slayer_count")
    val slayer_target = find("slayer_target")
    val poison = find("poison")
    val poison_2 = find("poison_2")
    val disease = find("disease")
    val soulreaper_souls = find("soulreaper_stacks", 858899946)
    val iban_charges = find("ibanmulti")
    val magearena = find("magearena")
    val magearena_charge = find("magearena_charge")

    val option_run = find("option_run", 39271563)
    val option_attackpriority = find("option_attackpriority", 251271828)
    val option_attackpriority_npc = find("option_attackpriority_npc", 296441051)

    val option_master_volume = find("option_master_volume", 861623718)
    val option_music = find("option_music", 38136650)
    val option_sounds = find("option_sounds", 38363631)
    val option_areasounds = find("option_areasounds", 197931274)

    val musicplay = find("musicplay", 4089500)
    val settings_tracking = find("settings_tracking", 648034597)
    val chat_filter_assist = find("chat_filter_assist", 239468797)

    /** Cache varp 101: the player's persistent quest-point total. */
    val quest_points = find("qp")

    val cookquest = find("cookquest", 6586291)
    val tbwt_main = find("tbwt_main")
    val tbwt_tiadeche = find("tbwt_tiadeche")
    val druidic_ritual = find("druidquest")
    val golem_quest = find("golem_quest")
    val heroes_quest = find("heroquest")
    val legends_quest = find("legendsquest")
    val doricquest = find("doricquest", 7040253)
    val haunted = find("haunted", 7267234)
    val runemysteries = find("runemysteries", 14303645)
    val hetty = find("hetty", 15211569)
    val hunt = find("hunt", 16119493)
    val rjquest = find("rjquest", 32689106)
    val imp = find("imp", 36320802)
    val dragonquest = find("dragonquest", 39952498)
    val vampire = find("vampire", 40406460)
    val sheep = find("sheep", 40633441)
    val squire = find("squire")
    val mcannon = find("mcannon")
    val elenaquest = find("elenaquest")
    val itwatchtower = find("itwatchtower")
    val eadgar_quest = find("eadgar_quest")
    val biohazard = find("biohazard")
    val priest_in_peril = find("priestperil")
    val monkey_madness = find("mm_main")
    val great_brain_robbery = find("brain_quest_var")
    val rag_and_bone_man_i = find("rag_quest")
    val rag_and_bone_man_ii = find("rag_quest")
    /** Cache varp 715: unused by cache-233 client scripts/varbits and reserved for II hand-ins. */
    val rag_and_bone_man_ii_handed_mask = find("rag_and_bone_man_ii_handed_mask")
    val onesmallfavour = find("onesmallfavour")
    val big_chompy_bird_hunting = find("chompybird")
    val elid_main = find("elid_main")
    val morttonquest = find("morttonquest")
    val waterfall_quest = find("waterfall_quest")
    val kandarin_achievement_diary2 = find("kandarin_achievement_diary2")

    val colosseum_glory = find("colosseum_glory", 937435372)

    val collection_count_other_max = find("collection_count_other_max", 1049110024)
    val collection_count_other = find("collection_count_other", 1048883043)
    val collection_count_minigames_max = find("collection_count_minigames_max", 1048656062)
    val collection_count_minigames = find("collection_count_minigames", 1048429081)
    val collection_count_clues_max = find("collection_count_clues_max", 1048202100)
    val collection_count_clues = find("collection_count_clues", 1047975119)
    val collection_count_raids_max = find("collection_count_raids_max", 1047748138)
    val collection_count_raids = find("collection_count_raids", 1047521157)
    val collection_count_bosses_max = find("collection_count_bosses_max", 1047294176)
    val collection_count_bosses = find("collection_count_bosses", 1047067195)
    val collection_count_max = find("collection_count_max", 668235906)
    val collection_count = find("collection_count", 668008925)

    val map_clock = find("map_clock", 698878341)
    val date_vars = find("date_vars", 698424318)
    val date_minutes = find("date_minutes")
    val aide_tele_timer = find("aide_tele_timer")
    val skillcape_perks = find("skillcape_perks")

    /* Server-side-only types */
    val surge_potion_cooldown_cycles = find("surge_potion_cooldown_cycles")
    val dt2_vestige_progress = find("dt2_vestige_progress")
    val godwars_altar_available_at = find("godwars_altar_available_at")
    val rag_and_bone_man_i_handed_mask = find("rag_and_bone_man_i_handed_mask")
    val music_playlist = find("music_playlist")
    val music_temp_state_3 = find("music_temp_state_3")
    val music_temp_state_2 = find("music_temp_state_2")
    val music_temp_state_1 = find("music_temp_state_1")
    val gameframe = find("gameframe")
    val pk_prey1 = find("pk_prey1")
    val pk_prey2 = find("pk_prey2")
    val pk_predator1 = find("pk_predator1")
    val pk_predator2 = find("pk_predator2")
    val pk_predator3 = find("pk_predator3")
    val prayer_drain = find("prayer_drain")
    val prayer0 = find("prayer0")
    val playtime = find("playtime")
    val generic_temp_state_65516 = find("generic_temp_state_65516")
    val dinhs_passive_delay = find("dinhs_passive_delay")
    val com_maxhit = find("com_maxhit")
    val forinthry_surge_expiration = find("forinthry_surge_expiration")
    val saved_autocast_state_staff = find("saved_autocast_state_staff")
    val saved_autocast_state_bladed_staff = find("saved_autocast_state_bladed_staff")
    val lastcombat = find("lastcombat")
    val lastcombat_pvp = find("lastcombat_pvp")
    val aggressive_npc = find("aggressive_npc")
    val generic_temp_coords_65529 = find("generic_temp_coords_65529")
    val inv_capacity_65530 = find("inv_capacity_65530")
    val generic_storage_65531 = find("generic_storage_65531")

    /*
     * "Restore" varps serve as temporary storage for varps that are modified temporarily and need
     * to be restored later.
     */
    val temp_restore_65527 = find("temp_restore_65527")
}
