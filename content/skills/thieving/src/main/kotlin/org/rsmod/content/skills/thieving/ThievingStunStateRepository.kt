package org.rsmod.content.skills.thieving

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.game.entity.Player

@Singleton
public class ThievingStunStateRepository {
    private val movementHeldUntil = WeakHashMap<Player, Int>()

    public fun stun(player: Player, movementCycles: Int) {
        require(movementCycles > 0) { "Movement stun duration must be positive." }
        movementHeldUntil[player] = player.currentMapClock + movementCycles
        blockMovement(player)
    }

    public fun blockMovement(player: Player) {
        if (!isMovementBlocked(player)) {
            if (player.walkTrigger == walktriggers.stunned) {
                player.clearWalkTrigger()
            }
            return
        }
        player.abortRoute()
        player.clearMapFlag()
        player.walkTrigger(walktriggers.stunned)
    }

    public fun isMovementBlocked(player: Player): Boolean {
        val expiry = movementHeldUntil[player] ?: return false
        if (player.currentMapClock < expiry) {
            return true
        }
        movementHeldUntil.remove(player)
        return false
    }

    public fun remainingCycles(player: Player): Int =
        ((movementHeldUntil[player] ?: player.currentMapClock) - player.currentMapClock)
            .coerceAtLeast(0)
}
