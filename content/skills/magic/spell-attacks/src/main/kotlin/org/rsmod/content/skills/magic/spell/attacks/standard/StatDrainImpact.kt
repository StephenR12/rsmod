package org.rsmod.content.skills.magic.spell.attacks.standard

import kotlin.math.max
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statDrain
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatType

internal data class StatDrainImpact(
    val stat: StatType,
    val percent: Int,
    val constant: Int = 0,
    val onlyWhenRestored: Boolean = false,
)

internal fun Player.canApplyPlayerStatDrain(stat: StatType): Boolean = stat(stat) >= statBase(stat)

internal fun Player.applyStatDrain(impact: StatDrainImpact) {
    if (!impact.onlyWhenRestored || canApplyPlayerStatDrain(impact.stat)) {
        statDrain(impact.stat, constant = impact.constant, percent = impact.percent)
    }
}

internal fun Npc.canApplyNpcStatDrain(stat: StatType): Boolean =
    currentLevel(stat) >= baseLevel(stat)

internal fun Npc.applyStatDrain(impact: StatDrainImpact) {
    if (impact.onlyWhenRestored && !canApplyNpcStatDrain(impact.stat)) {
        return
    }
    val base = baseLevel(impact.stat)
    val current = currentLevel(impact.stat)
    val drain = impact.constant + (base * impact.percent) / 100
    setCurrentLevel(impact.stat, max(base - drain, current - drain))
}

private fun Npc.currentLevel(stat: StatType): Int =
    when {
        stat.isType(stats.attack) -> attackLvl
        stat.isType(stats.strength) -> strengthLvl
        stat.isType(stats.defence) -> defenceLvl
        stat.isType(stats.magic) -> magicLvl
        else -> error("Unsupported NPC stat-drain type: $stat")
    }

private fun Npc.baseLevel(stat: StatType): Int =
    when {
        stat.isType(stats.attack) -> baseAttackLvl
        stat.isType(stats.strength) -> baseStrengthLvl
        stat.isType(stats.defence) -> baseDefenceLvl
        stat.isType(stats.magic) -> baseMagicLvl
        else -> error("Unsupported NPC stat-drain type: $stat")
    }

private fun Npc.setCurrentLevel(stat: StatType, level: Int) {
    when {
        stat.isType(stats.attack) -> attackLvl = level
        stat.isType(stats.strength) -> strengthLvl = level
        stat.isType(stats.defence) -> defenceLvl = level
        stat.isType(stats.magic) -> magicLvl = level
        else -> error("Unsupported NPC stat-drain type: $stat")
    }
}
