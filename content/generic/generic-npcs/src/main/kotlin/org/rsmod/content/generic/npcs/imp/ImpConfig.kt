package org.rsmod.content.generic.npcs.imp

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor

/** Final pre-cache-233-cutoff production overrides for the loaded ordinary Imp. */
internal object ImpNpcEdits : NpcEditor() {
    init {
        edit(npcs.imp) {
            respawnRate = 50
            param[params.elemental_weakness_type] = constants.elemental_weakness_water
            param[params.elemental_weakness_percent] = 10
        }
    }
}
