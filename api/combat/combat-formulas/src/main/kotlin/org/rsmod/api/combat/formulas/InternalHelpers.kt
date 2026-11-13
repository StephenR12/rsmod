package org.rsmod.api.combat.formulas

import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.game.type.npc.UnpackedNpcType

/**
 * Hit chance formulas internally use decimals (e.g., `1%` = `0.01`, `100%` = `1.0`). To maintain
 * consistency with other combat formulas that use whole integers, we scale them using this
 * constant.
 */
internal const val HIT_CHANCE_SCALE: Int = 10_000

internal fun scale(base: Int, multiplier: Int, divisor: Int): Int = (base * multiplier) / divisor

internal fun UnpackedNpcType.isSlayerTask(player: Player): Boolean = isAssignedSlayerTarget(player)

/** Runite golems use the attacker's Mining level for weapon accuracy and damage. */
internal fun Npc.miningMultiplier(player: Player): Double = if (id == 6600) player.miningLvl / 90.0 else 1.0
