package org.rsmod.content.interfaces.bank

import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

public class BankProximity
@Inject
constructor(
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val types: LocTypeList,
) {
    public fun nearBank(coords: CoordGrid): Boolean {
        for (dx in -4..4) for (dz in -4..4) {
            for (loc in locs.findAll(coords.translate(dx, dz))) {
                val type = types.getValue(loc.id)
                if (
                    type.op.any { it.equals("Bank", true) } ||
                        type.name.contains("bank chest", true) &&
                            type.op.any { it.equals("Use", true) }
                )
                    return true
            }
        }
        return npcs.findAll(ZoneKey.from(coords), 1).any { npc ->
            npc.coords.level == coords.level &&
                npc.coords.chebyshevDistance(coords) <= 4 &&
                npc.type.op.any { it.equals("Bank", true) }
        }
    }
}
