package org.rsmod.api.player.magic

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

/** Server-authoritative lifetime and protection roll for Shadow Veil. */
@Singleton
public class ShadowVeilStateRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player, duration: Int) {
        require(duration > 0) { "Shadow Veil duration must be positive: $duration" }
        expiresAt[player] = player.currentMapClock + duration
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    /** Rolls before any later failed-pickpocket protection, such as a dodgy necklace. */
    public fun protects(player: Player, random: GameRandom): Boolean {
        return isActive(player) && random.of(100) < PROTECTION_PERCENT
    }

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

    public companion object {
        public const val PROTECTION_PERCENT: Int = 15
    }
}
