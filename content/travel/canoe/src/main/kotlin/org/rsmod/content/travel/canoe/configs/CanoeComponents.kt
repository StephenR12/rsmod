package org.rsmod.content.travel.canoe.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias canoe_components = CanoeComponents

object CanoeComponents : ComponentReferences() {
    val shape_log = find("canoeing:log", 5540955978932720971)
    val shape_dugout = find("canoeing:dugout", 574740175170890111)
    val shape_stable_dugout = find("canoeing:stable_dugout", 5904149818566193745)
    val shape_waka = find("canoeing:waka", 558687581070453046)
    val shape_close = find("canoeing:close", 1179288482004399632)

    val destination_lumbridge = find("canoe_map_lum:destination_1")
    val destination_champs_guild = find("canoe_map_lum:destination_2")
    val destination_barb_village = find("canoe_map_lum:destination_3")
    val destination_edgeville = find("canoe_map_lum:destination_4")
    val destination_ferox_enclave = find("canoe_map_lum:destination_5")
    val destination_wild_pond = find("canoe_map_lum:destination_6")
}
