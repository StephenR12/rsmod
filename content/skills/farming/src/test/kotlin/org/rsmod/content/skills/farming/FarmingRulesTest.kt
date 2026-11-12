package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingRulesTest {
    @Test
    fun `harvest chance interpolates exact wiki endpoints`() {
        assertEquals(26, FarmingRules.harvestSaveChance(1, 25, 80))
        assertEquals(81, FarmingRules.harvestSaveChance(99, 25, 80))
    }

    @Test
    fun `magic secateurs floor boosted constants before interpolation`() {
        assertEquals(28, FarmingRules.harvestSaveChance(1, 25, 80, 10))
        assertEquals(89, FarmingRules.harvestSaveChance(99, 25, 80, 10))
    }

    @Test
    fun `herb disease thresholds match every compost tier`() {
        assertThreshold(27, FarmingCompost.None, 27)
        assertThreshold(27, FarmingCompost.Compost, 14)
        assertThreshold(27, FarmingCompost.Supercompost, 6)
        assertThreshold(27, FarmingCompost.Ultracompost, 3)
    }

    @Test
    fun `fruit tree disease thresholds match every compost tier`() {
        assertThreshold(18, FarmingCompost.None, 18)
        assertThreshold(18, FarmingCompost.Compost, 9)
        assertThreshold(18, FarmingCompost.Supercompost, 4)
        assertThreshold(18, FarmingCompost.Ultracompost, 2)
    }

    private fun assertThreshold(base: Int, compost: FarmingCompost, successes: Int) {
        assertTrue(FarmingRules.diseaseRollSucceeds(base, compost, successes - 1))
        assertFalse(FarmingRules.diseaseRollSucceeds(base, compost, successes))
    }
}
