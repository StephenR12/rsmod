package org.rsmod.api.player.magic

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Server-authoritative lifetime for the Staff of the Dead melee-protection special. */
@Singleton
public class StaffOfTheDeadStateRepository {
    public fun activate(player: Player, duration: Int) {
        require(duration > 0) { "Staff protection duration must be positive: $duration" }
        expiresAt[player] = player.currentMapClock + duration
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
