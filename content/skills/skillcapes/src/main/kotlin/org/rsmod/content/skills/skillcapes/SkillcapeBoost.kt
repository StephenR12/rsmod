package org.rsmod.content.skills.skillcapes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatType

/** Shared visible +1 effect for the 23 ordinary cache-era Skillcapes. */
public class SkillcapeBoost @Inject constructor() {
    public fun activate(
        player: Player,
        stat: StatType,
        nowSeconds: Int = currentEpochSecond(),
    ): Boolean {
        if (stat.hasTimedReuse && nowSeconds < player.readyAt(stat)) {
            return false
        }

        val before = player.stat(stat)
        player.statBoost(stat, constant = 1, percent = 0)
        if (player.stat(stat) == before) {
            return false
        }

        if (stat.hasTimedReuse) {
            player.setReadyAt(stat, nowSeconds + COOLDOWN_SECONDS)
        }
        return true
    }

    private val StatType.hasTimedReuse: Boolean
        get() = this == stats.hitpoints || this == stats.prayer

    private fun Player.readyAt(stat: StatType): Int =
        when (stat) {
            stats.hitpoints -> hitpointsSkillcapeReadyAt
            stats.prayer -> prayerSkillcapeReadyAt
            else -> 0
        }

    private fun Player.setReadyAt(stat: StatType, value: Int) {
        when (stat) {
            stats.hitpoints -> hitpointsSkillcapeReadyAt = value
            stats.prayer -> prayerSkillcapeReadyAt = value
            else -> Unit
        }
    }

    public companion object {
        public const val COOLDOWN_SECONDS: Int = 60
        private const val EPOCH_OFFSET_SECONDS: Long = 1_735_689_600L

        private fun currentEpochSecond(): Int =
            (System.currentTimeMillis() / 1000L - EPOCH_OFFSET_SECONDS).toInt()
    }
}

private var Player.hitpointsSkillcapeReadyAt by intVarp(varps.skillcape_hitpoints_boost_ready_at)
private var Player.prayerSkillcapeReadyAt by intVarp(varps.skillcape_prayer_boost_ready_at)
