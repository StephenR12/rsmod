package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueReward
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.varbit.VarBitType

public data class PickpocketClueDrop(
    val tier: ClueTier,
    val denominator: Int,
    val combatAchievementTierStatus: VarBitType,
) {
    init {
        require(denominator > 0) { "Pickpocket clue denominator must be positive: $denominator" }
    }
}

/** Successful-pickpocket clue policy shared by the cache-233.4 Paladin, Gnome and Hero families. */
public class PickpocketClueDrops
@Inject
constructor(private val clues: ClueInventory, private val steps: Cache233ClueSteps) {
    public fun roll(access: ProtectedAccess, drop: PickpocketClueDrop): ClueReward? {
        with(access) {
            val state = clues.state(player, drop.tier)
            if (!state.xMarksComplete && !state.canReceive) {
                return null
            }
            if (random.of(effectiveDenominator(this, drop)) != 0) {
                return null
            }
            return clues.reward(state, drop.tier) {
                val pool = steps.steps(drop.tier)
                steps.select(drop.tier, random.of(pool.size))
            }
        }
    }

    public fun effectiveDenominator(access: ProtectedAccess, drop: PickpocketClueDrop): Int {
        with(access) {
            return if (player.vars[drop.combatAchievementTierStatus] == COMPLETE_STATUS) {
                drop.denominator * COMBAT_ACHIEVEMENT_PERCENT / 100
            } else {
                drop.denominator
            }
        }
    }

    private companion object {
        const val COMPLETE_STATUS: Int = 2
        const val COMBAT_ACHIEVEMENT_PERCENT: Int = 95
    }
}
