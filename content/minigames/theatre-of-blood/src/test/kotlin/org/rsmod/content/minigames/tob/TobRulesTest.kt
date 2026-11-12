package org.rsmod.content.minigames.tob

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TobRulesTest {
    @Test
    fun `route contains the six encounters in official order`() {
        assertEquals(
            listOf(
                TobRoom.Maiden,
                TobRoom.Bloat,
                TobRoom.Nylocas,
                TobRoom.Sotetseg,
                TobRoom.Xarpus,
                TobRoom.Verzik,
            ),
            TobRules.route,
        )
    }

    @Test
    fun `entry mode has reduced common loot and no uniques`() {
        assertEquals(20, TobRules.commonLootPercent(TobMode.Entry))
        assertFalse(TobRules.canRollUnique(TobMode.Entry))
    }

    @Test
    fun `normal and hard modes can roll uniques`() {
        assertTrue(TobRules.canRollUnique(TobMode.Normal))
        assertTrue(TobRules.canRollUnique(TobMode.Hard))
        assertEquals(115, TobRules.commonLootPercent(TobMode.Hard))
    }
}
