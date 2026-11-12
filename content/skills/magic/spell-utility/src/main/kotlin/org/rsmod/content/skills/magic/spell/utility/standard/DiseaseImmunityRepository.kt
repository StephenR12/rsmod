package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Session-local fifteen-minute disease immunity granted by cache-233 Sanfew serum. */
@Singleton
public class DiseaseImmunityRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player) {
        expiresAt[player] = maxOf(expiresAt[player] ?: 0, player.currentMapClock + DURATION_CYCLES)
    }

    public fun isImmune(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val remaining = (expiresAt[player] ?: return 0) - player.currentMapClock
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
        public const val DURATION_CYCLES: Int = 1_500
    }
}
