package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

public object VarrockMining {
    public fun roll(player: Player, minimumTier: Int, random: GameRandom): Boolean {
        if (!player.members) return false
        val tier =
            when (player.torso?.id) {
                objs.diary_varrock_armour_1.id -> 1
                objs.diary_varrock_armour_2.id -> 2
                objs.diary_varrock_armour_3.id -> 3
                objs.diary_varrock_armour_4.id -> 4
                else -> return false
            }
        return tier >= minimumTier && random.of(10) == 0
    }

    public fun tierFor(product: ObjType): Int =
        when (product.id) {
            434,
            436,
            438,
            668,
            3211,
            440,
            442,
            453,
            444 -> 1
            1761,
            447,
            13356,
            6971,
            6973,
            6975,
            6977,
            6979,
            6981,
            6983 -> 2
            449 -> 3
            451,
            21347 -> 4
            else -> Int.MAX_VALUE
        }
}
