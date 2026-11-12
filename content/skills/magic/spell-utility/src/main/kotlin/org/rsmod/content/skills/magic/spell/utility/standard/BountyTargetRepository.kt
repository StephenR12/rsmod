package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid

/**
 * Session state shared with the future Bounty Hunter content plugin.
 *
 * The minigame owns matchmaking and location transitions. Teleport to Target consumes this small,
 * explicit contract so that the spell does not duplicate Bounty Hunter rules or infer arena state
 * from brittle coordinate rectangles.
 */
@Singleton
public class BountyTargetRepository @Inject constructor() {
    private val assignments = WeakHashMap<Player, BountyTargetAssignment>()
    private val locations = WeakHashMap<Player, BountyHunterLocation>()

    public fun assign(player: Player, target: Player) {
        assignments[player] = BountyTargetAssignment(target.uid, teleportsUsed = 0)
        VarPlayerIntMapSetter.set(player, varbits.bounty_teleport_quota, 0)
        VarPlayerIntMapSetter.set(player, varbits.bounty_target_limit, 0)
    }

    public fun assignment(player: Player): BountyTargetAssignment? = assignments[player]

    public fun location(player: Player): BountyHunterLocation? = locations[player]

    public fun setLocation(player: Player, location: BountyHunterLocation) {
        locations[player] = location
    }

    public fun recordTeleport(player: Player): Int {
        val current = requireNotNull(assignments[player]) { "Player has no Bounty Hunter target." }
        val used = current.teleportsUsed + 1
        assignments[player] = current.copy(teleportsUsed = used)
        VarPlayerIntMapSetter.set(player, varbits.bounty_teleport_quota, used)
        VarPlayerIntMapSetter.set(
            player,
            varbits.bounty_target_limit,
            if (used >= MAX_TELEPORTS_PER_TARGET) 1 else 0,
        )
        return used
    }

    public fun clearAssignment(player: Player) {
        assignments.remove(player)
        VarPlayerIntMapSetter.set(player, varbits.bounty_teleport_quota, 0)
        VarPlayerIntMapSetter.set(player, varbits.bounty_target_limit, 0)
    }

    public fun clear(player: Player) {
        val uid = player.uid
        clearAssignment(player)
        locations.remove(player)
        assignments
            .filterValues { assignment -> assignment.target == uid }
            .keys
            .toList()
            .forEach(::clearAssignment)
    }

    public companion object {
        public const val MAX_TELEPORTS_PER_TARGET: Int = 5
    }
}

public data class BountyTargetAssignment(
    public val target: PlayerUid,
    public val teleportsUsed: Int,
)

public enum class BountyHunterLocation {
    Lobby,
    Crater,
}
