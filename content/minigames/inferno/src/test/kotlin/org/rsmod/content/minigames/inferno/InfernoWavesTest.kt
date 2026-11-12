package org.rsmod.content.minigames.inferno

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class InfernoWavesTest {
    @Test
    fun `all 69 waves have canonical compositions`() {
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Bat), wave(1))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Bat, Bat), wave(2))
        assertEquals(List(6) { Nibbler }, wave(3))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Blob), wave(4))
        assertEquals(List(6) { Nibbler }, wave(8))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Meleer), wave(9))
        assertEquals(List(6) { Nibbler }, wave(17))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Ranger), wave(18))
        assertEquals(List(6) { Nibbler }, wave(34))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Mager), wave(35))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Mager, Ranger), wave(50))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Mager, Ranger, Ranger), wave(65))
        assertEquals(listOf(Nibbler, Nibbler, Nibbler, Mager, Mager), wave(66))
        assertEquals(listOf(Jad), wave(67))
        assertEquals(listOf(Jad, Jad, Jad), wave(68))
        assertEquals(listOf(Zuk), wave(69))
        assertEquals((1..69).toList(), (1..69).filter { InfernoWaves.composition(it).isNotEmpty() })
    }

    @Test
    fun `invalid wave is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { InfernoWaves.composition(0) }
        assertThrows(IllegalArgumentException::class.java) { InfernoWaves.composition(70) }
    }

    private fun wave(number: Int) = InfernoWaves.composition(number)

    private companion object {
        val Nibbler = InfernoMonster.Nibbler
        val Bat = InfernoMonster.Bat
        val Blob = InfernoMonster.Blob
        val Meleer = InfernoMonster.Meleer
        val Ranger = InfernoMonster.Ranger
        val Mager = InfernoMonster.Mager
        val Jad = InfernoMonster.Jad
        val Zuk = InfernoMonster.Zuk
    }
}
