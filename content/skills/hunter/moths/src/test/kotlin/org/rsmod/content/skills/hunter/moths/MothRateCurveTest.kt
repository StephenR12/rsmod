package org.rsmod.content.skills.hunter.moths

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MothRateCurveTest {
    @Test
    fun `success curves preserve exact cache-cutoff chart interpolation`() {
        assertEquals(201.0 / 256.0, MothRateCurve(20, 296, 65).chance(65))
        assertEquals(221.0 / 256.0, MothRateCurve(40, 316, 65).chance(65))
        assertEquals(209.0 / 256.0, MothRateCurve(0, 276, 75).chance(75))
        assertEquals(222.0 / 256.0, MothRateCurve(20, 286, 75).chance(75))
        assertEquals(1.0, MothRateCurve(20, 296, 65).chance(99))
        assertEquals(1.0, MothRateCurve(40, 316, 65).chance(99))
    }

    @Test
    fun `sunlight restoration floors twenty percent of base before adding six`() {
        assertEquals(6, MothEffects.sunlightRestoreAmount(1))
        assertEquals(22, MothEffects.sunlightRestoreAmount(80))
        assertEquals(25, MothEffects.sunlightRestoreAmount(99))
    }
}
