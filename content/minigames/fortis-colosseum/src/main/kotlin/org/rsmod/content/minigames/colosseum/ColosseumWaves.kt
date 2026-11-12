package org.rsmod.content.minigames.colosseum

public enum class ColosseumMonster {
    Meleer,
    Ranger,
    Mager,
    Shaman,
    Jaguar,
    Javelin,
    Manticore,
    Shockwave,
    Minotaur,
    Sol,
}

public data class ColosseumWave(
    val initial: List<ColosseumMonster>,
    val reinforcements: List<ColosseumMonster> = emptyList(),
)

/** Cache-233 Fortis Colosseum wave roster. Reinforcements enter 40 seconds after wave start. */
public object ColosseumWaves {
    public const val FINAL_WAVE: Int = 12
    private val trio =
        listOf(ColosseumMonster.Meleer, ColosseumMonster.Ranger, ColosseumMonster.Mager)
    private val jaguar = listOf(ColosseumMonster.Jaguar)
    private val jaguarShaman = listOf(ColosseumMonster.Jaguar, ColosseumMonster.Shaman)
    private val minotaur = listOf(ColosseumMonster.Minotaur)
    private val minotaurShaman = listOf(ColosseumMonster.Minotaur, ColosseumMonster.Shaman)
    private val waves =
        listOf(
            ColosseumWave(trio + ColosseumMonster.Shaman, jaguar),
            ColosseumWave(trio + listOf(ColosseumMonster.Shaman, ColosseumMonster.Javelin), jaguar),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Shaman,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Javelin,
                    ),
                jaguar,
            ),
            ColosseumWave(
                trio + listOf(ColosseumMonster.Shaman, ColosseumMonster.Manticore),
                jaguarShaman,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Shaman,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                    ),
                jaguarShaman,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Shaman,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                    ),
                jaguarShaman,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Shockwave,
                    ),
                minotaur,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Shockwave,
                    ),
                minotaur,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Manticore,
                    ),
                minotaur,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Manticore,
                    ),
                minotaurShaman,
            ),
            ColosseumWave(
                trio +
                    listOf(
                        ColosseumMonster.Javelin,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Manticore,
                        ColosseumMonster.Shockwave,
                    ),
                minotaurShaman,
            ),
            ColosseumWave(listOf(ColosseumMonster.Sol)),
        )

    public fun get(number: Int): ColosseumWave {
        require(number in 1..FINAL_WAVE) { "Invalid Colosseum wave: $number" }
        return waves[number - 1]
    }
}
