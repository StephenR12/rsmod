package org.rsmod.content.skills.magic.spell.attacks.ancient

import jakarta.inject.Inject
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.util.Bounds

public class AncientTargetSelector
@Inject
constructor(private val npcs: NpcList, private val players: PlayerList) {
    public fun targets(
        primary: PathingEntity,
        multiTarget: Boolean,
        multiway: Boolean,
    ): List<PathingEntity> {
        if (!multiTarget || !multiway) return listOf(primary)

        // Official AoE is a 3x3 square centred on the primary target's south-west origin tile.
        val area = Bounds(primary.x - 1, primary.z - 1, primary.level, 3, 3)
        val candidates =
            when (primary) {
                is Npc -> npcs.asSequence().filterNotNull().filter { it.isValidTarget() }
                is Player -> players.asSequence().filterNotNull().filter { it.isValidTarget() }
            }
        return sequenceOf(primary)
            .plus(candidates.filter { it !== primary && intersects(it.bounds(), area) })
            .take(MAX_TARGETS)
            .toList()
    }

    private fun intersects(candidate: Bounds, area: Bounds): Boolean =
        candidate.level == area.level && candidate.isWithinDistance(area, 0)

    private companion object {
        const val MAX_TARGETS = 9
    }
}
