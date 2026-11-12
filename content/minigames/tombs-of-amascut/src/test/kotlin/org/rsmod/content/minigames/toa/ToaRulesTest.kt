package org.rsmod.content.minigames.toa

import kotlin.test.Test
import kotlin.test.assertEquals

class ToaRulesTest {
    @Test
    fun `raid level scales stats by wiki formula`() {
        assertEquals(100, ToaRules.statPercent(0))
        assertEquals(160, ToaRules.statPercent(150))
        assertEquals(220, ToaRules.statPercent(300))
    }

    @Test
    fun `damage scaling caps at two hundred fifty percent`() {
        assertEquals(250, ToaRules.damagePercent(600))
    }
}
