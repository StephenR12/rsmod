package org.rsmod.content.skills.herblore

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Shared cache-233 delay state for non-barbarian potion consumption. */
@Singleton
public class PotionConsumptionRepository {
    private val nextPotionAt = WeakHashMap<Player, Int>()

    public fun isReady(player: Player): Boolean =
        (nextPotionAt[player] ?: Int.MIN_VALUE) <= player.currentMapClock

    public fun consume(player: Player, delay: Int) {
        require(delay >= 0) { "Potion delay must not be negative." }
        nextPotionAt[player] = player.currentMapClock + delay
    }

    public fun clear(player: Player) {
        nextPotionAt.remove(player)
    }

    public fun remainingCycles(player: Player): Int =
        ((nextPotionAt[player] ?: player.currentMapClock) - player.currentMapClock).coerceAtLeast(0)
}
