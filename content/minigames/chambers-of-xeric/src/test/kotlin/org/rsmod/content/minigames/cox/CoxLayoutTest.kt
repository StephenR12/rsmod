package org.rsmod.content.minigames.cox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoxLayoutTest {
    @Test
    fun `normal layout has three combat two puzzle prep and olm rooms`() {
        val route = CoxLayout.generate(42, challenge = false)
        assertEquals(7, route.size)
        assertEquals(3, route.dropLast(2).count(CoxRoom::combat))
        assertEquals(2, route.dropLast(2).count { !it.combat })
        assertEquals(listOf(CoxRoom.Prep, CoxRoom.Olm), route.takeLast(2))
        assertEquals(route.size, route.distinct().size)
    }

    @Test
    fun `normal layouts are deterministic but scoutable`() {
        assertEquals(CoxLayout.generate(123, false), CoxLayout.generate(123, false))
        assertNotEquals(CoxLayout.generate(123, false), CoxLayout.generate(124, false))
    }

    @Test
    fun `challenge mode contains every encounter in fixed order`() {
        val route = CoxLayout.generate(0, challenge = true)
        assertEquals(CoxLayout.challengeRoute, route)
        assertTrue(route.containsAll(CoxRoom.entries))
        assertFalse(route.dropLast(2).isEmpty())
    }
}
