package org.rsmod.api.death

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/** Cache-233 clue rolls shared by NPC drop-table registrations. */
public class NpcClueDropRolls
@Inject
constructor(private val clues: ClueInventory, private val easySteps: Cache233EasyClueSteps) {
    /**
     * Performs an independent clue roll with ownership, X Marks and scroll-box capacity policy.
     *
     * Before X Marks, an already-owned tier skips the chance roll. After X Marks, the roll is still
     * performed at capacity so a success can emit the exact blocked-box message.
     */
    public fun independent(
        tier: ClueTier,
        clueScroll: ObjType,
        numerator: Int = 1,
        denominator: Int,
    ): NpcDropRoll {
        require(denominator > 0) { "Clue denominator must be positive: $denominator" }
        require(numerator in 1..denominator) {
            "Clue numerator must be in 1..$denominator: $numerator"
        }
        return NpcDropRoll { context ->
            if (tier.members && !context.hero.members) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            val state = clues.state(context.hero, tier)
            if (!state.xMarksComplete && !state.canReceive) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            if (context.random.of(denominator) >= numerator) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            val reward = clues.reward(state, tier, clueScroll)
            val drop =
                reward.obj?.let { type ->
                    NpcDrop(
                        obj = InvObj(type),
                        coords = context.coords,
                        receiver = context.hero,
                        duration = reward.groundDuration,
                    )
                }
            NpcDropRollResult(
                selected = true,
                drops = listOfNotNull(drop),
                messages = listOfNotNull(reward.message),
            )
        }
    }

    /** Performs an Easy-clue roll and selects one exact cache-233.4 initial step on success. */
    public fun independentEasy(numerator: Int = 1, denominator: Int): NpcDropRoll {
        require(denominator > 0) { "Clue denominator must be positive: $denominator" }
        require(numerator in 1..denominator) {
            "Clue numerator must be in 1..$denominator: $numerator"
        }
        return NpcDropRoll { context ->
            if (!context.hero.members) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            val state = clues.state(context.hero, ClueTier.Easy)
            if (!state.xMarksComplete && !state.canReceive) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            if (context.random.of(denominator) >= numerator) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            val reward =
                clues.reward(state, ClueTier.Easy) {
                    easySteps.select(context.random.of(Cache233EasyClueSteps.SIZE))
                }
            val drop =
                reward.obj?.let { type ->
                    NpcDrop(
                        obj = InvObj(type),
                        coords = context.coords,
                        receiver = context.hero,
                        duration = reward.groundDuration,
                    )
                }
            NpcDropRollResult(
                selected = true,
                drops = listOfNotNull(drop),
                messages = listOfNotNull(reward.message),
            )
        }
    }
}
