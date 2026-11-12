package org.rsmod.game.inv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class FreshDropOriginTest {
    @Test
    fun `packed origin round trips and enforces centered same-level area`() {
        val origin = CoordGrid(3_140, 3_209, 0)
        val vars = FreshDropOrigin.pack(origin)

        assertEquals(origin, FreshDropOrigin.unpack(vars))
        assertTrue(FreshDropOrigin.contains(vars, origin.translate(15, -15), radius = 15))
        assertFalse(FreshDropOrigin.contains(vars, origin.translateX(16), radius = 15))
        assertFalse(FreshDropOrigin.contains(vars, origin.translateLevel(1), radius = 15))
        assertNull(FreshDropOrigin.unpack(0))
        assertFalse(FreshDropOrigin.contains(0, origin, radius = 15))
    }
}
