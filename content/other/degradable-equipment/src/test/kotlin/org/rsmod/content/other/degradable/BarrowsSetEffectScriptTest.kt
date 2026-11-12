package org.rsmod.content.other.degradable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BarrowsSetEffectScriptTest {
    @Test
    fun `Dharok recoil returns fifteen percent with a minimum of one`() {
        assertEquals(1, BarrowsSetEffectScript.reflectedDamage(1))
        assertEquals(1, BarrowsSetEffectScript.reflectedDamage(9))
        assertEquals(3, BarrowsSetEffectScript.reflectedDamage(20))
        assertEquals(15, BarrowsSetEffectScript.reflectedDamage(100))
    }

    @Test
    fun `Dharok recoil rejects non-damaging hits`() {
        assertFailsWith<IllegalArgumentException> { BarrowsSetEffectScript.reflectedDamage(0) }
    }
}
