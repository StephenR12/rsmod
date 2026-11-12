package org.rsmod.content.generic.npcs.giant_rat

import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.type.npc.NpcType

/** Final pre-cache-233-cutoff production overrides for the loaded regular Giant rats. */
internal object GiantRatNpcEdits : NpcEditor() {
    private val loadedTypes =
        setOf(
            npcs.giant_rat,
            npcs.giant_rat_grey,
            npcs.giant_rat_grey_2,
            npcs.giant_rat_grey_3,
            npcs.giant_rat_level_6,
            npcs.giant_rat_level_6_2,
        )

    init {
        loadedTypes.forEach(::editGiantRat)
    }

    private fun editGiantRat(type: NpcType) {
        edit(type) {
            respawnRate = 30
            huntMode = huntmodes.constant_melee
        }
    }
}
