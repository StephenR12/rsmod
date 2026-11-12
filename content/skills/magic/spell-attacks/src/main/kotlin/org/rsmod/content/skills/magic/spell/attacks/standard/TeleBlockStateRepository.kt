package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.skills.magic.spell.attacks.standard.config.teleblock_timers
import org.rsmod.game.entity.Player

public enum class TeleBlockStatus {
    Available,
    Blocked,
    Immune,
}

public data class TeleBlockState(val source: Player, val blockedUntil: Int, val immuneUntil: Int)

internal data class TeleBlockImpact(val source: Player, val protectedFromMagicAtCast: Boolean)

/** Target-owned Tele Block state shared by spell casts and future teleport implementations. */
@Singleton
public class TeleBlockStateRepository {
    private val states = WeakHashMap<Player, TeleBlockState>()
    private var Player.teleblockCycles by intVarBit(varbits.teleblock_cycles)

    public fun status(target: Player): TeleBlockStatus {
        val state = states[target] ?: return TeleBlockStatus.Available
        if (!target.isInWilderness() || target.teleblockCycles == 0) {
            clear(target)
            return TeleBlockStatus.Available
        }
        return when {
            target.currentMapClock < state.blockedUntil -> TeleBlockStatus.Blocked
            target.currentMapClock < state.immuneUntil -> TeleBlockStatus.Immune
            else -> {
                clear(target)
                TeleBlockStatus.Available
            }
        }
    }

    public fun apply(target: Player, source: Player, duration: Int): TeleBlockState? {
        require(duration > 0) { "Tele Block duration must be positive: $duration" }
        if (status(target) != TeleBlockStatus.Available) {
            return null
        }
        val state =
            TeleBlockState(
                source = source,
                blockedUntil = target.currentMapClock + duration,
                immuneUntil = target.currentMapClock + duration + REAPPLICATION_IMMUNITY_CYCLES,
            )
        states[target] = state
        syncClientCycles(target, state)
        target.softTimer(teleblock_timers.status, 1)
        return state
    }

    public fun tick(target: Player) {
        val state = states[target]
        if (state == null || status(target) == TeleBlockStatus.Available) {
            clear(target)
            return
        }
        syncClientCycles(target, state)
    }

    public fun clear(target: Player) {
        states.remove(target)
        target.teleblockCycles = 0
        target.clearSoftTimer(teleblock_timers.status)
    }

    public fun isBlocked(target: Player): Boolean = status(target) == TeleBlockStatus.Blocked

    public fun remainingBlockingCycles(target: Player): Int {
        val state = states[target] ?: return 0
        if (status(target) != TeleBlockStatus.Blocked) {
            return 0
        }
        return max(0, state.blockedUntil - target.currentMapClock)
    }

    public fun stateFor(target: Player): TeleBlockState? = states[target]

    private fun syncClientCycles(target: Player, state: TeleBlockState) {
        // Cache varbit 4163 encodes active cycles above 100, followed by 100 immunity cycles.
        target.teleblockCycles = max(0, state.immuneUntil - target.currentMapClock)
    }

    private fun Player.isInWilderness(): Boolean = vars[varbits.inside_wilderness] == 1

    public companion object {
        public const val FULL_DURATION_CYCLES: Int = 500
        public const val PROTECTED_DURATION_CYCLES: Int = 250
        public const val REAPPLICATION_IMMUNITY_CYCLES: Int = 100
    }
}
