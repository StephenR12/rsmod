package org.rsmod.api.combat.commons

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.combat.commons.npc.requiresSlayerAssignment
import org.rsmod.api.combat.commons.npc.slayerSuperiorOwner
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.events.canHitNearbyPlayer
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.stat
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.util.Bounds

public class AreaAttackTargets
@Inject
constructor(private val npcs: NpcList, private val players: PlayerList, private val bus: EventBus) {
    public fun targets(
        caster: Player,
        primary: PathingEntity,
        multiTarget: Boolean,
        multiway: Boolean,
    ): List<PathingEntity> {
        if (!multiTarget || !multiway) return listOf(primary)

        // Official AoE is a 3x3 square centred on the primary target's south-west origin tile.
        val area = Bounds(primary.x - 1, primary.z - 1, primary.level, 3, 3)
        val candidates =
            when (primary) {
                is Npc ->
                    npcs.asSequence().filterNotNull().filter {
                        it.isValidTarget() &&
                            it.visType.op.any { op -> op.equals("Attack", true) } &&
                            caster.stat(stats.slayer) >=
                                it.visType.param(params.slayer_levelrequire) &&
                            (it.slayerSuperiorOwner == null ||
                                it.slayerSuperiorOwner == caster.uid) &&
                            (!it.requiresSlayerAssignment(caster) ||
                                it.isAssignedSlayerTarget(caster))
                    }
                is Player ->
                    players.asSequence().filterNotNull().filter {
                        it !== caster && it.isValidTarget() && caster.canHitNearbyPlayer(it, bus)
                    }
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
