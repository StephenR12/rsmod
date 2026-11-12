package org.rsmod.api.death

import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.conditional
import org.rsmod.api.death.NpcDropRolls.firstSelected
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.game.type.obj.ObjType

/** Shared cache-233 long-bone and curved-bone tertiary roll for eligible NPCs. */
public object NpcRareBoneDropRolls {
    /**
     * Rolls a members-only long bone first, then a curved bone only after the long-bone roll
     * misses.
     *
     * This preserves the published effective curved-bone rate of 399/400 * 1/5,000, or 1/5,012.5.
     */
    public fun longOrCurved(): NpcDropRoll = longOrCurved(objs.long_bone, objs.curved_bone)

    internal fun longOrCurved(longBone: ObjType, curvedBone: ObjType): NpcDropRoll =
        conditional(
            predicate = { it.hero.members },
            roll =
                firstSelected(
                    independent(denominator = LONG_BONE_DENOMINATOR, roll = item(longBone)),
                    independent(denominator = CURVED_BONE_DENOMINATOR, roll = item(curvedBone)),
                ),
        )

    public const val LONG_BONE_DENOMINATOR: Int = 400
    public const val CURVED_BONE_DENOMINATOR: Int = 5_000
}
