package org.rsmod.api.death

import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.game.type.obj.ObjType

/** Shared cache-233 herb table used as a nested branch by production NPC drop tables. */
public object NpcHerbDropRolls {
    /** Returns the exact 128-weight standard grimy-herb table. */
    public fun standard(): NpcDropRoll =
        standard(
            listOf(
                objs.grimy_guam_leaf,
                objs.grimy_marrentill,
                objs.grimy_tarromin,
                objs.grimy_harralander,
                objs.grimy_ranarr_weed,
                objs.grimy_irit_leaf,
                objs.grimy_avantoe,
                objs.grimy_kwuarm,
                objs.grimy_cadantine,
                objs.grimy_lantadyme,
                objs.grimy_dwarf_weed,
            )
        )

    internal fun standard(herbs: List<ObjType>): NpcDropRoll {
        require(herbs.size == WEIGHTS.size) {
            "Standard herb table requires ${WEIGHTS.size} entries: ${herbs.size}"
        }
        return weighted(herbs.zip(WEIGHTS) { herb, weight -> NpcWeightedDrop(weight, item(herb)) })
    }

    private val WEIGHTS: List<Int> = listOf(32, 24, 18, 14, 11, 8, 6, 5, 4, 3, 3)
}
