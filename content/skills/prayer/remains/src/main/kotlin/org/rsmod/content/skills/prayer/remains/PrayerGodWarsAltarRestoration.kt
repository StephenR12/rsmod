package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

public class PrayerGodWarsAltarRestoration
@Inject
constructor(
    private val alignment: PrayerGodWarsAlignment,
    private val altarRestoration: PrayerAltarRestoration,
) {
    public fun restore(access: ProtectedAccess, altar: BoundLocInfo, god: PrayerGodWarsGod) {
        with(access) {
            if (isInCombat()) {
                mes("You cannot use the altar while in combat.")
                return
            }

            val now = prayerGodWarsEpochSecond()
            val secondsRemaining = vars[varps.godwars_altar_available_at] - now
            if (secondsRemaining > 0) {
                val minutesRemaining = remainingMinutes(secondsRemaining)
                vars[varbits.godwars_altar_delay] = minutesRemaining
                val unit = if (minutesRemaining == 1) "minute" else "minutes"
                mes("You must wait another $minutesRemaining $unit before you may use this altar.")
                return
            }

            val alignedItems = alignment.count(player, god)
            altarRestoration.restore(this, altar, bonus = alignedItems)

            if (god == PrayerGodWarsGod.Zaros && alignedItems > 0) {
                if (stat(stats.hitpoints) < statBase(stats.hitpoints)) {
                    statHeal(stats.hitpoints, constant = 0, percent = 100)
                }
                player.runEnergy = constants.run_max_energy
                UpdateRun.energy(player, player.runEnergy)
                vars[varps.sa_energy] = constants.sa_max_energy
            }

            vars[varps.godwars_altar_available_at] = now + COOLDOWN_SECONDS
            vars[varbits.godwars_altar_delay] = COOLDOWN_MINUTES
        }
    }

    /** Reconstructs the client's coarse timer after reconnecting from the persistent deadline. */
    public fun syncClientTimer(player: Player) {
        val secondsRemaining =
            player.vars[varps.godwars_altar_available_at] - prayerGodWarsEpochSecond()
        player.godWarsAltarDelay = remainingMinutes(secondsRemaining)
    }

    private fun remainingMinutes(seconds: Int): Int =
        if (seconds <= 0) 0 else ((seconds + 59) / 60).coerceAtMost(COOLDOWN_MINUTES)

    public companion object {
        public const val COOLDOWN_MINUTES: Int = 10
        public const val COOLDOWN_SECONDS: Int = COOLDOWN_MINUTES * 60
    }
}

/** Seconds since 1 January 2025; this remains within a signed varp until 2093. */
public fun prayerGodWarsEpochSecond(): Int =
    (System.currentTimeMillis() / 1000L - PRAYER_GOD_WARS_EPOCH_OFFSET).toInt()

private const val PRAYER_GOD_WARS_EPOCH_OFFSET = 1_735_689_600L

private var Player.godWarsAltarDelay by intVarBit(varbits.godwars_altar_delay)
