package org.rsmod.content.skills.cooking

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Shared cache-233 food-consumption and attack-delay state. */
@Singleton
public class FoodConsumptionRepository {
    private val nextFoodAt = WeakHashMap<Player, Int>()
    private val nextComboAt = WeakHashMap<Player, Int>()

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

    public fun isComboReady(player: Player): Boolean =
        (nextComboAt[player] ?: Int.MIN_VALUE) <= player.currentMapClock

    public fun consumeCombo(player: Player) {
        nextComboAt[player] = player.currentMapClock + 3
        consume(player, eatDelay = 3, attackDelay = 2)
    }

    public fun clear(player: Player) {
        nextComboAt.remove(player)
        nextFoodAt.remove(player)
    }

    public fun remainingCycles(player: Player): Int =
        ((nextFoodAt[player] ?: player.currentMapClock) - player.currentMapClock).coerceAtLeast(0)
}
