package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class GemRockReward(
    val obj: ObjType,
    val weight: Int,
    val displayName: String,
    val article: String,
)

public object GemRockRewards {
    public val all =
        listOf(
            GemRockReward(objs.uncut_opal, 60, "Opal", "an"),
            GemRockReward(objs.uncut_jade, 30, "Jade", "a"),
            GemRockReward(objs.uncut_red_topaz, 15, "Red topaz", "a"),
            GemRockReward(objs.uncut_sapphire, 9, "Sapphire", "a"),
            GemRockReward(objs.uncut_emerald, 5, "Emerald", "an"),
            GemRockReward(objs.uncut_ruby, 5, "Ruby", "a"),
            GemRockReward(objs.uncut_diamond, 4, "Diamond", "a"),
        )

    public val totalWeight: Int = all.sumOf { it.weight }

    public fun at(roll: Int): GemRockReward {
        require(roll in 0 until totalWeight)
        var cursor = roll
        for (reward in all) {
            if (cursor < reward.weight) {
                return reward
            }
            cursor -= reward.weight
        }
        error("Gem-rock roll was not covered: $roll")
    }
}
