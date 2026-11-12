package org.rsmod.content.skills.hunter.moths

import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.stat.StatType

public object MothEffects {
    public const val SUNLIGHT_CONSTANT: Int = 6
    public const val SUNLIGHT_PERCENT: Int = 20
    public const val SUNLIGHT_HITPOINTS: Int = 8
    public const val SUNLIGHT_RUN_PERCENT: Int = 5
    public const val MOONLIGHT_PRAYER: Int = 22

    public val sunlightRestoredStats: List<StatType> =
        listOf(
            stats.attack,
            stats.defence,
            stats.strength,
            stats.ranged,
            stats.magic,
            stats.cooking,
            stats.woodcutting,
            stats.fletching,
            stats.fishing,
            stats.firemaking,
            stats.crafting,
            stats.smithing,
            stats.mining,
            stats.herblore,
            stats.agility,
            stats.thieving,
            stats.slayer,
            stats.farming,
            stats.runecrafting,
            stats.hunter,
            stats.construction,
        )

    public fun sunlightRestoreAmount(baseLevel: Int): Int =
        SUNLIGHT_CONSTANT + (baseLevel * SUNLIGHT_PERCENT) / 100

    public fun apply(access: ProtectedAccess, effect: MothEffect, directCatch: Boolean) {
        when (effect) {
            MothEffect.Sunlight -> applySunlight(access, directCatch)
            MothEffect.Moonlight -> access.statHeal(stats.prayer, MOONLIGHT_PRAYER, percent = 0)
        }
    }

    private fun applySunlight(access: ProtectedAccess, directCatch: Boolean) {
        for (stat in sunlightRestoredStats) {
            access.statHeal(stat, SUNLIGHT_CONSTANT, SUNLIGHT_PERCENT)
        }
        access.statHeal(stats.hitpoints, SUNLIGHT_HITPOINTS, percent = 0)
        if (directCatch) {
            val amount = (constants.run_max_energy * SUNLIGHT_RUN_PERCENT) / 100
            access.player.runEnergy =
                min(constants.run_max_energy, access.player.runEnergy + amount)
            UpdateRun.energy(access.player, access.player.runEnergy)
        }
    }
}
