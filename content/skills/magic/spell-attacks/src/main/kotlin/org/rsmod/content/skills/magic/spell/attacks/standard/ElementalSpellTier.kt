package org.rsmod.content.skills.magic.spell.attacks.standard

/**
 * Base maximum-hit scaling for standard elemental combat spells.
 *
 * Since the 29 May 2024 combat rebalance, an elemental spell scales to the strongest spell the
 * player can cast within the same tier. The lowest value remains available for callers below the
 * tier's first requirement; spell requirement checks happen before damage calculation.
 */
internal enum class ElementalSpellTier(private vararg val thresholds: Pair<Int, Int>) {
    Strike(1 to 2, 5 to 4, 9 to 6, 13 to 8),
    Bolt(17 to 9, 23 to 10, 29 to 11, 35 to 12),
    Blast(41 to 13, 47 to 14, 53 to 15, 59 to 16),
    Wave(62 to 17, 65 to 18, 70 to 19, 75 to 20),
    Surge(81 to 21, 85 to 22, 90 to 23, 95 to 24);

    fun baseMaxHit(magicLevel: Int): Int {
        return thresholds.lastOrNull { (requiredLevel) -> magicLevel >= requiredLevel }?.second
            ?: thresholds.first().second
    }
}
