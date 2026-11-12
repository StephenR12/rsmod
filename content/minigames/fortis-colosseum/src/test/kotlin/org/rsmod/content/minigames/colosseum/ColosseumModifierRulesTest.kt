package org.rsmod.content.minigames.colosseum

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ColosseumModifierRulesTest {
    @Test
    fun `first offer is canonical`() {
        assertEquals(
            listOf(
                ColosseumModifier.Relentless,
                ColosseumModifier.Blasphemy,
                ColosseumModifier.Frailty,
            ),
            ColosseumModifierRules.firstWave(),
        )
    }

    @Test
    fun `early-only modifiers disappear at wave seven`() {
        val active =
            ColosseumModifier.entries.associateWith {
                if (it == ColosseumModifier.RedFlag || it == ColosseumModifier.DynamicDuo) 0
                else it.maxLevel
            }
        val offers = ColosseumModifierRules.offers(7, active)
        assertFalse(ColosseumModifier.RedFlag in offers)
        assertFalse(ColosseumModifier.DynamicDuo in offers)
    }

    @Test
    fun `doom limits tighten by tier`() {
        assertEquals(listOf(20, 15, 10), (1..3).map(ColosseumModifierRules::doomLimit))
    }
}
