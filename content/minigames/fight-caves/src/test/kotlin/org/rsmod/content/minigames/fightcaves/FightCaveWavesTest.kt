package org.rsmod.content.minigames.fightcaves

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FightCaveWavesTest {
    @Test
    fun `tier substitution progression produces canonical boundary waves`() {
        assertEquals(listOf(FightCaveMonster.TzKih), FightCaveWaves.composition(1))
        assertEquals(
            listOf(FightCaveMonster.TzKih, FightCaveMonster.TzKih),
            FightCaveWaves.composition(2),
        )
        assertEquals(listOf(FightCaveMonster.TzKek), FightCaveWaves.composition(3))
        assertEquals(listOf(FightCaveMonster.TokXil), FightCaveWaves.composition(7))
        assertEquals(listOf(FightCaveMonster.YtMejKot), FightCaveWaves.composition(15))
        assertEquals(listOf(FightCaveMonster.KetZek), FightCaveWaves.composition(31))
        assertEquals(
            listOf(FightCaveMonster.KetZek, FightCaveMonster.KetZek),
            FightCaveWaves.composition(62),
        )
        assertEquals(listOf(FightCaveMonster.TzTokJad), FightCaveWaves.composition(63))
    }

    @Test
    fun `all pre Jad waves have the published monster totals`() {
        val totals = (1..62).flatMap(FightCaveWaves::composition).groupingBy { it }.eachCount()
        assertEquals(48, totals[FightCaveMonster.TzKih])
        assertEquals(40, totals[FightCaveMonster.TzKek])
        assertEquals(36, totals[FightCaveMonster.TokXil])
        assertEquals(34, totals[FightCaveMonster.YtMejKot])
        assertEquals(33, totals[FightCaveMonster.KetZek])
    }

    @Test
    fun `spawn order advances once per wave then once per lower monster`() {
        val wave = FightCaveWaves.spawns(wave = 13, rotationStart = 0)
        assertEquals(
            listOf(FightCaveSpawn.NW, FightCaveSpawn.C, FightCaveSpawn.S),
            wave.map(FightCaveWaveSpawn::spawn),
        )
        assertEquals(
            listOf(FightCaveMonster.TokXil, FightCaveMonster.TzKek, FightCaveMonster.TzKek),
            wave.map(FightCaveWaveSpawn::monster),
        )
    }

    @Test
    fun `entry schedule contains fifteen offsets and repeats rotation four for two minutes`() {
        val starts = (0L..15L).map(FightCaveWaves::rotationStart)
        assertEquals(listOf(12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), starts)
    }
}
