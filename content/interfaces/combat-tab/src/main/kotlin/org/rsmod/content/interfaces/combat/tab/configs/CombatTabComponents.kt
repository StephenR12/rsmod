package org.rsmod.content.interfaces.combat.tab.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias combat_components = CombatTabComponents

object CombatTabComponents : ComponentReferences() {
    val stance1 = find("combat_interface:0", 2858390886771847764)
    val stance2 = find("combat_interface:1", 2437616626352755402)
    val stance3 = find("combat_interface:2", 4786378263585440965)
    val stance4 = find("combat_interface:3", 4365604003166348603)
    val auto_retaliate = find("combat_interface:retaliate", 7472549650049137011)
    val special_attack = find("combat_interface:special_attack", 6807823470789281920)
    val autocast_normal = find("combat_interface:autocast_normal")
    val autocast_defensive = find("combat_interface:autocast_defensive")
    val autocast_spells = find("autocast:spells")

    val special_attack_orb = find("orbs:specbutton", 6290097952827873827)
}
