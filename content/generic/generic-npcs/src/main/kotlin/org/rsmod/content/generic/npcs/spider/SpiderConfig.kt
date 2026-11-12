package org.rsmod.content.generic.npcs.spider

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor

/** Final pre-cache-233-cutoff production overrides for the two loaded Spider identities. */
internal object SpiderNpcEdits : NpcEditor() {
    init {
        edit(npcs.spider) {
            respawnRate = 35
            param[params.elemental_weakness_type] = constants.elemental_weakness_fire
            param[params.elemental_weakness_percent] = 50
        }
        edit(npcs.giant_spider) {
            respawnRate = 30
            huntMode = huntmodes.constant_melee
            param[params.elemental_weakness_type] = constants.elemental_weakness_fire
            param[params.elemental_weakness_percent] = 50
        }
    }
}
