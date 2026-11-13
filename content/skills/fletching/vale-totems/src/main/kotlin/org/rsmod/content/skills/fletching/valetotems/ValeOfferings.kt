package org.rsmod.content.skills.fletching.valetotems

import org.rsmod.api.random.GameRandom

internal data class ValeDrop(
    val id: Int,
    val low: Int = 1,
    val high: Int = low,
    val weight: Int = 1,
)

internal object ValeOfferings {
    private val seeds =
        listOf(
            listOf(
                ValeDrop(5313, 1, 2, 103420),
                ValeDrop(5314, 1, 2, 68556),
                ValeDrop(5288, 1, 2, 68471),
                ValeDrop(5315, 1, 2, 41544),
                ValeDrop(5289, 1, 2, 40942),
                ValeDrop(5100, 3, 6, 27786),
                ValeDrop(5312, 3, 6, 20912),
                ValeDrop(5287, 1, 2, 20797),
                ValeDrop(5316, 1, 2, 20736),
                ValeDrop(22877, 1, 2, 733),
                ValeDrop(5321, 5, 8, 328),
                ValeDrop(5283, 1, 2, 318),
                ValeDrop(5102, 7, 11, 39),
            ),
            listOf(
                ValeDrop(5313, 1, 2, 81764),
                ValeDrop(5288, 1, 2, 54436),
                ValeDrop(5314, 1, 2, 54087),
                ValeDrop(5289, 1, 2, 32739),
                ValeDrop(5315, 1, 2, 32536),
                ValeDrop(5100, 3, 6, 24525),
                ValeDrop(5287, 1, 2, 18007),
                ValeDrop(5312, 3, 6, 17949),
                ValeDrop(5316, 1, 2, 16246),
                ValeDrop(5102, 7, 11, 4268),
                ValeDrop(5283, 1, 2, 3746),
                ValeDrop(5321, 5, 8, 3034),
                ValeDrop(22877, 1, 2, 557),
            ),
            listOf(
                ValeDrop(5313, 1, 2, 72148),
                ValeDrop(5288, 1, 2, 50564),
                ValeDrop(5314, 1, 2, 47628),
                ValeDrop(5100, 3, 6, 41411),
                ValeDrop(5102, 7, 11, 37383),
                ValeDrop(5283, 1, 2, 30304),
                ValeDrop(5312, 3, 6, 29219),
                ValeDrop(5287, 1, 2, 29040),
                ValeDrop(5289, 1, 2, 28183),
                ValeDrop(5315, 1, 2, 28129),
                ValeDrop(5321, 5, 8, 25261),
                ValeDrop(5316, 1, 2, 14171),
                ValeDrop(22877, 1, 2, 464),
            ),
            listOf(
                ValeDrop(5102, 7, 11, 30425),
                ValeDrop(5283, 1, 2, 24585),
                ValeDrop(5100, 3, 6, 20472),
                ValeDrop(5321, 5, 8, 20400),
                ValeDrop(5312, 3, 6, 13764),
                ValeDrop(5287, 1, 2, 13646),
                ValeDrop(5313, 1, 2, 9324),
                ValeDrop(5288, 1, 2, 7993),
                ValeDrop(5314, 1, 2, 5497),
                ValeDrop(5289, 1, 2, 3142),
                ValeDrop(5315, 1, 2, 3135),
                ValeDrop(5316, 1, 2, 1577),
                ValeDrop(22877, 1, 2, 63),
            ),
            listOf(
                ValeDrop(5102, 7, 11, 6637),
                ValeDrop(5283, 1, 2, 5192),
                ValeDrop(5321, 5, 8, 4289),
                ValeDrop(5100, 3, 6, 3963),
                ValeDrop(5287, 1, 2, 2627),
                ValeDrop(5312, 3, 6, 2619),
                ValeDrop(5288, 1, 2, 666),
                ValeDrop(5313, 1, 2, 367),
                ValeDrop(5314, 1, 2, 43),
                ValeDrop(5289, 1, 2, 5),
            ),
        )

    fun roll(random: GameRandom, level: Int, canReceiveBark: Boolean): ValeDrop {
        when (random.of(1000)) {
            in 0..4 -> return ValeDrop(31052)
            in 5..7 -> return ValeDrop(31043)
            in 8..9 -> return ValeDrop(31034)
        }
        val roll = random.of(if (canReceiveBark) 399 else 374)
        return when (roll) {
            in 0..79 -> ValeDrop(31047, 26, 32)
            in 80..144 -> ValeDrop(31032)
            in 145..194 -> ValeDrop(31045, 5, 6)
            in 195..229 -> ValeDrop(29381, 70, 90)
            in 230..244 -> ValeDrop(314, 400, 500)
            in 245..268 ->
                when (random.of(10)) {
                    in 0..2 -> ValeDrop(6051, 2, 3)
                    in 3..5 -> ValeDrop(6049, 3, 4)
                    in 6..7 -> ValeDrop(6047, 3, 4)
                    else -> ValeDrop(6045, 3, 4)
                }
            in 269..318 -> {
                val id =
                    when (random.of(100)) {
                        0 -> 5070
                        1 -> 5071
                        2 -> 5072
                        in 3..34 -> 5074
                        else -> 5073
                    }
                ValeDrop(id, if (level >= 80) 2 else 1)
            }
            in 319..373 -> {
                val table =
                    seeds[
                        when {
                            level >= 90 -> 0
                            level >= 80 -> 1
                            level >= 65 -> 2
                            level >= 50 -> 3
                            else -> 4
                        }]
                var ticket = random.of(table.sumOf { it.weight })
                table.first {
                    ticket -= it.weight
                    ticket < 0
                }
            }
            else -> ValeDrop(28134, 70, 90)
        }
    }
}
