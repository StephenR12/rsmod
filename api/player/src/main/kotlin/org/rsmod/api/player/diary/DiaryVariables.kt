package org.rsmod.api.player.diary

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object DiaryPages : VarpReferences() {
    val ardounge_achievement_diary = find("ardounge_achievement_diary")
    val ardounge_achievement_diary2 = find("ardounge_achievement_diary2")
    val atjun_tasks_4 = find("atjun_tasks_4")
    val desert_achievement_diary = find("desert_achievement_diary")
    val desert_achievement_diary2 = find("desert_achievement_diary2")
    val falador_achievement_diary = find("falador_achievement_diary")
    val falador_achievement_diary2 = find("falador_achievement_diary2")
    val fremennik_achievement_diary = find("fremennik_achievement_diary")
    val fremennik_achievement_diary2 = find("fremennik_achievement_diary2")
    val kandarin_achievement_diary = find("kandarin_achievement_diary")
    val kandarin_achievement_diary2 = find("kandarin_achievement_diary2")
    val kourend_achievement_diary = find("kourend_achievement_diary")
    val kourend_achievement_diary2 = find("kourend_achievement_diary2")
    val lumb_dray_achievement_diary = find("lumb_dray_achievement_diary")
    val lumb_dray_achievement_diary2 = find("lumb_dray_achievement_diary2")
    val morytania_achievement_diary = find("morytania_achievement_diary")
    val morytania_achievement_diary2 = find("morytania_achievement_diary2")
    val varrock_achievement_diary = find("varrock_achievement_diary")
    val varrock_achievement_diary2 = find("varrock_achievement_diary2")
    val western_achievement_diary = find("western_achievement_diary")
    val western_achievement_diary2 = find("western_achievement_diary2")
    val wilderness_achievement_diary = find("wilderness_achievement_diary")
    val wilderness_achievement_diary2 = find("wilderness_achievement_diary2")
}

internal object DiaryBits : VarBitReferences() {
    val atjun_easy_banana = find("atjun_easy_banana")
    val atjun_easy_boat_ardy = find("atjun_easy_boat_ardy")
    val atjun_easy_boat_sarim = find("atjun_easy_boat_sarim")
    val atjun_easy_cairn = find("atjun_easy_cairn")
    val atjun_easy_fishing = find("atjun_easy_fishing")
    val atjun_easy_gold = find("atjun_easy_gold")
    val atjun_easy_jogre = find("atjun_easy_jogre")
    val atjun_easy_seaweed = find("atjun_easy_seaweed")
    val atjun_easy_swing = find("atjun_easy_swing")
    val atjun_easy_tzhaar = find("atjun_easy_tzhaar")
    val atjun_hard_deathwing = find("atjun_hard_deathwing")
    val atjun_hard_dragon = find("atjun_hard_dragon")
    val atjun_hard_duradel = find("atjun_hard_duradel")
    val atjun_hard_fightcave = find("atjun_hard_fightcave")
    val atjun_hard_fightpits = find("atjun_hard_fightpits")
    val atjun_hard_karambwan = find("atjun_hard_karambwan")
    val atjun_hard_nature = find("atjun_hard_nature")
    val atjun_hard_oomlie = find("atjun_hard_oomlie")
    val atjun_hard_palm = find("atjun_hard_palm")
    val atjun_hard_xbow = find("atjun_hard_xbow")
    val atjun_med_agility = find("atjun_med_agility")
    val atjun_med_cart = find("atjun_med_cart")
    val atjun_med_charter = find("atjun_med_charter")
    val atjun_med_cleanup = find("atjun_med_cleanup")
    val atjun_med_crandor = find("atjun_med_crandor")
    val atjun_med_farming = find("atjun_med_farming")
    val atjun_med_glider = find("atjun_med_glider")
    val atjun_med_graahk = find("atjun_med_graahk")
    val atjun_med_karambwan = find("atjun_med_karambwan")
    val atjun_med_khazard = find("atjun_med_khazard")
    val atjun_med_machette = find("atjun_med_machette")
    val atjun_med_mahogany = find("atjun_med_mahogany")
    val atjun_med_shilo_lava = find("atjun_med_shilo_lava")
    val atjun_med_shilo_stairs = find("atjun_med_shilo_stairs")
    val atjun_med_shilo_vines = find("atjun_med_shilo_vines")
    val atjun_med_spider = find("atjun_med_spider")
    val atjun_med_teak = find("atjun_med_teak")
    val atjun_med_topaz = find("atjun_med_topaz")
    val atjun_med_volcano = find("atjun_med_volcano")

    val wildernessAnyComplete = find("wilderness_diary_any_complete")
    val started =
        DiaryRegion.entries.associateWith { region ->
            find(
                if (region == DiaryRegion.Karamja) "atjun_started"
                else "started_${region.key}_diary"
            )
        }

    val tiers =
        DiaryRegion.entries
            .flatMap { region ->
                DiaryDifficulty.entries.map { difficulty ->
                    val area = region.key
                    val level = difficulty.name.lowercase()
                    val short = if (difficulty == DiaryDifficulty.Medium) "med" else level
                    val legacy =
                        region == DiaryRegion.Karamja && difficulty != DiaryDifficulty.Elite
                    val complete =
                        if (legacy) "atjun_${short}_done" else "${area}_diary_${level}_complete"
                    val reward = if (legacy) "atjun_${short}_reward" else "${area}_${level}_reward"
                    (region to difficulty) to
                        DiaryTierVars(find("${area}_${short}_count"), find(complete), find(reward))
                }
            }
            .toMap()
}
