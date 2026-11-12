package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.prayerRegenerationDisplay by intVarBit(varbits.prayer_regeneration_potion_timer)

/** Cache-233 Prayer-regeneration state shared by direct consumption and lifecycle hooks. */
@Singleton
public class PrayerRegenerationPotionRepository {
    private val states = WeakHashMap<Player, State>()

    /** A new dose replaces the active effect and restarts its 12-cycle phase. */
    public fun apply(player: Player) {
        states[player] =
            State(
                restoresRemaining = RESTORES_PER_DOSE,
                nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES,
            )
        syncClient(player, RESTORES_PER_DOSE)
        player.softTimer(timers.prayer_regeneration_effect, RESTORE_INTERVAL_CYCLES)
    }

    public fun tick(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        if (state.restoresRemaining <= 0) {
            clear(player)
            return
        }

        if (player.stat(stats.prayer) < player.statBase(stats.prayer)) {
            player.statHeal(stats.prayer, constant = 1, percent = 0)
        }
        state.restoresRemaining--
        if (state.restoresRemaining <= 0) {
            clear(player)
            return
        }

        state.nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES
        syncClient(player, state.restoresRemaining)
        player.softTimer(timers.prayer_regeneration_effect, RESTORE_INTERVAL_CYCLES)
    }

    public fun resume(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        val delay = state.pausedDelay ?: RESTORE_INTERVAL_CYCLES
        state.nextRestoreAt = player.currentMapClock + delay
        state.pausedDelay = null
        syncClient(player, state.restoresRemaining)
        player.softTimer(timers.prayer_regeneration_effect, delay)
    }

    public fun pause(player: Player) {
        states[player]?.let { state ->
            state.pausedDelay = max(1, state.nextRestoreAt - player.currentMapClock)
        }
        player.clearSoftTimer(timers.prayer_regeneration_effect)
    }

    public fun clear(player: Player) {
        states.remove(player)
        syncClient(player, 0)
        player.clearSoftTimer(timers.prayer_regeneration_effect)
    }

    public fun isActive(player: Player): Boolean = player.prayerRegenerationDisplay > 0

    public fun restoresRemaining(player: Player): Int =
        states[player]?.restoresRemaining ?: player.prayerRegenerationDisplay

    private fun reconstruct(player: Player): State? {
        val restoresRemaining = player.prayerRegenerationDisplay
        if (restoresRemaining <= 0) {
            player.clearSoftTimer(timers.prayer_regeneration_effect)
            return null
        }
        return State(
                restoresRemaining = restoresRemaining,
                nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES,
            )
            .also { states[player] = it }
    }

    private fun syncClient(player: Player, restoresRemaining: Int) {
        // Cache varbit 11361 counts the remaining 12-cycle restoration intervals.
        player.prayerRegenerationDisplay = restoresRemaining
    }

    private data class State(
        var restoresRemaining: Int,
        var nextRestoreAt: Int,
        var pausedDelay: Int? = null,
    )

    public companion object {
        public const val RESTORE_INTERVAL_CYCLES: Int = 12
        public const val RESTORES_PER_DOSE: Int = 66
        public const val TOTAL_ACTIVE_CYCLES: Int = RESTORE_INTERVAL_CYCLES * RESTORES_PER_DOSE
    }
}
