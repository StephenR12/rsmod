package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.healGroupCooldownDisplay by intVarBit(varbits.heal_group_timelimit)

/** Owns the exact 150-tick cooldown mirrored by cache-233 varbit 925. */
@Singleton
public class HealGroupStateRepository @Inject constructor() {
    private val cooldownExpiresAt = WeakHashMap<Player, Int>()

    public fun isReady(player: Player): Boolean {
        val expiry =
            cooldownExpiresAt[player]
                ?: run {
                    player.healGroupCooldownDisplay = 0
                    return true
                }
        if (player.currentMapClock < expiry) {
            return false
        }
        clear(player)
        return true
    }

    public fun begin(player: Player) {
        cooldownExpiresAt[player] = player.currentMapClock + COOLDOWN_CYCLES
        player.healGroupCooldownDisplay = 1
        player.softTimer(timers.magic_heal_group_cooldown, 1)
    }

    public fun tick(player: Player) {
        if (isReady(player)) {
            clear(player)
        }
    }

    public fun syncOnLogin(player: Player) {
        if (isReady(player)) {
            clear(player)
        } else {
            player.healGroupCooldownDisplay = 1
            player.softTimer(timers.magic_heal_group_cooldown, 1)
        }
    }

    public fun clear(player: Player) {
        cooldownExpiresAt.remove(player)
        player.healGroupCooldownDisplay = 0
        player.clearSoftTimer(timers.magic_heal_group_cooldown)
    }

    public companion object {
        public const val COOLDOWN_CYCLES: Int = 150
    }
}
