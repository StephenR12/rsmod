package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.vengeanceActiveDisplay by intVarBit(varbits.vengeance_rebound)
private var Player.vengeanceCooldownDisplay by intVarBit(varbits.vengeance_timelimit)

/** Owns the server state mirrored by cache-233's Vengeance active and cooldown varbits. */
@Singleton
public class VengeanceStateRepository @Inject constructor() {
    private val cooldownExpiresAt = WeakHashMap<Player, Int>()

    public fun isActive(player: Player): Boolean = player.vengeanceActiveDisplay != 0

    public fun activate(player: Player) {
        player.vengeanceActiveDisplay = 1
    }

    public fun clearActive(player: Player) {
        player.vengeanceActiveDisplay = 0
    }

    public fun isCooldownReady(player: Player): Boolean {
        val expiry = cooldownExpiresAt[player] ?: return true
        if (player.currentMapClock < expiry) {
            return false
        }
        clearCooldown(player)
        return true
    }

    public fun beginCooldown(player: Player) {
        cooldownExpiresAt[player] = player.currentMapClock + COOLDOWN_CYCLES
        player.vengeanceCooldownDisplay = 1
        player.softTimer(timers.magic_vengeance_cooldown, 1)
    }

    public fun tickCooldown(player: Player) {
        if (isCooldownReady(player)) {
            clearCooldown(player)
        }
    }

    public fun syncOnLogin(player: Player) {
        clearActive(player)
        if (isCooldownReady(player)) {
            clearCooldown(player)
        } else {
            player.vengeanceCooldownDisplay = 1
            player.softTimer(timers.magic_vengeance_cooldown, 1)
        }
    }

    public fun clearCooldown(player: Player) {
        cooldownExpiresAt.remove(player)
        player.vengeanceCooldownDisplay = 0
        player.clearSoftTimer(timers.magic_vengeance_cooldown)
    }

    public companion object {
        public const val COOLDOWN_CYCLES: Int = 50
    }
}
