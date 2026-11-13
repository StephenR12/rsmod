package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.config.refs.stats
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MudskipperPoint
@Inject
constructor(
    private val types: LocTypeList,
    private val items: ObjTypeList,
    private val monsters: NpcTypeList,
    private val repository: NpcRepository,
) : PluginScript() {
    // The fishing spots are in water. These are the adjacent 2x2 clear stretches of shore.
    private val shore = listOf(CoordGrid(2984, 3112), CoordGrid(2995, 3108), CoordGrid(3002, 3117))

    override fun ScriptContext.startup() {
        val explosive = items.getValue(6664)
        for ((i, pos) in shore.withIndex()) {
            val spot = types.getValue(10087 + i)
            onOpLoc1(spot) { mes("Something seems to have scared all the fishes away...") }
            onOpLoc2(spot) { mes("Something seems to have scared all the fishes away...") }
            onOpLocU(spot, explosive) { mes("Stand further back before throwing the explosive.") }
            onApLocU(spot, explosive) {
                if (!isWithinApRange(it.loc, 8)) return@onApLocU
                if (isWithinDistance(it.loc, 2)) {
                    mes("Stand further back before throwing the explosive.")
                    return@onApLocU
                }
                if (
                    !player.members ||
                        vars[DungeonJournalBits.mogres] < 3 ||
                        stat(stats.slayer) < 32
                ) {
                    mes("You haven't learned to hunt mogres.")
                    return@onApLocU
                }
                if (!invDel(inv, explosive, slot = it.invSlot).success) return@onApLocU
                val mogre = Npc(monsters.getValue(2592), pos)
                repository.add(mogre, 300)
                mogre.queueCombatRetaliate(player)
            }
        }
    }
}
