package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

/** Persistent Hunter-cape fields stored in cache-native `skillcape_perks` varp 1312. */
public class HunterCapeState @Inject constructor() {
    private var ProtectedAccess.packedState by intVarp(varps.skillcape_perks)

    public fun usedToday(access: ProtectedAccess, currentUtcDay: Int = currentUtcDay()): Int =
        usedToday(access.player, currentUtcDay)

    public fun usedToday(player: Player, currentUtcDay: Int = currentUtcDay()): Int {
        val state = decode(player.vars[varps.skillcape_perks])
        return if (state.utcDay == normalizedDay(currentUtcDay)) state.teleportsUsed else 0
    }

    public fun consumeTeleport(access: ProtectedAccess, currentUtcDay: Int = currentUtcDay()) {
        with(access) {
            val state = decode(packedState)
            val day = normalizedDay(currentUtcDay)
            val used = if (state.utcDay == day) state.teleportsUsed else 0
            require(used < DAILY_TELEPORT_LIMIT) { "Hunter cape daily teleport limit exceeded." }
            packedState = encode(packedState, state.copy(teleportsUsed = used + 1, utcDay = day))
        }
    }

    public fun hasGuildTeleport(access: ProtectedAccess): Boolean = hasGuildTeleport(access.player)

    public fun hasGuildTeleport(player: Player): Boolean =
        decode(player.vars[varps.skillcape_perks]).guildTeleport

    public fun unlockGuildTeleport(access: ProtectedAccess) {
        with(access) {
            val state = decode(packedState)
            packedState = encode(packedState, state.copy(guildTeleport = true))
        }
    }

    public fun setForTest(
        access: ProtectedAccess,
        currentUtcDay: Int,
        teleportsUsed: Int,
        guildTeleport: Boolean = false,
    ) {
        require(teleportsUsed in 0..DAILY_TELEPORT_LIMIT)
        with(access) {
            packedState =
                encode(
                    packedState,
                    State(teleportsUsed, normalizedDay(currentUtcDay), guildTeleport),
                )
        }
    }

    private fun decode(value: Int): State =
        State(
            teleportsUsed = value and TELEPORT_COUNT_MASK,
            utcDay = (value ushr UTC_DAY_SHIFT) and UTC_DAY_MASK,
            guildTeleport = value and GUILD_TELEPORT_MASK != 0,
        )

    private fun encode(previous: Int, state: State): Int {
        val encoded =
            (state.teleportsUsed and TELEPORT_COUNT_MASK) or
                ((state.utcDay and UTC_DAY_MASK) shl UTC_DAY_SHIFT) or
                (if (state.guildTeleport) GUILD_TELEPORT_MASK else 0)
        return (previous and HUNTER_STATE_MASK.inv()) or encoded
    }

    private fun normalizedDay(day: Int): Int = day and UTC_DAY_MASK

    private data class State(val teleportsUsed: Int, val utcDay: Int, val guildTeleport: Boolean)

    public companion object {
        public const val DAILY_TELEPORT_LIMIT: Int = 5

        private const val TELEPORT_COUNT_MASK = 0b111
        private const val GUILD_TELEPORT_MASK = 1 shl 3
        private const val UTC_DAY_SHIFT = 4
        private const val UTC_DAY_MASK = 0x1FFFF
        private const val HUNTER_STATE_MASK =
            TELEPORT_COUNT_MASK or GUILD_TELEPORT_MASK or (UTC_DAY_MASK shl UTC_DAY_SHIFT)

        public fun currentUtcDay(): Int = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
    }
}
