package org.rsmod.content.minigames.colosseum

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ColosseumWavesTest {
    @Test
    fun `all twelve waves match the canonical roster`() {
        assertEquals(listOf(Meleer, Ranger, Mager, Shaman), ColosseumWaves.get(1).initial)
        assertEquals(listOf(Jaguar), ColosseumWaves.get(1).reinforcements)
        assertEquals(2, ColosseumWaves.get(3).initial.count { it == Javelin })
        assertEquals(listOf(Jaguar, Shaman), ColosseumWaves.get(6).reinforcements)
        assertEquals(listOf(Minotaur), ColosseumWaves.get(9).reinforcements)
        assertEquals(listOf(Minotaur, Shaman), ColosseumWaves.get(11).reinforcements)
        assertEquals(listOf(Sol), ColosseumWaves.get(12).initial)
    }

    @Test
    fun `invalid waves are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ColosseumWaves.get(0) }
        assertThrows(IllegalArgumentException::class.java) { ColosseumWaves.get(13) }
    }

    private companion object {
        val Meleer = ColosseumMonster.Meleer
        val Ranger = ColosseumMonster.Ranger
        val Mager = ColosseumMonster.Mager
        val Shaman = ColosseumMonster.Shaman
        val Jaguar = ColosseumMonster.Jaguar
        val Javelin = ColosseumMonster.Javelin
        val Minotaur = ColosseumMonster.Minotaur
        val Sol = ColosseumMonster.Sol
    }
}
