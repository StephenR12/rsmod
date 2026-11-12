package org.rsmod.content.minigames.fightcaves

/** Cache-233.4 Fight Cave wave and rotation rules. */
public object FightCaveWaves {
    public const val FINAL_WAVE: Int = 63

    /** Lowest to highest tier, with each new tier replacing a full two-of-each lower stack. */
    private val tiers =
        listOf(
            1 to FightCaveMonster.TzKih,
            3 to FightCaveMonster.TzKek,
            7 to FightCaveMonster.TokXil,
            15 to FightCaveMonster.YtMejKot,
            31 to FightCaveMonster.KetZek,
        )

    public fun composition(wave: Int): List<FightCaveMonster> {
        require(wave in 1..FINAL_WAVE) { "Wave must be in 1..$FINAL_WAVE: $wave" }
        if (wave == FINAL_WAVE) {
            return listOf(FightCaveMonster.TzTokJad)
        }
        var remainder = wave
        val descending = mutableListOf<FightCaveMonster>()
        for ((value, monster) in tiers.asReversed()) {
            val count = (remainder / value).coerceAtMost(2)
            repeat(count) { descending += monster }
            remainder -= count * value
        }
        check(remainder == 0) { "Wave did not fit the five Fight Cave tiers: $wave" }
        return descending
    }

    /**
     * The cycle published by the Wiki. The highest tier uses the wave's start; subsequent monsters
     * use subsequent entries, in descending level order.
     */
    public val spawnCycle: List<FightCaveSpawn> =
        listOf(
            FightCaveSpawn.SE,
            FightCaveSpawn.SW,
            FightCaveSpawn.C,
            FightCaveSpawn.NW,
            FightCaveSpawn.SW,
            FightCaveSpawn.SE,
            FightCaveSpawn.S,
            FightCaveSpawn.NW,
            FightCaveSpawn.C,
            FightCaveSpawn.SE,
            FightCaveSpawn.SW,
            FightCaveSpawn.S,
            FightCaveSpawn.NW,
            FightCaveSpawn.C,
            FightCaveSpawn.S,
        )

    public fun spawns(wave: Int, rotationStart: Int): List<FightCaveWaveSpawn> {
        require(rotationStart in spawnCycle.indices)
        val start = Math.floorMod(rotationStart + wave - 1, spawnCycle.size)
        return composition(wave).mapIndexed { index, monster ->
            FightCaveWaveSpawn(monster, spawnCycle[(start + index) % spawnCycle.size], index)
        }
    }

    /**
     * Historical UTC entry schedule. There are fifteen offsets in a sixteen-minute period; the
     * first and last minute both select rotation 4 (cycle offset 12).
     */
    public fun rotationStart(epochMinute: Long): Int =
        ENTRY_OFFSETS[Math.floorMod(epochMinute, ENTRY_OFFSETS.size.toLong()).toInt()]

    private val ENTRY_OFFSETS = intArrayOf(12, 13, 14, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
}

public enum class FightCaveMonster {
    TzKih,
    TzKek,
    TokXil,
    YtMejKot,
    KetZek,
    TzTokJad,
}

public enum class FightCaveSpawn(public val localX: Int, public val localZ: Int) {
    NW(10, 50),
    C(30, 30),
    SE(50, 25),
    S(35, 15),
    SW(10, 15),
}

public data class FightCaveWaveSpawn(
    val monster: FightCaveMonster,
    val spawn: FightCaveSpawn,
    val ordinal: Int,
)
