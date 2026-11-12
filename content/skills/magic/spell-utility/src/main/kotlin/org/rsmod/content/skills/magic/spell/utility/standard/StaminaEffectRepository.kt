package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.staminaActiveDisplay by intVarBit(varbits.stamina_active)
private var Player.staminaDurationDisplay by intVarBit(varbits.stamina_duration)

/** Cache-233 two-minute stamina-potion state shared by potion consumption and potion sharing. */
@Singleton
public class StaminaEffectRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player) {
        expiresAt[player] = player.currentMapClock + DURATION_CYCLES
        syncClient(player, DURATION_CYCLES)
        player.softTimer(timers.stamina_effect, 1)
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val expiry = expiresAt[player] ?: return 0
        val remaining = expiry - player.currentMapClock
        if (remaining <= 0) {
            clear(player)
            return 0
        }
        return remaining
    }

    public fun tick(player: Player) {
        val remaining = remainingCycles(player)
        if (remaining > 0) {
            syncClient(player, remaining)
        }
    }

    public fun clear(player: Player) {
        expiresAt.remove(player)
        player.staminaActiveDisplay = 0
        player.staminaDurationDisplay = 0
        player.clearSoftTimer(timers.stamina_effect)
    }

    private fun syncClient(player: Player, remaining: Int) {
        player.staminaActiveDisplay = 1
        // Cache varbit 24 exposes the remaining duration in ten-cycle units.
        player.staminaDurationDisplay = (remaining + DISPLAY_UNIT_CYCLES - 1) / DISPLAY_UNIT_CYCLES
    }

    public companion object {
        public const val DURATION_CYCLES: Int = 200
        public const val DISPLAY_UNIT_CYCLES: Int = 10
    }
}
