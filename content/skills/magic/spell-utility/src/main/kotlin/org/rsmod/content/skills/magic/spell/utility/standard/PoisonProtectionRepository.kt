package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.min
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/**
 * Cache-233 poison/venom protection encoded through client varp 102.
 *
 * Negative values count 30-cycle toxin intervals. Values below -38 protect against venom; values
 * from -38 through -1 protect only against poison. The global interval phase is deliberately
 * preserved, which produces the documented 18-36 and 36-54 second venom-immunity ranges.
 */
@Singleton
public class PoisonProtectionRepository
@Inject
constructor(private val hitProcessor: InstantPlayerHitProcessor) {
    public fun apply(player: Player, strength: PoisonProtectionStrength) {
        val current = player.vars[varps.poison]
        val updated =
            when {
                current >= LunarPoisonCure.VENOM_THRESHOLD && !strength.curesVenom ->
                    LunarPoisonCure.curedState(current)
                current > 0 -> strength.initialVarp
                current == 0 -> strength.initialVarp
                else -> min(current, strength.initialVarp)
            }
        setState(player, updated)
    }

    public fun tick(player: Player) {
        val current = player.vars[varps.poison]
        if (current == 0) {
            player.clearSoftTimer(timers.toxins)
            return
        }
        if (player.ui.modals.isNotEmpty()) {
            player.softTimer(timers.toxins, INTERFACE_POLL_CYCLES)
            return
        }
        when {
            current < 0 -> setState(player, current + 1, alignToGlobalPhase = false)
            current >= LunarPoisonCure.VENOM_THRESHOLD -> tickVenom(player, current)
            else -> tickPoison(player, current)
        }
    }

    public fun resume(player: Player) {
        if (player.vars[varps.poison] != 0) {
            scheduleNextInterval(player, alignToGlobalPhase = true)
        }
    }

    public fun pause(player: Player) {
        player.clearSoftTimer(timers.toxins)
    }

    public fun isPoisonImmune(player: Player): Boolean = player.vars[varps.poison] < 0

    public fun isVenomImmune(player: Player): Boolean =
        player.vars[varps.poison] < VENOM_IMMUNITY_BOUNDARY

    /**
     * Applies recurring poison to a clean player. Source-specific reapplication remains separate.
     */
    public fun inflictPoison(player: Player, severity: Int): Boolean {
        require(severity in 1 until LunarPoisonCure.VENOM_THRESHOLD) {
            "Poison severity must be positive and below the venom threshold. (severity=$severity)"
        }
        val current = player.vars[varps.poison]
        if (current != 0) {
            return false
        }
        setState(player, severity)
        return true
    }

    /** Applies the standard six-damage venom state unless venom immunity or venom is active. */
    public fun inflictVenom(player: Player): Boolean {
        val current = player.vars[varps.poison]
        if (current < VENOM_IMMUNITY_BOUNDARY || current >= LunarPoisonCure.VENOM_THRESHOLD) {
            return false
        }
        setState(player, LunarPoisonCure.VENOM_THRESHOLD)
        return true
    }

    private fun tickPoison(player: Player, severity: Int) {
        val damage = LunarPoisonCure.poisonDamage(severity)
        player.takeInstantHit(
            type = HitType.Typeless,
            damage = damage,
            processor = hitProcessor,
            hitmark = hitmark_groups.poison_damage,
        )
        setState(player, severity - 1, alignToGlobalPhase = false)
    }

    private fun tickVenom(player: Player, state: Int) {
        val damage = LunarPoisonCure.venomDamage(state)
        player.takeInstantHit(
            type = HitType.Typeless,
            damage = damage,
            processor = hitProcessor,
            hitmark = hitmark_groups.venom,
        )
        val next = if (damage < LunarPoisonCure.MAX_VENOM_DAMAGE) state + 1 else state
        setState(player, next, alignToGlobalPhase = false)
    }

    private fun setState(player: Player, state: Int, alignToGlobalPhase: Boolean = true) {
        VarPlayerIntMapSetter.set(player, varps.poison, state)
        if (state != 0) {
            scheduleNextInterval(player, alignToGlobalPhase)
        } else {
            player.clearSoftTimer(timers.toxins)
        }
    }

    private fun scheduleNextInterval(player: Player, alignToGlobalPhase: Boolean) {
        val cycles =
            if (alignToGlobalPhase) {
                val elapsed = Math.floorMod(player.currentMapClock, TOXIN_INTERVAL_CYCLES)
                TOXIN_INTERVAL_CYCLES - elapsed
            } else {
                TOXIN_INTERVAL_CYCLES
            }
        player.softTimer(timers.toxins, cycles)
    }

    public companion object {
        public const val TOXIN_INTERVAL_CYCLES: Int = 30
        public const val INTERFACE_POLL_CYCLES: Int = 1
        public const val VENOM_IMMUNITY_BOUNDARY: Int = -38
    }
}

/** Exact cache-233 varp-102 starting states for share-compatible poison remedies. */
public enum class PoisonProtectionStrength(
    public val initialVarp: Int,
    public val curesVenom: Boolean,
) {
    Antipoison(initialVarp = -5, curesVenom = false),
    Superantipoison(initialVarp = -20, curesVenom = false),
    AntidotePlus(initialVarp = -30, curesVenom = false),
    AntidotePlusPlus(initialVarp = -40, curesVenom = false),
    AntiVenom(initialVarp = -41, curesVenom = true),
    AntiVenomPlus(initialVarp = -50, curesVenom = true),
    ExtendedAntiVenomPlus(initialVarp = -59, curesVenom = true),
}
