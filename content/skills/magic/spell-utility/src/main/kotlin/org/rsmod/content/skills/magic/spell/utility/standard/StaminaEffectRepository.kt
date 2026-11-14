package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.EnduranceRing
import org.rsmod.api.player.ring
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.staminaActiveDisplay by intVarBit(varbits.stamina_active)
private var Player.staminaDurationDisplay by intVarBit(varbits.stamina_duration)

/** Two-minute stamina-potion state shared by potion consumption and potion sharing. */
@Singleton
public class StaminaEffectRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    private val extended = java.util.Collections.newSetFromMap(WeakHashMap<Player, Boolean>())

    /** Returns whether a charge was spent, so the dose can restore its additional run energy. */
    public fun drinkDose(player: Player): Boolean {
        val ring = player.ring
        val charges = EnduranceRing.charges(ring)
        if (ring == null || charges <= 0) {
            activate(player)
            return false
        }
        player.ring = EnduranceRing.withCharges(ring,charges-1)
        activate(player, DURATION_CYCLES * 2)
        extended.add(player)
        return true
    }

    public fun activate(player: Player, duration: Int = DURATION_CYCLES) {
        require(duration > 0)
        extended.remove(player)
        expiresAt[player] = player.currentMapClock + duration
        syncClient(player, duration)
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
        var remaining = remainingCycles(player)
        val ring = player.ring
        if (remaining > 0 && player in extended && ring?.id != objs.ring_of_endurance.id && ring?.id != objs.ring_of_endurance_nocharges.id) {
            remaining = (remaining + 1) / 2
            extended.remove(player)
            expiresAt[player] = player.currentMapClock + remaining
        }
        if (remaining > 0) {
            syncClient(player, remaining)
        }
    }

    public fun clear(player: Player) {
        expiresAt.remove(player)
        extended.remove(player)
        player.staminaActiveDisplay = 0
        player.staminaDurationDisplay = 0
        player.clearSoftTimer(timers.stamina_effect)
    }

    private fun syncClient(player: Player, remaining: Int) {
        player.staminaActiveDisplay = 1
        // The client varbit exposes at most twenty ten-cycle units. An Endurance Ring's longer
        // effect remains tracked by expiresAt; revision 238 no longer has an extra-duration varbit.
        val units = (remaining + DISPLAY_UNIT_CYCLES - 1) / DISPLAY_UNIT_CYCLES
        player.staminaDurationDisplay = minOf(units,20)
    }

    public companion object {
        public const val DURATION_CYCLES: Int = 200
        public const val DISPLAY_UNIT_CYCLES: Int = 10
    }
}
