package org.rsmod.content.minigames.colosseum

public enum class ColosseumModifier(
    public val displayName: String,
    public val glory: Int,
    public val maxLevel: Int = 3,
) {
    Bees("Bees!", 200),
    Blasphemy("Blasphemy", 200),
    Doom("Doom", 200),
    DynamicDuo("Dynamic Duo", 200, 1),
    Frailty("Frailty", 200),
    Mantimayhem("Mantimayhem", 200),
    Myopia("Myopia", 200),
    Quartet("Quartet", 200, 1),
    RedFlag("Red Flag", 250, 1),
    Reentry("Reentry", 200),
    Relentless("Relentless", 200),
    Solarflare("Solarflare", 250),
    Totemic("Totemic", 200, 1),
    Volatility("Volatility", 200),
}

public object ColosseumModifierRules {
    public fun firstWave(): List<ColosseumModifier> =
        listOf(ColosseumModifier.Relentless, ColosseumModifier.Blasphemy, ColosseumModifier.Frailty)

    public fun offers(
        waveJustCompleted: Int,
        active: Map<ColosseumModifier, Int>,
    ): List<ColosseumModifier> {
        val eligible =
            ColosseumModifier.entries.filter { modifier ->
                (active[modifier] ?: 0) < modifier.maxLevel &&
                    when (modifier) {
                        ColosseumModifier.RedFlag,
                        ColosseumModifier.DynamicDuo -> waveJustCompleted < 7
                        else -> true
                    }
            }
        if (eligible.size <= 3) return eligible
        val start = Math.floorMod(waveJustCompleted * 5 + active.values.sum(), eligible.size)
        val ordered = eligible.drop(start) + eligible.take(start)
        return listOf(ordered[0], ordered[3 % ordered.size], ordered[6 % ordered.size])
            .distinct()
            .let { if (it.size == 3) it else ordered.take(3) }
    }

    public fun doomLimit(level: Int): Int =
        when (level) {
            1 -> 20
            2 -> 15
            else -> 10
        }

    public fun frailtyPercent(level: Int): Int =
        when (level) {
            1 -> 90
            2 -> 75
            else -> 50
        }

    public fun relentlessPercent(level: Int): Int = 100 + 15 * level
}
