package org.rsmod.api.death

import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.type.varp.VarpType

/** Exact eligibility and duplicate policy shared by lower-champion scroll tertiary rolls. */
public object NpcChampionScrollDropRolls {
    /**
     * Performs the canonical independent 1-in-5,000 lower-champion-scroll roll.
     *
     * Players may receive repeated scrolls until [completed] is set. Once the matching champion has
     * been defeated, a successful roll emits the historical dark-red feedback instead. The
     * membership and 32-quest-point gates are checked before consuming the chance roll.
     */
    public fun independent(
        type: ObjType,
        questPoints: VarpType,
        completed: VarBitType,
        denominator: Int = DEFAULT_DENOMINATOR,
    ): NpcDropRoll {
        require(denominator > 0) { "Champion-scroll denominator must be positive: $denominator" }
        return NpcDropRoll { context ->
            val hero = context.hero
            if (!hero.members || hero.vars[questPoints] < MIN_QUEST_POINTS) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            if (context.random.of(denominator) != 0) {
                return@NpcDropRoll NpcDropRollResult.Miss
            }
            if (hero.vars[completed] != 0) {
                return@NpcDropRoll NpcDropRollResult(
                    selected = true,
                    drops = emptyList(),
                    messages = listOf(DUPLICATE_MESSAGE),
                )
            }
            NpcDropRollResult(
                selected = true,
                drops =
                    listOf(NpcDrop(obj = InvObj(type), coords = context.coords, receiver = hero)),
                messages = listOf(DROP_MESSAGE),
            )
        }
    }

    public const val MIN_QUEST_POINTS: Int = 32
    public const val DEFAULT_DENOMINATOR: Int = 5_000
    public const val DROP_MESSAGE: String =
        "<col=990000>A Champion's scroll falls to the ground as you slay your opponent.</col>"
    public const val DUPLICATE_MESSAGE: String =
        "<col=990000>You have a funny feeling that you would have received a Champion's scroll...</col>"
}
