package org.rsmod.content.skills.firemaking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class FiremakingRestrictedAreasTest {
    @Test
    fun `dwarven restriction follows both lobes and their inclusive boundary`() {
        val message = "The dwarves won't be happy if you light a fire here."
        assertEquals(message, FiremakingRestrictedAreas.messageAt(CoordGrid(3000, 3430, 0)))
        assertEquals(message, FiremakingRestrictedAreas.messageAt(CoordGrid(2900, 3500, 0)))
        assertEquals(message, FiremakingRestrictedAreas.messageAt(CoordGrid(2944, 3417, 0)))
        assertNull(FiremakingRestrictedAreas.messageAt(CoordGrid(2900, 3430, 0)))
        assertNull(FiremakingRestrictedAreas.messageAt(CoordGrid(3073, 3500, 0)))
        assertNull(FiremakingRestrictedAreas.messageAt(CoordGrid(3000, 3430, 1)))
    }

    @Test
    fun `gilbert and jiggig rectangles include their published edges`() {
        assertEquals(
            "A colourful magical force won't let you light a fire here.",
            FiremakingRestrictedAreas.messageAt(CoordGrid(3118, 3390, 0)),
        )
        assertEquals(
            "A colourful magical force won't let you light a fire here.",
            FiremakingRestrictedAreas.messageAt(CoordGrid(3136, 3408, 0)),
        )
        assertEquals(
            "The ogres won't be happy if you light a fire here.",
            FiremakingRestrictedAreas.messageAt(CoordGrid(2432, 3008, 0)),
        )
        assertEquals(
            "The ogres won't be happy if you light a fire here.",
            FiremakingRestrictedAreas.messageAt(CoordGrid(2496, 3072, 0)),
        )
        assertNull(FiremakingRestrictedAreas.messageAt(CoordGrid(3117, 3390, 0)))
        assertNull(FiremakingRestrictedAreas.messageAt(CoordGrid(2497, 3072, 0)))
    }
}
