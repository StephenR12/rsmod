package org.rsmod.content.skills.magic.spell.utility.standard

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class FarmingPatchSpellsTest {
    @Test
    fun GameTestState.`resurrection chance is 50 percent at level 78`() {
        assertEquals(50, ResurrectCropsRules.successChance(78))
    }

    @Test
    fun GameTestState.`resurrection chance scales to 75 percent at level 99`() {
        assertEquals(75, ResurrectCropsRules.successChance(99))
    }

    @Test
    fun GameTestState.`resurrection chance clamps boosted and drained levels`() {
        assertEquals(50, ResurrectCropsRules.successChance(1))
        assertEquals(75, ResurrectCropsRules.successChance(120))
    }
}
