package org.rsmod.api.player.stat

import java.util.WeakHashMap
import org.rsmod.game.entity.Player

private data class IncomingHealingState(val percent: Int, val until: Int)

/** Temporary modifiers applied centrally to healing received by a player. */
public object IncomingHealingModifier {
    private val states = WeakHashMap<Player, IncomingHealingState>()

    public fun reduce(player: Player, reductionPercent: Int, duration: Int) {
        require(reductionPercent in 0..100)
        require(duration > 0)
        val incoming = IncomingHealingState(reductionPercent, player.currentMapClock + duration)
        val current = activeState(player)
        states[player] =
            if (current == null) {
                incoming
            } else {
                IncomingHealingState(
                    percent = maxOf(current.percent, incoming.percent),
                    until = maxOf(current.until, incoming.until),
                )
            }
    }

    public fun modify(player: Player, amount: Int): Int {
        val state = activeState(player) ?: return amount
        return amount * (100 - state.percent) / 100
    }

    public fun reductionPercent(player: Player): Int = activeState(player)?.percent ?: 0

    private fun activeState(player: Player): IncomingHealingState? {
        val state = states[player] ?: return null
        if (player.currentMapClock >= state.until) {
            states.remove(player)
            return null
        }
        return state
    }
}

public fun Player.reduceIncomingHealing(percent: Int, duration: Int) {
    IncomingHealingModifier.reduce(this, percent, duration)
}

internal fun Player.modifyIncomingHealing(amount: Int): Int =
    IncomingHealingModifier.modify(this, amount)
