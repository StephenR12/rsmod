package org.rsmod.content.generic.npcs.farmer

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.type.npc.NpcType

/** Final pre-cache-233-cutoff combat overrides for the five Farmers loaded in the world. */
internal object FarmerNpcEdits : NpcEditor() {
    private val farmerTypes: Set<NpcType> =
        setOf(npcs.farmer, npcs.farmer_3, npcs.farmer_2_f, npcs.farmer_3_f, npcs.farmer_4)

    init {
        farmerTypes.forEach { type ->
            edit(type) {
                respawnRate = 25
                param[params.attackrate] = 6
                param[params.npc_attack_type] = categories.attacktype_stab
                param[params.attack_stab] = 5
                param[params.melee_strength] = 6
            }
        }
    }
}
