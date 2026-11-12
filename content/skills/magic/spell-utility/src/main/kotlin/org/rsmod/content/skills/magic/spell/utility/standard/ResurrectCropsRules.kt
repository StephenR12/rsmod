package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.max

public object ResurrectCropsRules {
    /** 50% at level 78, scaling linearly to 75% at level 99. */
    public fun successChance(visibleMagicLevel: Int): Int =
        50 + (max(78, visibleMagicLevel).coerceAtMost(99) - 78) * 25 / 21
}
