package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varbit.VarBitType

private var Player.menaphiteRemedyDisplay by intVarBit(varbits.menaphite_remedy_timer)

/** Cache-233 Menaphite-remedy restoration state and 25-cycle client-visible cadence. */
@Singleton
public class MenaphiteRemedyRepository
@Inject
constructor(private val divineEffects: DivinePotionEffectRepository) {
    private val states = WeakHashMap<Player, State>()

    /** A new dose removes existing divine status and restarts all twenty restoration intervals. */
    public fun apply(player: Player) {
        divineEffects.clearAll(player)
        states[player] =
            State(
                restoresRemaining = RESTORES_PER_DOSE,
                nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES,
            )
        syncClient(player, RESTORES_PER_DOSE)
        player.softTimer(timers.menaphite_remedy_effect, RESTORE_INTERVAL_CYCLES)
    }

    public fun tick(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        if (state.restoresRemaining <= 0) {
            clear(player)
            return
        }

        for (stat in restoredStats) {
            if (player.stat(stat) < player.statBase(stat)) {
                player.statHeal(stat, constant = RESTORE_CONSTANT, percent = RESTORE_PERCENT)
            }
        }
        state.restoresRemaining--
        if (state.restoresRemaining <= 0) {
            clear(player)
            return
        }

        state.nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES
        syncClient(player, state.restoresRemaining)
        player.softTimer(timers.menaphite_remedy_effect, RESTORE_INTERVAL_CYCLES)
    }

    public fun pause(player: Player) {
        states[player]?.let { state ->
            state.pausedDelay = max(1, state.nextRestoreAt - player.currentMapClock)
        }
        player.clearSoftTimer(timers.menaphite_remedy_effect)
    }

    public fun resume(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        val delay = state.pausedDelay ?: RESTORE_INTERVAL_CYCLES
        state.nextRestoreAt = player.currentMapClock + delay
        state.pausedDelay = null
        syncClient(player, state.restoresRemaining)
        player.softTimer(timers.menaphite_remedy_effect, delay)
    }

    public fun clear(player: Player) {
        states.remove(player)
        syncClient(player, 0)
        player.clearSoftTimer(timers.menaphite_remedy_effect)
    }

    public fun isActive(player: Player): Boolean = player.menaphiteRemedyDisplay > 0

    public fun restoresRemaining(player: Player): Int =
        states[player]?.restoresRemaining ?: player.menaphiteRemedyDisplay

    private fun reconstruct(player: Player): State? {
        val restoresRemaining = player.menaphiteRemedyDisplay
        if (restoresRemaining <= 0) {
            player.clearSoftTimer(timers.menaphite_remedy_effect)
            return null
        }
        return State(
                restoresRemaining = restoresRemaining,
                nextRestoreAt = player.currentMapClock + RESTORE_INTERVAL_CYCLES,
            )
            .also { states[player] = it }
    }

    private fun syncClient(player: Player, restoresRemaining: Int) {
        player.menaphiteRemedyDisplay = restoresRemaining
    }

    private data class State(
        var restoresRemaining: Int,
        var nextRestoreAt: Int,
        var pausedDelay: Int? = null,
    )

    public companion object {
        public const val RESTORE_INTERVAL_CYCLES: Int = 25
        public const val RESTORES_PER_DOSE: Int = 20
        public const val RESTORE_CONSTANT: Int = 6
        public const val RESTORE_PERCENT: Int = 16
        public const val TOTAL_ACTIVE_CYCLES: Int = RESTORE_INTERVAL_CYCLES * RESTORES_PER_DOSE

        private val restoredStats =
            listOf(stats.attack, stats.strength, stats.defence, stats.ranged, stats.magic)
    }
}

/** Shared cache-backed divine-status seam used by Menaphite remedy and future divine potions. */
@Singleton
public class DivinePotionEffectRepository {
    public fun clearAll(player: Player) {
        for (timer in effectTimers) {
            VarPlayerIntMapSetter.set(player, timer, 0)
        }
    }

    public fun anyActive(player: Player): Boolean = effectTimers.any { player.vars[it] > 0 }

    private val effectTimers: List<VarBitType> =
        listOf(
            varbits.divine_super_attack_timer,
            varbits.divine_super_strength_timer,
            varbits.divine_super_defence_timer,
            varbits.divine_ranging_timer,
            varbits.divine_magic_timer,
            varbits.divine_super_combat_timer,
            varbits.divine_bastion_timer,
            varbits.divine_battlemage_timer,
            varbits.saturated_heart_timer,
        )
}
