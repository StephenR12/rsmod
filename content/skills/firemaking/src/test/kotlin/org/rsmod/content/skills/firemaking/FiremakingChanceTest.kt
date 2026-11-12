package org.rsmod.content.skills.firemaking

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FiremakingChanceTest {
    @Test
    fun `cutoff success chart boundaries use 256 outcomes`() {
        assertEquals(65, Firemaking.successfulRolls(1))
        assertEquals(70, Firemaking.successfulRolls(2))
        assertEquals(74, Firemaking.successfulRolls(3))
        assertEquals(252, Firemaking.successfulRolls(42))
        assertEquals(256, Firemaking.successfulRolls(43))
        assertEquals(256, Firemaking.successfulRolls(99))

        assertTrue(Firemaking.rollSuccess(level = 1, roll = 64))
        assertFalse(Firemaking.rollSuccess(level = 1, roll = 65))
        assertTrue(Firemaking.rollSuccess(level = 43, roll = 255))
    }

    @Test
    fun `barbarian requirements add twenty levels and cap at ninety nine`() {
        assertEquals(21, Firemaking.barbarianLevel(ordinaryLevel = 1))
        assertEquals(95, Firemaking.barbarianLevel(ordinaryLevel = 75))
        assertEquals(99, Firemaking.barbarianLevel(ordinaryLevel = 90))
    }
}
