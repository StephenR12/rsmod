package org.rsmod.content.generic.npcs.ram

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.type.npc.NpcType

public val RAM_TYPES: List<NpcType> =
    listOf(npcs.ram, npcs.ram_2, npcs.ram_3, npcs.ram_monastery, npcs.ram_sheared)

/** Final pre-cache-233-cutoff respawn and elemental-weakness overrides for every Ram. */
internal object RamNpcEdits : NpcEditor() {
    init {
        RAM_TYPES.forEach { type ->
            edit(type) {
                respawnRate = 45
                param[params.elemental_weakness_type] = constants.elemental_weakness_fire
                param[params.elemental_weakness_percent] = 20
            }
        }
    }
}
