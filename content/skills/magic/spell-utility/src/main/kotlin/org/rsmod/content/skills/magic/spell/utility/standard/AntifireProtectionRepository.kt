package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.antifireDisplay by intVarBit(varbits.antifire_potion)
private var Player.superAntifireDisplay by intVarBit(varbits.super_antifire_potion)

/**
 * Cache-233 antifire potion state shared by direct potion consumers and Boost Potion Share.
 *
 * The client exposes ordinary antifire in 30-cycle units through varbit 3981 and superior antifire
 * in 20-cycle units through varbit 6101. Exact expiry is kept in cycles while the player is
 * resident; the cache varbits provide a bounded resume value after reconstruction.
 */
@Singleton
public class AntifireProtectionRepository {
    private val states = WeakHashMap<Player, State>()

    public fun apply(player: Player, strength: AntifireProtectionStrength) {
        val state = states.getOrPut(player, ::State)
        val expiresAt = player.currentMapClock + strength.durationCycles
        if (strength.superior) {
            state.superExpiresAt = expiresAt
            state.superWarningSent = false
        } else {
            state.regularExpiresAt = expiresAt
            state.regularWarningSent = false
        }
        syncClient(player, state)
        player.softTimer(timers.antifire_effect, TICK_CYCLES)
    }

    public fun tick(player: Player) {
        val state = states[player] ?: reconstruct(player) ?: return
        tickRegular(player, state)
        tickSuperior(player, state)
        syncClient(player, state)
        if (state.regularExpiresAt == null && state.superExpiresAt == null) {
            states.remove(player)
            player.clearSoftTimer(timers.antifire_effect)
        }
    }

    public fun resume(player: Player) {
        val state = states[player] ?: reconstruct(player)
        if (state != null) {
            syncClient(player, state)
            player.softTimer(timers.antifire_effect, TICK_CYCLES)
        }
    }

    public fun pause(player: Player) {
        player.clearSoftTimer(timers.antifire_effect)
    }

    public fun clear(player: Player) {
        states.remove(player)
        player.antifireDisplay = 0
        player.superAntifireDisplay = 0
        player.clearSoftTimer(timers.antifire_effect)
    }

    public fun isRegularActive(player: Player): Boolean = player.antifireDisplay > 0

    public fun isSuperiorActive(player: Player): Boolean = player.superAntifireDisplay > 0

    public fun remainingCycles(player: Player, superior: Boolean): Int {
        val state = states[player]
        val expiry = if (superior) state?.superExpiresAt else state?.regularExpiresAt
        if (expiry != null) {
            return max(0, expiry - player.currentMapClock)
        }
        val display = if (superior) player.superAntifireDisplay else player.antifireDisplay
        val unit = if (superior) SUPER_DISPLAY_UNIT_CYCLES else REGULAR_DISPLAY_UNIT_CYCLES
        return display * unit
    }

    private fun tickRegular(player: Player, state: State) {
        val expiry = state.regularExpiresAt ?: return
        val remaining = expiry - player.currentMapClock
        when {
            remaining <= 0 -> {
                state.regularExpiresAt = null
                player.mes(REGULAR_EXPIRED_MESSAGE)
            }
            remaining <= WARNING_CYCLES && !state.regularWarningSent -> {
                state.regularWarningSent = true
                player.mes(REGULAR_WARNING_MESSAGE)
            }
        }
    }

    private fun tickSuperior(player: Player, state: State) {
        val expiry = state.superExpiresAt ?: return
        val remaining = expiry - player.currentMapClock
        when {
            remaining <= 0 -> {
                state.superExpiresAt = null
                player.mes(SUPER_EXPIRED_MESSAGE)
            }
            remaining <= WARNING_CYCLES && !state.superWarningSent -> {
                state.superWarningSent = true
                player.mes(SUPER_WARNING_MESSAGE)
            }
        }
    }

    private fun syncClient(player: Player, state: State) {
        player.antifireDisplay =
            displayUnits(player, state.regularExpiresAt, REGULAR_DISPLAY_UNIT_CYCLES)
        player.superAntifireDisplay =
            displayUnits(player, state.superExpiresAt, SUPER_DISPLAY_UNIT_CYCLES)
    }

    private fun displayUnits(player: Player, expiry: Int?, unit: Int): Int {
        if (expiry == null) {
            return 0
        }
        val remaining = max(0, expiry - player.currentMapClock)
        return (remaining + unit - 1) / unit
    }

