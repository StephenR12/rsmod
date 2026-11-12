package org.rsmod.content.skills.magic.spell.attacks.standard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ElementalSpellTierTest {
    @Test
    fun `strike spells scale at each unlock boundary`() {
        assertScaling(
            ElementalSpellTier.Strike,
            1 to 2,
            4 to 2,
            5 to 4,
            8 to 4,
            9 to 6,
            12 to 6,
            13 to 8,
            99 to 8,
        )
    }

    @Test
    fun `bolt spells scale at each unlock boundary`() {
        assertScaling(
            ElementalSpellTier.Bolt,
            17 to 9,
            22 to 9,
            23 to 10,
            28 to 10,
            29 to 11,
            34 to 11,
            35 to 12,
            99 to 12,
        )
    }

    @Test
    fun `blast spells scale at each unlock boundary`() {
        assertScaling(
            ElementalSpellTier.Blast,
            41 to 13,
            46 to 13,
            47 to 14,
            52 to 14,
            53 to 15,
            58 to 15,
            59 to 16,
            99 to 16,
        )
    }

    @Test
    fun `wave spells scale at each unlock boundary`() {
        assertScaling(
            ElementalSpellTier.Wave,
            62 to 17,
            64 to 17,
            65 to 18,
            69 to 18,
            70 to 19,
            74 to 19,
            75 to 20,
            99 to 20,
        )
    }

    @Test
    fun `surge spells scale at each unlock boundary`() {
        assertScaling(
            ElementalSpellTier.Surge,
            81 to 21,
            84 to 21,
            85 to 22,
            89 to 22,
            90 to 23,
            94 to 23,
            95 to 24,
            99 to 24,
        )
    }

    private fun assertScaling(tier: ElementalSpellTier, vararg cases: Pair<Int, Int>) {
        for ((magicLevel, expectedMaxHit) in cases) {
            assertEquals(expectedMaxHit, tier.baseMaxHit(magicLevel), "magicLevel=$magicLevel")
        }
    }
}
