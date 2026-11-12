package org.rsmod.content.generic.npcs.goblin

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.type.npc.NpcType

/** Final pre-cache-233-cutoff combat overrides for the nine loaded ordinary Goblins. */
internal object GoblinNpcEdits : NpcEditor() {
    private val unarmedTypes: Set<NpcType> =
        setOf(
            npcs.goblin_unarmed_1,
            npcs.goblin_unarmed_2,
            npcs.goblin_unarmed_3,
            npcs.goblin_unarmed_4,
            npcs.goblin_unarmed_5,
            npcs.goblin_unarmed_6,
            npcs.goblin_unarmed_7,
            npcs.goblin_unarmed_8,
        )

    init {
        unarmedTypes.forEach { type ->
            edit(type) {
                respawnRate = 35
                param[params.attackrate] = 4
                param[params.npc_attack_type] = categories.attacktype_crush
            }
        }
        edit(npcs.goblin_level_5) {
            respawnRate = 35
            param[params.attackrate] = 6
            param[params.npc_attack_type] = categories.attacktype_stab
        }
    }
}
