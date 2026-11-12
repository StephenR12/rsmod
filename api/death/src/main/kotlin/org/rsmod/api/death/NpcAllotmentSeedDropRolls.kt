package org.rsmod.api.death

import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.weighted
import org.rsmod.game.type.obj.ObjType

/** Shared final pre-cache-233-cutoff allotment-seed table for NPC drop tables. */
public object NpcAllotmentSeedDropRolls {
    /** Returns the exact 128-weight allotment-seed table. */
    public fun standard(): NpcDropRoll =
        standard(
            listOf(
                SeedEntry(objs.potato_seed, 1..4),
                SeedEntry(objs.onion_seed, 1..3),
                SeedEntry(objs.cabbage_seed, 1..3),
                SeedEntry(objs.tomato_seed, 1..2),
                SeedEntry(objs.sweetcorn_seed, 1..2),
                SeedEntry(objs.strawberry_seed, 1..1),
                SeedEntry(objs.watermelon_seed, 1..1),
                SeedEntry(objs.snape_grass_seed, 1..3),
            )
        )

    internal fun standard(seeds: List<SeedEntry>): NpcDropRoll {
        require(seeds.size == WEIGHTS.size) {
            "Allotment-seed table requires ${WEIGHTS.size} entries: ${seeds.size}"
        }
        return weighted(
            seeds.zip(WEIGHTS) { seed, weight ->
                NpcWeightedDrop(weight, item(seed.type, seed.quantity))
            }
        )
    }

    internal data class SeedEntry(val type: ObjType, val quantity: IntRange)

    private val WEIGHTS: List<Int> = listOf(64, 32, 16, 8, 4, 2, 1, 1)
}
