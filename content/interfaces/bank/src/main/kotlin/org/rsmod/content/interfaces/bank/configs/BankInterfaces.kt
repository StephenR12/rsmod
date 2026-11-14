package org.rsmod.content.interfaces.bank.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

internal typealias bank_interfaces = BankInterfaces

internal typealias bank_components = BankComponents

internal typealias bank_comsubs = BankSubComponents

object BankInterfaces : InterfaceReferences() {
    val tutorial_overlay = find("screenhighlight", 1206696351)
}

object BankComponents : ComponentReferences() {
    val tutorial_button = find("bankmain:bank_tut", 7360148623500133599)
    val capacity_container = find("bankmain:capacity_layer", 5691092119508665688)
    val capacity_text = find("bankmain:capacity", 71329117100551007)
    val main_inventory = find("bankmain:items", 7094555693885599186)
    val tabs = find("bankmain:tabs", 3676966183117485051)
    val incinerator_confirm = find("bankmain:incinerator_confirm", 6103145201068339551)
    val potionstore_items = find("bankmain:potionstore_items", 8634889936909244044)
    val worn_off_stab = find("bankmain:stabatt", 7411826288201489950)
    val worn_off_slash = find("bankmain:slashatt", 2004225798056733861)
    val worn_off_crush = find("bankmain:crushatt", 5819997344766753580)
    val worn_off_magic = find("bankmain:magicatt", 9168677472510795206)
    val worn_off_range = find("bankmain:rangeatt", 3761076982366039117)
    val worn_speed_base = find("bankmain:attackspeedbase", 4635331154148677252)
    val worn_speed = find("bankmain:attackspeedactual", 2576410791747962788)
    val worn_def_stab = find("bankmain:stabdef", 4932131665381683125)
    val worn_def_slash = find("bankmain:slashdef", 8747903212091702844)
    val worn_def_crush = find("bankmain:crushdef", 3340302721946946755)
    val worn_def_range = find("bankmain:rangedef", 1281382359546232292)
    val worn_def_magic = find("bankmain:magicdef", 6688982849690988381)
    val worn_melee_str = find("bankmain:meleestrength", 2452437042561876300)
    val worn_ranged_str = find("bankmain:rangestrength", 6268208589271896019)
    val worn_magic_dmg = find("bankmain:magicdamage", 860608099127139930)
    val worn_prayer = find("bankmain:prayer", 4676379645837159649)
    val worn_undead = find("bankmain:typemultiplier", 3788513966452089192)
    val worn_slayer = find("bankmain:slayermultiplier", 7604285513162108911)
    val tutorial_overlay_target = find("bankmain:bank_highlight", 5345750290142623035)
    val confirmation_overlay_target = find("bankmain:popup", 7725483948440596960)
    val tooltip = find("bankmain:tooltip", 4370591860360031623)

    val rearrange_mode = find("bankmain:swap_insert")
    val withdraw_mode_note = find("bankmain:note", 1651873535979018581)
    val always_placehold = find("bankmain:placeholder", 8713600179453527191)
    val deposit_inventory = find("bankmain:depositinv", 2356503362278335427)
    val deposit_worn = find("bankmain:depositworn", 4351763597899652282)
    val quantity_1 = find("bankmain:quantity1", 204200291747623615)
    val quantity_5 = find("bankmain:quantity5", 3772955057254732222)
    val quantity_10 = find("bankmain:quantity10", 7929524431800327403)
    val quantity_x = find("bankmain:quantityx", 2490388573437703835)
    val quantity_all = find("bankmain:quantityall", 5436056834652600059)

    val incinerator_toggle = find("bankmain:incinerator_toggle", 8811722089281928309)
    val tutorial_button_toggle = find("bankmain:banktut_toggle", 2067562618570899398)
    val inventory_item_options_toggle = find("bankmain:sideops_toggle", 475733675136163027)
    val bank_item_options_toggle = find("bankmain:bankops_toggle")
    val deposit_inv_toggle = find("bankmain:depositinv_toggle", 8107276768556202465)
    val deposit_worn_toggle = find("bankmain:depositworn_toggle", 8107276768556202466)
    val release_placehold = find("bankmain:release_placeholders", 3296806909227800107)
    val bank_fillers_1 = find("bankmain:bank_filler_1", 900768247992960749)
    val bank_fillers_10 = find("bankmain:bank_filler_10", 3200502756228274450)
    val bank_fillers_50 = find("bankmain:bank_filler_50", 2767110765259747971)
    val bank_fillers_x = find("bankmain:bank_filler_x", 575354396200290974)
    val bank_fillers_all = find("bankmain:bank_filler_all", 4729142482797154929)
    val bank_fillers_fill = find("bankmain:bank_filler_confirm", 8201018915889475344)
    val bank_tab_display = find("bankmain:dropdown_content", 428114165262132213)

    val side_inventory = find("bankside:items", 1885880344080200061)
    val worn_inventory = find("bankside:wornops", 6203990611586493264)
    val lootingbag_inventory = find("bankside:lootingbag_items", 8800055068705330501)
    val league_inventory = find("bankside:league_secondinv_items", 81253577765913503)
    val bankside_highlight = find("bankside:bankside_highlight", 6202930921141607019)

    val tutorial_close_button = find("screenhighlight:pausebutton", 8373824249352593324)
    val tutorial_next_page = find("screenhighlight:continue", 2368578001968595651)
    val tutorial_prev_page = find("screenhighlight:previous", 7461125518300620858)
}

@Suppress("ConstPropertyName")
object BankSubComponents {
    const val main_tab = 10
    val other_tabs = 11..19

    val tab_extended_slots_offset = 19..28
}
