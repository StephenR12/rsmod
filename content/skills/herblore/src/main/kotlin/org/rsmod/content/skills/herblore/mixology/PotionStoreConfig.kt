package org.rsmod.content.skills.herblore.mixology

import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

/** Extracted from cache233.4 client3750/4818, not from item-ID arithmetic. */
public data class PotionStoreSpec(
    val doseEnum: Int,
    val counterName: String,
    val preferenceName: String?,
)

public object PotionStoreSpecs {
    val all =
        listOf(
            PotionStoreSpec(
                729,
                "potionstore_agility_potion",
                "potionstore_vile_size_agility_potion",
            ),
            PotionStoreSpec(4085, "potionstore_ancient_brew", "potionstore_vile_size_ancient_brew"),
            PotionStoreSpec(144, "potionstore_anti_venom", "potionstore_vile_size_anti_venom"),
            PotionStoreSpec(242, "potionstore_anti_venom_p", "potionstore_vile_size_anti_venom_p"),
            PotionStoreSpec(733, "potionstore_antidote_p", "potionstore_vile_size_antidote_p"),
            PotionStoreSpec(734, "potionstore_antidote_pp", "potionstore_vile_size_antidote_pp"),
            PotionStoreSpec(
                725,
                "potionstore_antifire_potion",
                "potionstore_vile_size_antifire_potion",
            ),
            PotionStoreSpec(721, "potionstore_antipoison", "potionstore_vile_size_antipoison"),
            PotionStoreSpec(
                712,
                "potionstore_attack_potion",
                "potionstore_vile_size_attack_potion",
            ),
            PotionStoreSpec(
                1924,
                "potionstore_bastion_potion",
                "potionstore_vile_size_bastion_potion",
            ),
            PotionStoreSpec(
                1923,
                "potionstore_battlemage_potion",
                "potionstore_vile_size_battlemage_potion",
            ),
            PotionStoreSpec(
                2859,
                "potionstore_blighted_2restore_potion",
                "potionstore_vile_size_blighted_2restore_potion",
            ),
            PotionStoreSpec(
                732,
                "potionstore_combat_potion",
                "potionstore_vile_size_combat_potion",
            ),
            PotionStoreSpec(
                1019,
                "potionstore_compost_potion",
                "potionstore_vile_size_compost_potion",
            ),
            PotionStoreSpec(
                714,
                "potionstore_defence_potion",
                "potionstore_vile_size_defence_potion",
            ),
            PotionStoreSpec(
                878,
                "potionstore_divine_bastion_potion",
                "potionstore_vile_size_divine_bastion_potion",
            ),
            PotionStoreSpec(
                383,
                "potionstore_divine_battlemage_potion",
                "potionstore_vile_size_divine_battlemage_potion",
            ),
            PotionStoreSpec(
                2361,
                "potionstore_divine_magic_potion",
                "potionstore_vile_size_divine_magic_potion",
            ),
            PotionStoreSpec(
                2360,
                "potionstore_divine_ranging_potion",
                "potionstore_vile_size_divine_ranging_potion",
            ),
            PotionStoreSpec(
                2357,
                "potionstore_divine_super_attack_potion",
                "potionstore_vile_size_divine_super_attack_potion",
            ),
            PotionStoreSpec(
                2356,
                "potionstore_divine_super_combat_potion",
                "potionstore_vile_size_divine_super_combat_potion",
            ),
            PotionStoreSpec(
                2359,
                "potionstore_divine_super_defence_potion",
                "potionstore_vile_size_divine_super_defence_potion",
            ),
            PotionStoreSpec(
                2358,
                "potionstore_divine_super_strength_potion",
                "potionstore_vile_size_divine_super_strength_potion",
            ),
            PotionStoreSpec(
                726,
                "potionstore_energy_potion",
                "potionstore_vile_size_energy_potion",
            ),
            PotionStoreSpec(
                893,
                "potionstore_extended_antifire_potion",
                "potionstore_vile_size_extended_antifire_potion",
            ),
            PotionStoreSpec(
                4822,
                "potionstore_extended_antivenomplus_potion",
                "potionstore_vile_size_extended_antivenomplus_potion",
            ),
            PotionStoreSpec(
                1764,
                "potionstore_extended_super_antifire_potion",
                "potionstore_vile_size_extended_super_antifire_potion",
            ),
            PotionStoreSpec(
                717,
                "potionstore_fishing_potion",
                "potionstore_vile_size_fishing_potion",
            ),
            PotionStoreSpec(
                4916,
                "potionstore_forgottenbrew_potion",
                "potionstore_vile_size_forgottenbrew_potion",
            ),
            PotionStoreSpec(
                5572,
                "potionstore_goading_potion",
                "potionstore_vile_size_goading_potion",
            ),
            PotionStoreSpec(3599, "potionstore_surge_potion", "potionstore_vile_size_surge_potion"),
            PotionStoreSpec(
                1017,
                "potionstore_guthix_balance_potion",
                "potionstore_vile_size_guthix_balance_potion",
            ),
            PotionStoreSpec(
                731,
                "potionstore_hunter_potion",
                "potionstore_vile_size_hunter_potion",
            ),
            PotionStoreSpec(
                740,
                "potionstore_magicessence_potion",
                "potionstore_vile_size_magicessence_potion",
            ),
            PotionStoreSpec(730, "potionstore_magic_potion", "potionstore_vile_size_magic_potion"),
            PotionStoreSpec(
                4659,
                "potionstore_menaphiteremedy_potion",
                "potionstore_vile_size_menaphiteremedy_potion",
            ),
            PotionStoreSpec(
                5573,
                "potionstore_prayer_regeneration_potion",
                "potionstore_vile_size_prayer_regeneration_potion",
            ),
            PotionStoreSpec(
                715,
                "potionstore_prayer_potion",
                "potionstore_vile_size_prayer_potion",
            ),
            PotionStoreSpec(
                720,
                "potionstore_ranging_potion",
                "potionstore_vile_size_ranging_potion",
            ),
            PotionStoreSpec(
                735,
                "potionstore_relicymsbalm_potion",
                "potionstore_vile_size_relicymsbalm_potion",
            ),
            PotionStoreSpec(
                713,
                "potionstore_restore_potion",
                "potionstore_vile_size_restore_potion",
            ),
            PotionStoreSpec(
                905,
                "potionstore_sanfewserum_potion",
                "potionstore_vile_size_sanfewserum_potion",
            ),
            PotionStoreSpec(
                724,
                "potionstore_saradominbrew_potion",
                "potionstore_vile_size_saradominbrew_potion",
            ),
            PotionStoreSpec(
                736,
                "potionstore_serum207_potion",
                "potionstore_vile_size_serum207_potion",
            ),
            PotionStoreSpec(
                973,
                "potionstore_stamina_potion",
                "potionstore_vile_size_stamina_potion",
            ),
            PotionStoreSpec(
                711,
                "potionstore_strength_potion",
                "potionstore_vile_size_strength_potion",
            ),
            PotionStoreSpec(
                1794,
                "potionstore_super_antifire_potion",
                "potionstore_vile_size_super_antifire_potion",
            ),
            PotionStoreSpec(
                716,
                "potionstore_super_attack_potion",
                "potionstore_vile_size_super_attack_potion",
            ),
            PotionStoreSpec(
                334,
                "potionstore_super_combat_potion",
                "potionstore_vile_size_super_combat_potion",
            ),
            PotionStoreSpec(
                719,
                "potionstore_super_defence_potion",
                "potionstore_vile_size_super_defence_potion",
            ),
            PotionStoreSpec(
                727,
                "potionstore_super_energy_potion",
                "potionstore_vile_size_super_energy_potion",
            ),
            PotionStoreSpec(
                728,
                "potionstore_super_restore_potion",
                "potionstore_vile_size_super_restore_potion",
            ),
            PotionStoreSpec(
                718,
                "potionstore_super_strength_potion",
                "potionstore_vile_size_super_strength_potion",
            ),
            PotionStoreSpec(
                722,
                "potionstore_super_antipoison_potion",
                "potionstore_vile_size_super_antipoison_potion",
            ),
            PotionStoreSpec(
                4830,
                "potionstore_weapon_poison_potion",
                "potionstore_vile_size_weapon_poison_potion",
            ),
            PotionStoreSpec(
                4834,
                "potionstore_weapon_poison_p_potion",
                "potionstore_vile_size_weapon_poison_p_potion",
            ),
            PotionStoreSpec(
                4835,
                "potionstore_weapon_poison_pp_potion",
                "potionstore_vile_size_weapon_poison_pp_potion",
            ),
            PotionStoreSpec(
                723,
                "potionstore_zamorak_brew_potion",
                "potionstore_vile_size_zamorak_brew_potion",
            ),
            PotionStoreSpec(749, "potionstore_agility_mix", "potionstore_vile_size_agility_mix"),
            PotionStoreSpec(4084, "potionstore_ancient_mix", "potionstore_vile_size_ancient_mix"),
            PotionStoreSpec(759, "potionstore_antifire_mix", "potionstore_vile_size_antifire_mix"),
            PotionStoreSpec(
                743,
                "potionstore_antipoison_mix",
                "potionstore_vile_size_antipoison_mix",
            ),
            PotionStoreSpec(
                1021,
                "potionstore_antidote_p_mix",
                "potionstore_vile_size_antidote_p_mix",
            ),
            PotionStoreSpec(742, "potionstore_attack_mix", "potionstore_vile_size_attack_mix"),
            PotionStoreSpec(745, "potionstore_combat_mix", "potionstore_vile_size_combat_mix"),
            PotionStoreSpec(748, "potionstore_defence_mix", "potionstore_vile_size_defence_mix"),
            PotionStoreSpec(747, "potionstore_energy_mix", "potionstore_vile_size_energy_mix"),
            PotionStoreSpec(
                894,
                "potionstore_extended_antifire_mix",
                "potionstore_vile_size_extended_antifire_mix",
            ),
            PotionStoreSpec(
                1878,
                "potionstore_extended_super_antifire_mix",
                "potionstore_vile_size_extended_super_antifire_mix",
            ),
            PotionStoreSpec(753, "potionstore_fishing_mix", "potionstore_vile_size_fishing_mix"),
            PotionStoreSpec(762, "potionstore_hunting_mix", "potionstore_vile_size_hunting_mix"),
            PotionStoreSpec(
                756,
                "potionstore_magic_essence_mix",
                "potionstore_vile_size_magic_essence_mix",
            ),
            PotionStoreSpec(761, "potionstore_magic_mix", "potionstore_vile_size_magic_mix"),
            PotionStoreSpec(750, "potionstore_prayer_mix", "potionstore_vile_size_prayer_mix"),
            PotionStoreSpec(760, "potionstore_ranging_mix", "potionstore_vile_size_ranging_mix"),
            PotionStoreSpec(746, "potionstore_restore_mix", "potionstore_vile_size_restore_mix"),
            PotionStoreSpec(974, "potionstore_stamina_mix", "potionstore_vile_size_stamina_mix"),
            PotionStoreSpec(744, "potionstore_strength_mix", "potionstore_vile_size_strength_mix"),
            PotionStoreSpec(
                1795,
                "potionstore_super_antifire_mix",
                "potionstore_vile_size_super_antifire_mix",
            ),
            PotionStoreSpec(
                751,
                "potionstore_superattack_mix",
                "potionstore_vile_size_superattack_mix",
            ),
            PotionStoreSpec(
                758,
                "potionstore_super_defence_mix",
                "potionstore_vile_size_super_defence_mix",
            ),
            PotionStoreSpec(
                754,
                "potionstore_super_energy_mix",
                "potionstore_vile_size_super_energy_mix",
            ),
            PotionStoreSpec(
                757,
                "potionstore_super_restore_mix",
                "potionstore_vile_size_super_restore_mix",
            ),
            PotionStoreSpec(
                755,
                "potionstore_super_strength_mix",
                "potionstore_vile_size_super_strength_mix",
            ),
            PotionStoreSpec(
                752,
                "potionstore_antipoison_super_mix",
                "potionstore_vile_size_antipoison_super_mix",
            ),
            PotionStoreSpec(763, "potionstore_zamorak_mix", "potionstore_vile_size_zamorak_mix"),
            PotionStoreSpec(1020, "potionstore_relicyms_mix", "potionstore_vile_size_relicyms_mix"),
            PotionStoreSpec(4837, "potionstore_guamvial", null),
            PotionStoreSpec(4838, "potionstore_marrentillvial", null),
            PotionStoreSpec(4840, "potionstore_tarrominvial", null),
            PotionStoreSpec(5509, "potionstore_dwarfweedvial", null),
            PotionStoreSpec(4841, "potionstore_harralandervial", null),
            PotionStoreSpec(5589, "potionstore_huascavial", null),
            PotionStoreSpec(5503, "potionstore_iritvial", null),
            PotionStoreSpec(5505, "potionstore_kwuarmvial", null),
            PotionStoreSpec(5508, "potionstore_lantadymevial", null),
            PotionStoreSpec(5501, "potionstore_ranarrvial", null),
            PotionStoreSpec(5506, "potionstore_snapdragonvial", null),
            PotionStoreSpec(5502, "potionstore_toadflaxvial", null),
            PotionStoreSpec(5510, "potionstore_torstolvial", null),
            PotionStoreSpec(5504, "potionstore_avantoevial", null),
            PotionStoreSpec(5507, "potionstore_cadantinevial", null),
            PotionStoreSpec(5511, "potionstore_cadantine_bloodvial", null),
        )
}

public object PotionStoreVars : VarpReferences() {
    val counters = PotionStoreSpecs.all.map { find(it.counterName) }
    val vials = find("potionstore_vials")
    val bases = (1..6).map { find("potionstore_base_var_$it") }
    val favourites = (1..8).map { find("potionstore_favourite_$it") }
}

public object PotionStoreBits : VarBitReferences() {
    val preferred = PotionStoreSpecs.all.map { it.preferenceName?.let(::find) }
    val currentTab = find("bank_currenttab")
    val previousTab = find("potionstore_previous_tab")
    val always = find("bank_depositpotion")
}

public object PotionStoreComps : ComponentReferences() {
    val toggle = find("bankmain:potionstore_button")
    val items = find("bankmain:potionstore_items")
}

internal object PotionStoreSaves : VarpEditor() {
    init {
        for (save in
            PotionStoreVars.counters +
                PotionStoreVars.vials +
                PotionStoreVars.bases +
                PotionStoreVars.favourites) edit(save) { permanent = true }
    }
}
