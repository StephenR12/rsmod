package org.rsmod.content.generic.npcs.frog

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor

/** Final pre-cache-233-cutoff production overrides for the level-five Frog. */
internal object FrogNpcEdits : NpcEditor() {
    init {
        edit(npcs.frog) {
            respawnRate = 50
            param[params.elemental_weakness_type] = constants.elemental_weakness_earth
            param[params.elemental_weakness_percent] = 40
        }
    }
}
