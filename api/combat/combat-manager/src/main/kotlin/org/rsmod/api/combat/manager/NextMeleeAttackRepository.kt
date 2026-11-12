package org.rsmod.api.combat.manager

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** One-shot modifiers armed by specials such as Virulence and Entice. */
@Singleton
public class NextMeleeAttackRepository {
    private val effects = WeakHashMap<Player, Effect>()

    public fun guarantee(player: Player, duration: Int) {
        require(duration > 0) { "Duration must be positive: $duration" }
        val current = activeEffect(player)
        effects[player] =
            Effect(
                minimumDamage = current?.minimumDamage ?: 0,
                guaranteed = true,
                expiresAt = maxOf(current?.expiresAt ?: 0, player.currentMapClock + duration),
            )
    }

    public fun setMinimumDamage(player: Player, damage: Int, duration: Int) {
        require(damage >= 0) { "Minimum damage must not be negative: $damage" }
        require(duration > 0) { "Duration must be positive: $duration" }
        val current = activeEffect(player)
        effects[player] =
            Effect(
                minimumDamage = maxOf(current?.minimumDamage ?: 0, damage),
                guaranteed = current?.guaranteed == true,
                expiresAt = maxOf(current?.expiresAt ?: 0, player.currentMapClock + duration),
            )
    }

    /** Removes and returns the modifier for the next melee accuracy/damage roll. */
    public fun consume(player: Player): Effect? {
        val effect = activeEffect(player) ?: return null
        effects.remove(player)
        return effect
    }

    private fun activeEffect(player: Player): Effect? {
        val effect = effects[player] ?: return null
        if (effect.expiresAt <= player.currentMapClock) {
            effects.remove(player)
            return null
        }
        return effect
    }

    public data class Effect(
        public val minimumDamage: Int,
        public val guaranteed: Boolean,
        public val expiresAt: Int,
    )
}
