package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.goadingPotionDisplay by intVarBit(varbits.goading_potion_timer)

/** Cache-233 goading-potion duration shared by direct potion consumers and Boost Potion Share. */
@Singleton
public class GoadingPotionRepository {
    private val states = WeakHashMap<Player, State>()

    public fun apply(player: Player) {
        states[player] = State(expiresAt = player.currentMapClock + DURATION_CYCLES)
        syncClient(player)
        player.softTimer(timers.goading_effect, TICK_CYCLES)
    }

    public fun tick(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        if (state.expiresAt <= player.currentMapClock) {
            clear(player)
            return
        }
        syncClient(player)
    }

    public fun resume(player: Player) {
        val state = states[player] ?: reconstruct(player)
        if (state != null) {
            state.pausedRemaining?.let { state.expiresAt = player.currentMapClock + it }
            state.pausedRemaining = null
            syncClient(player)
            player.softTimer(timers.goading_effect, TICK_CYCLES)
        }
    }

    public fun pause(player: Player) {
        states[player]?.let { state ->
            state.pausedRemaining = max(0, state.expiresAt - player.currentMapClock)
        }
        player.clearSoftTimer(timers.goading_effect)
    }

    public fun clear(player: Player) {
        states.remove(player)
        player.goadingPotionDisplay = 0
        player.clearSoftTimer(timers.goading_effect)
    }

    public fun cancel(player: Player): Boolean {
        if (!isActive(player)) {
            return false
        }
        clear(player)
        player.mes(EXPIRED_MESSAGE)
        return true
    }

    public fun isActive(player: Player): Boolean = player.goadingPotionDisplay > 0

    public fun remainingCycles(player: Player): Int {
        val state = states[player]
        return if (state != null) {
            state.pausedRemaining ?: max(0, state.expiresAt - player.currentMapClock)
        } else {
            player.goadingPotionDisplay * DISPLAY_UNIT_CYCLES
        }
    }

    private fun syncClient(player: Player) {
        val state = states[player] ?: return
        val remaining = state.pausedRemaining ?: max(0, state.expiresAt - player.currentMapClock)
        player.goadingPotionDisplay = (remaining + DISPLAY_UNIT_CYCLES - 1) / DISPLAY_UNIT_CYCLES
    }

    private fun reconstruct(player: Player): State? {
        val display = player.goadingPotionDisplay
        if (display <= 0) {
            player.clearSoftTimer(timers.goading_effect)
            return null
        }
        return State(expiresAt = player.currentMapClock + display * DISPLAY_UNIT_CYCLES).also {
            states[player] = it
        }
    }

    private data class State(var expiresAt: Int, var pausedRemaining: Int? = null)

    public companion object {
        public const val DURATION_CYCLES: Int = 600
        public const val DISPLAY_UNIT_CYCLES: Int = 6
        public const val TICK_CYCLES: Int = 1
        public const val EXPIRED_MESSAGE: String =
            "The effects of your Goading potion have worn off."
    }
}
