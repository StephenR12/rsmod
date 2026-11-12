package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor

internal object BarbarianPyreNpcEditor : NpcEditor() {
    init {
        edit(npcs.brut_angry_spirit_bad) {
            vislevel = 166
            attack = 150
            strength = 140
            defence = 100
            hitpoints = 190
            magic = 1
            ranged = 1
            param[params.attackrate] = 4
            param[params.attack_melee] = 9
            param[params.melee_strength] = 15
            param[params.defence_stab] = 0
            param[params.defence_slash] = 73
            param[params.defence_crush] = 72
            param[params.defence_magic] = -3
            param[params.defence_ranged] = 30
            param[params.defence_standard] = 30
            param[params.defence_light] = 72
            param[params.defence_heavy] = 72
            param[params.npc_attack_type] = categories.attacktype_crush
            param[params.elemental_weakness_type] = constants.elemental_weakness_wind
            param[params.elemental_weakness_percent] = 10
        }
    }
}
