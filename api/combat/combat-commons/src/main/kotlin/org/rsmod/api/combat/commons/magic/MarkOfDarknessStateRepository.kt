package org.rsmod.api.combat.commons.magic

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Server-authoritative lifetime for the caster-side Mark of Darkness buff. */
@Singleton
public class MarkOfDarknessStateRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player, duration: Int) {
        require(duration > 0) { "Mark of Darkness duration must be positive: $duration" }
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
}