    private fun reconstruct(player: Player): State? {
        val regular = player.antifireDisplay
        val superior = player.superAntifireDisplay
        if (regular <= 0 && superior <= 0) {
            player.clearSoftTimer(timers.antifire_effect)
            return null
        }
        return State(
                regularExpiresAt =
                    regular
                        .takeIf { it > 0 }
                        ?.let { player.currentMapClock + it * REGULAR_DISPLAY_UNIT_CYCLES },
                superExpiresAt =
                    superior
                        .takeIf { it > 0 }
                        ?.let { player.currentMapClock + it * SUPER_DISPLAY_UNIT_CYCLES },
            )
            .also { states[player] = it }
    }

    private data class State(
        var regularExpiresAt: Int? = null,
        var superExpiresAt: Int? = null,
        var regularWarningSent: Boolean = false,
        var superWarningSent: Boolean = false,
    )

    public companion object {
        public const val TICK_CYCLES: Int = 1
        public const val WARNING_CYCLES: Int = 25
        public const val REGULAR_DISPLAY_UNIT_CYCLES: Int = 30
        public const val SUPER_DISPLAY_UNIT_CYCLES: Int = 20
        public const val REGULAR_WARNING_MESSAGE: String =
            "Your antifire potion is about to expire."
        public const val REGULAR_EXPIRED_MESSAGE: String = "Your antifire potion has expired."
        public const val SUPER_WARNING_MESSAGE: String =
            "Your super antifire potion is about to expire."
        public const val SUPER_EXPIRED_MESSAGE: String = "Your super antifire potion has expired."
    }
}

/** Cache-233 dose durations and client-display tiers for the four antifire families. */
public enum class AntifireProtectionStrength(
    public val durationCycles: Int,
    public val superior: Boolean,
) {
    Antifire(durationCycles = 600, superior = false),
    ExtendedAntifire(durationCycles = 1_200, superior = false),
    SuperAntifire(durationCycles = 300, superior = true),
    ExtendedSuperAntifire(durationCycles = 600, superior = true),
}

/** Standard, non-boss dragonfire families with common cache-233 protection rules. */
public enum class DragonfireBreath {
    Chromatic,
    Metallic,
}

/**
 * Resolves protection around the caller's inclusive damage roll.
 *
 * Boss breaths, KBD secondary effects, Vorkath, Elvarg, Galvek, drakes and wyverns deliberately do
 * not pass through this resolver because their tables and secondary effects differ.
 */
@Singleton
public class StandardDragonfireDamageResolver
@Inject
constructor(private val antifire: AntifireProtectionRepository) {
    public fun maximumDamage(
        player: Player,
        breath: DragonfireBreath,
        resistedAccuracyRoll: Boolean,
        antiDragonShield: Boolean,
    ): Int =
        resolveDamage(
            player,
            breath,
            resistedAccuracyRoll,
            antiDragonShield,
            prePotionMaximum(player, breath, resistedAccuracyRoll, antiDragonShield),
        )

    /** Applies potion reduction after the supplied shield/prayer-capped damage roll. */
    public fun resolveDamage(
        player: Player,
        breath: DragonfireBreath,
        resistedAccuracyRoll: Boolean,
        antiDragonShield: Boolean,
        rolledDamage: Int,
    ): Int {
        val maximum = prePotionMaximum(player, breath, resistedAccuracyRoll, antiDragonShield)
        require(rolledDamage in 0..maximum) {
            "Dragonfire roll must be within the protection-capped range. " +
                "(rolled=$rolledDamage, maximum=$maximum)"
        }
        if (antifire.isSuperiorActive(player)) {
            return 0
        }
        return if (antifire.isRegularActive(player)) {
            max(0, rolledDamage - REGULAR_POTION_REDUCTION)
        } else {
            rolledDamage
        }
    }

    private fun prePotionMaximum(
        player: Player,
        breath: DragonfireBreath,
        resistedAccuracyRoll: Boolean,
        antiDragonShield: Boolean,
    ): Int {
        val unprotected = if (resistedAccuracyRoll) RESISTED_MAX_HIT else FAILED_MAX_HIT
        val prayer = player.vars[varbits.protect_from_magic] != 0
        return when (breath) {
            DragonfireBreath.Chromatic ->
                when {
                    antiDragonShield -> SHIELD_MAX_HIT
                    prayer -> PRAYER_MAX_HIT
                    else -> unprotected
                }
            DragonfireBreath.Metallic -> if (antiDragonShield) SHIELD_MAX_HIT else unprotected
        }
    }

    private companion object {
        const val FAILED_MAX_HIT = 50
        const val RESISTED_MAX_HIT = 30
        const val SHIELD_MAX_HIT = 5
        const val PRAYER_MAX_HIT = 10
        const val REGULAR_POTION_REDUCTION = 15
    }
}
