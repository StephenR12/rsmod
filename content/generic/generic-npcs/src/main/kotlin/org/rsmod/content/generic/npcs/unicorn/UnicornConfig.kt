package org.rsmod.content.generic.npcs.unicorn

import org.rsmod.api.config.refs.npcs
import org.rsmod.api.type.editors.npc.NpcEditor

/** Final pre-cache-233-cutoff respawn override for the loaded ordinary Unicorn. */
internal object UnicornNpcEdits : NpcEditor() {
    init {
        edit(npcs.unicorn) { respawnRate = 90 }
    }
}
