package org.rsmod.content.minigames.inferno

/** The complete, cache-233 Inferno wave table. */
public object InfernoWaves {
    public const val FINAL_WAVE: Int = 69
    private val restWaves = setOf(3, 8, 17, 34)
    private val tiers =
        listOf(
            1 to InfernoMonster.Bat,
            3 to InfernoMonster.Blob,
            7 to InfernoMonster.Meleer,
            15 to InfernoMonster.Ranger,
            31 to InfernoMonster.Mager,
        )

    public fun composition(wave: Int): List<InfernoMonster> {
        require(wave in 1..FINAL_WAVE) { "Wave must be in 1..$FINAL_WAVE: $wave" }
        if (wave == 67) return listOf(InfernoMonster.Jad)
        if (wave == 68) return List(3) { InfernoMonster.Jad }
        if (wave == 69) return listOf(InfernoMonster.Zuk)
        if (wave in restWaves) return List(6) { InfernoMonster.Nibbler }

        val progression = wave - restWaves.count { it < wave }
        var remainder = progression
        val monsters = MutableList(3) { InfernoMonster.Nibbler }
        for ((value, monster) in tiers.asReversed()) {
            val count = (remainder / value).coerceAtMost(2)
            repeat(count) { monsters += monster }
            remainder -= count * value
        }
        check(remainder == 0) { "Wave did not fit Inferno tiers: $wave" }
        return monsters
    }
}

public enum class InfernoMonster {
    Nibbler,
    Bat,
    Blob,
    Meleer,
    Ranger,
    Mager,
    Jad,
    Zuk,
}
