package org.rsmod.api.player.magic

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Server-authoritative lifetime for the defensive effects granted by Ward of Arceuus. */
@Singleton
public class WardOfArceuusStateRepository {
    public fun activate(player: Player, duration: Int) {
        require(duration > 0) { "Ward of Arceuus duration must be positive: $duration" }
        expiresAt[player] = player.currentMapClock + duration
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val expiry = expiresAt[player] ?: return 0
        val remaining = expiry - player.currentMapClock
        if (remaining <= 0) {
            expiresAt.remove(player)
            return 0
        }
        return remaining
    }

    public fun clear(player: Player) {
        expiresAt.remove(player)
    }

    internal companion object {
        private val expiresAt = WeakHashMap<Player, Int>()

        fun isActiveForHitModifier(player: Player): Boolean {
            val expiry = expiresAt[player] ?: return false
            if (expiry <= player.currentMapClock) {
                expiresAt.remove(player)
                return false
            }
            return true
        }
    }
}
