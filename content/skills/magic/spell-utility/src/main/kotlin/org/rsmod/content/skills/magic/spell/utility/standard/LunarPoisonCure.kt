package org.rsmod.content.skills.magic.spell.utility.standard

import kotlin.math.min

/** Shared cache-233 poison-state transition used by the Lunar curing spell family. */
public object LunarPoisonCure {
    public const val VENOM_THRESHOLD: Int = 1_000_000
    public const val VENOM_OFFSET: Int = 999_997
    public const val MAX_VENOM_DAMAGE: Int = 20
    public const val POISON_STATE_PER_DAMAGE: Int = 5

    /** Converts venom to poison with the same next damage; ordinary poison is fully cured. */
    public fun curedState(poisonState: Int): Int {
        if (poisonState < VENOM_THRESHOLD) {
            return 0
        }
        return venomDamage(poisonState) * POISON_STATE_PER_DAMAGE
    }

    public fun venomDamage(poisonState: Int): Int =
        min(MAX_VENOM_DAMAGE, (poisonState - VENOM_OFFSET) * 2)

    public fun poisonDamage(poisonState: Int): Int =
        (poisonState + POISON_STATE_PER_DAMAGE - 1) / POISON_STATE_PER_DAMAGE
}
