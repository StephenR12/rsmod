package org.rsmod.content.skills.cooking

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Shared cache-233 food-consumption and attack-delay state. */
@Singleton
public class FoodConsumptionRepository {
    private val nextFoodAt = WeakHashMap<Player, Int>()

    public fun isReady(player: Player): Boolean =
        (nextFoodAt[player] ?: Int.MIN_VALUE) <= player.currentMapClock

    public fun consume(player: Player, eatDelay: Int, attackDelay: Int) {
        require(eatDelay >= 0) { "Eat delay must not be negative." }
        require(attackDelay >= 0) { "Attack delay must not be negative." }
        nextFoodAt[player] = player.currentMapClock + eatDelay
        if (player.actionDelay > player.currentMapClock) {
            player.actionDelay += attackDelay
        }
    }

    public fun clear(player: Player) {
        nextFoodAt.remove(player)
    }

    public fun remainingCycles(player: Player): Int =
        ((nextFoodAt[player] ?: player.currentMapClock) - player.currentMapClock).coerceAtLeast(0)
}
