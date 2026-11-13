package org.rsmod.content.minigames.giantsfoundry

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object FoundryQueues : QueueReferences() {
    val refine = find("giants_foundry_work")
}

internal object FoundryTimers : TimerReferences() {
    val cooling = find("giants_foundry_preform_cooling")
}

internal object FoundryBits : VarBitReferences() {
    val quest = find("sleeping_giants")
    val words = (1..2).map { find("giants_foundry_commission_word_$it") }
    val filter = find("giants_foundry_reward_shop_filter")
    val tab = find("giants_foundry_mould_current_tab")
    val parts = listOf("ricasso", "blade", "tip").map { find("giants_foundry_mould_selected_$it") }
    val alloy = find("giants_foundry_mould_alloy_dificulty")
    val mould = find("giants_foundry_mould_state")
    val unlocked =
        listOf("ricasso", "blade", "tip").map { part ->
            (7..11).map { find("giants_foundry_unlocked_mould_${part}_$it") }
        }
    val metals =
        listOf("bronze", "iron", "steel", "mithril", "adamant", "rune").map {
            find("giants_foundry_${it}_count")
        }
    val crucible = find("giants_foundry_crucible_state")
    val difficulty = find("giants_foundry_preform_dificulty")
    val quality = find("giants_foundry_preform_quality")
    val sequences = (0..6).map { find("giants_foundry_seed_$it") }
    val stored = find("giants_foundry_preform_stored")
    val heat = find("giants_foundry_preform_temperature")
    val progress = find("giants_foundry_preform_completion")
    val initial = find("giants_foundry_preform_start_quality")
}

internal object FoundryVars : VarpReferences() {
    val points = find("giants_foundry_reward_shop_points")
    val selected = find("giants_foundry_reward_shop_selected_item")
}

internal object FoundryLocs : LocReferences() {
    val entrance = find("giants_foundry_entrance")
    val exit = find("giants_foundry_exit")
    val tools =
        listOf("trip_hammer", "grindstone", "polishing_wheel").map { find("giants_foundry_$it") }
    val crucibles =
        listOf("crucible_multi", "crucible_empty", "crucible_partial", "crucible_full").map {
            find("giants_foundry_$it")
        }
    val jigs =
        listOf(
                "mould_jig",
                "jig_empty",
                "jig_mould",
                "jig_mould_filled",
                "jig_mould_cooling",
                "jig_mould_cooled",
            )
            .map { find("giants_foundry_$it") }
    val storage =
        listOf("preform_storage", "preform_storage_empty", "preform_storage_full").map {
            find("giants_foundry_$it")
        }
    val heat = find("giants_foundry_lava_pool")
    val cool = find("giants_foundry_waterfall")
    val boxes =
        listOf("supply_box_multi", "supply_box", "supply_box_empty").map {
            find("giants_foundry_$it")
        }
    val bank = find("giants_foundry_chest")
}

internal object FoundryNpcs : NpcReferences() {
    val kovac = find("giants_foundry_kovac")
    val multi = find("giants_foundry_kovac_multi")
}

internal object FoundryObjs : ObjReferences() {
    val glass = find("beer_glass")
    val ores =
        listOf("coal", "iron_ore", "mithril_ore", "adamantite_ore", "runite_ore").map {
            find("cert_$it")
        }
    val preform = find("giants_foundry_preform")
    val glovesIce = find("smithing_uniform_gloves_ice")
    val rewards =
        listOf(
                "double_ammo_mould",
                "kovacs_grog",
                "smithing_catalyst",
                "giants_foundry_ore_pack",
                "smithing_uniform_torso",
                "smithing_uniform_legs",
                "smithing_uniform_boots",
                "smithing_uniform_gloves",
                "giants_foundry_colossal_blade",
            )
            .map { find(it) }
}

internal object FoundrySeqs : SeqReferences() {
    val work = find("giants_foundry_work_preform")
    val loop = find("giants_foundry_work_preform_loop")
    val finish = find("giants_foundry_finish_sword")
}

internal object FoundryInterfaces : InterfaceReferences() {
    val mould = find("giants_foundry_mould")
    val shop = find("giants_foundry_reward_shop")
    val hud = find("giants_foundry_hud")
}

internal object FoundryButtons : ComponentReferences() {
    val mouldTabs = find("giants_foundry_mould:tab_panel")
    val mouldList = find("giants_foundry_mould:content")
    val sweet = find("giants_foundry_hud:sweetspot_layer")
    val set = find("giants_foundry_mould:set_button")
    val reset = find("giants_foundry_mould:reset_button")
    val shopList = find("giants_foundry_reward_shop:list")
    val buy = find("giants_foundry_reward_shop:buy_button")
    val filter = find("giants_foundry_reward_shop:dropdown_content")
}

internal object FoundryPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GiantsFoundry>("foundry-npcs.toml")
    }
}
