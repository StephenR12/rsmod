package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class MiningGemPreTableEntry(
    val obj: ObjType?,
    val weight: Int,
    val displayName: String?,
    val article: String?,
) {
    init {
        require((obj == null) == (displayName == null && article == null))
    }
}

public object MiningGemPreTable {
    public const val TOTAL_WEIGHT: Int = 128
    public const val NORMAL_DENOMINATOR: Int = 32_768
    public const val CHARGED_GLORY_DENOMINATOR: Int = 8_216

    public val all =
        listOf(
            MiningGemPreTableEntry(obj = null, weight = 70, displayName = null, article = null),
            MiningGemPreTableEntry(objs.uncut_sapphire, 32, "sapphire", "a"),
            MiningGemPreTableEntry(objs.uncut_emerald, 16, "emerald", "an"),
            MiningGemPreTableEntry(objs.uncut_ruby, 8, "ruby", "a"),
            MiningGemPreTableEntry(objs.uncut_diamond, 2, "diamond", "a"),
        )

    init {
        require(all.sumOf { it.weight } == TOTAL_WEIGHT)
    }

    public fun at(roll: Int): MiningGemPreTableEntry {
        require(roll in 0 until TOTAL_WEIGHT)
        var cursor = roll
        for (entry in all) {
            if (cursor < entry.weight) {
                return entry
            }
            cursor -= entry.weight
        }
        error("Mining gem pre-table roll was not covered: $roll")
    }
}
