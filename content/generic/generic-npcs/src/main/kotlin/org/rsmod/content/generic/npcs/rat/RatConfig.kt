package org.rsmod.content.generic.npcs.rat

import org.rsmod.api.config.refs.npcs
import org.rsmod.api.type.editors.npc.NpcEditor

/** Final pre-cache-233-cutoff production override for the loaded regular Rat. */
internal object RatNpcEdits : NpcEditor() {
    init {
        edit(npcs.rat) { respawnRate = 2 }
    }
}
