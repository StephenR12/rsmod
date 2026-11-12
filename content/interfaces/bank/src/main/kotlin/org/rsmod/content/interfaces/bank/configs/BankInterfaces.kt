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
    val tutorial_button = find("bankmain:bank_tut", 7135446288139269005)
    val capacity_container = find("bankmain:capacity_layer", 5691092119508665688)
    val capacity_text = find("bankmain:capacity", 71329117100551008)
    val main_inventory = find("bankmain:items", 8484261777138475560)
    val tabs = find("bankmain:tabs", 5066672266370361425)
    val incinerator_confirm = find("bankmain:incinerator_confirm", 7492851284321215925)
    val potionstore_items = find("bankmain:potionstore_items", 801223983307344610)
    val worn_off_stab = find("bankmain:stabatt", 5368284553245019773)
    val worn_off_slash = find("bankmain:slashatt", 6650696305357172442)
    val worn_off_crush = find("bankmain:crushatt", 1243095815212416353)
    val worn_off_magic = find("bankmain:magicatt", 5058867361922436072)
    val worn_off_range = find("bankmain:rangeatt", 4591775942956457980)
    val worn_speed_base = find("bankmain:attackspeedbase", 9004121245991399539)
    val worn_speed = find("bankmain:attackspeedactual", 3874201171304359744)
    val worn_def_stab = find("bankmain:stabdef", 473492763335668764)
    val worn_def_slash = find("bankmain:slashdef", 4171001682537365617)
    val worn_def_crush = find("bankmain:crushdef", 7986773229247385336)
    val worn_def_range = find("bankmain:rangedef", 2112081320136651155)
    val worn_def_magic = find("bankmain:magicdef", 2579172739102629247)
    val worn_melee_str = find("bankmain:meleestrength", 8070076729761208913)
    val worn_ranged_str = find("bankmain:rangestrength", 1691307059717558792)
    val worn_magic_dmg = find("bankmain:magicdamage", 5507078606427578511)
    val worn_prayer = find("bankmain:prayer", 99478116282822422)
    val worn_undead = find("bankmain:typemultiplier", 2033646581317590566)
    val worn_slayer = find("bankmain:slayermultiplier", 3027383983607771684)
    val tutorial_overlay_target = find("bankmain:bank_highlight", 8347293645791880928)
    val confirmation_overlay_target = find("bankmain:popup", 5345750290142623061)
    val tooltip = find("bankmain:tooltip", 4320937687314686090)

    val rearrange_mode_swap = find("bankmain:swap", 7319232678072104634)
    val rearrange_mode_insert = find("bankmain:insert", 6869961168519985648)
    val withdraw_mode_item = find("bankmain:item", 2510729662576342869)
    val withdraw_mode_note = find("bankmain:note", 3245570119814374912)
    val always_placehold = find("bankmain:placeholder", 5373906735621325153)
    val deposit_inventory = find("bankmain:depositinv", 2621356993398576010)
    val deposit_worn = find("bankmain:depositworn", 4616617229019892865)
    val quantity_1 = find("bankmain:quantity1", 7134228484220256887)
    val quantity_5 = find("bankmain:quantity5", 3565830576145944234)
    val quantity_10 = find("bankmain:quantity10", 585247277110118155)
    val quantity_x = find("bankmain:quantityx", 6455702818875624943)
    val quantity_all = find("bankmain:quantityall", 2264218406509099907)

    val incinerator_toggle = find("bankmain:incinerator_toggle", 2580629615254100013)
    val tutorial_button_toggle = find("bankmain:banktut_toggle", 3457268701823775704)
    val inventory_item_options_toggle = find("bankmain:sideops_toggle", 1865439758389039335)
    val bank_item_options_toggle = find("bankmain:bankops_toggle")
    val deposit_inv_toggle = find("bankmain:depositinv_toggle", 273610814954302965)
    val deposit_worn_toggle = find("bankmain:depositworn_toggle", 273610814954302966)
    val release_placehold = find("bankmain:release_placeholders", 7905153908374342404)
    val bank_fillers_1 = find("bankmain:bank_filler_1", 8541127422168111406)
    val bank_fillers_10 = find("bankmain:bank_filler_10", 3582900509382314945)
    val bank_fillers_50 = find("bankmain:bank_filler_50", 4844269236865183160)
    val bank_fillers_x = find("bankmain:bank_filler_x", 7266842139852129837)
    val bank_fillers_all = find("bankmain:bank_filler_all", 6238430592236396587)
    val bank_fillers_fill = find("bankmain:bank_filler_confirm", 5394895824129949685)
    val bank_tab_display = find("bankmain:dropdown_content", 3645899373675108237)

    val side_inventory = find("bankside:items", 1885880344080200061)
    val worn_inventory = find("bankside:wornops", 6203990611586493264)
    val lootingbag_inventory = find("bankside:lootingbag_items", 8800055068705330501)
    val league_inventory = find("bankside:league_secondinv_items", 81253577765913503)
    val bankside_highlight = find("bankside:bankside_highlight", 3015669162793304897)

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
