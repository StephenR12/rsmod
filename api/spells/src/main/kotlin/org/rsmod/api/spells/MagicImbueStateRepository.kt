package org.rsmod.api.spells

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.magicImbueDisplay by intVarBit(varbits.magic_imbue_active)

@Singleton
public class MagicImbueStateRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player) {
        expiresAt[player] = player.currentMapClock + DURATION_CYCLES
        syncClient(player, DURATION_CYCLES)
        player.softTimer(timers.magic_imbue_active, 1)
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val expiry = expiresAt[player]
        if (expiry == null) {
            player.magicImbueDisplay = 0
            return 0
        }
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
        player.magicImbueDisplay = 0
        player.clearSoftTimer(timers.magic_imbue_active)
    }

    private fun syncClient(player: Player, remaining: Int) {
        // Cache varbit 5438 is only two bits wide. RuneLite defines each non-zero unit as ten
        // remaining game ticks, while the effect itself lasts the Wiki's exact 21-tick period.
        player.magicImbueDisplay = minOf(MAX_DISPLAY_VALUE, (remaining + 9) / 10)
    }

    public companion object {
        public const val DURATION_CYCLES: Int = 21
        public const val MAX_DISPLAY_VALUE: Int = 2
    }
}
