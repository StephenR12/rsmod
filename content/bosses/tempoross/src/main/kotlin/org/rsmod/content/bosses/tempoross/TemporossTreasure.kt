package org.rsmod.content.bosses.tempoross

import org.rsmod.api.random.GameRandom
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

/** The reward pool samples the player's base Fishing level when each permit is spent. */
internal object TemporossTreasure {
    private val fish = intArrayOf(345, 353, 349, 331, 359, 377, 363, 371, 383, 395, 389)
    private val fishLevels = intArrayOf(40, 46, 50, 76, 79, 81)
    private val quantities = listOf(30..40, 20..30, 10..20, 7..12, 5..10)

    private data class Drop(val id: Int, val quantity: IntRange, val weight: Int)

    private val casket =
        listOf(
            Drop(1147, 1..1, 9), // Rune med helm
            Drop(1163, 1..1, 9), // Rune full helm
            Drop(1113, 1..1, 9), // Rune chainbody
            Drop(1127, 2..3, 1), // Rune platebody
            Drop(1079, 2..3, 1), // Rune platelegs
            Drop(1093, 2..3, 1), // Rune plateskirt
            Drop(1185, 3..5, 1), // Rune sq shield
            Drop(1201, 3..5, 1), // Rune kiteshield
            Drop(1637, 10..14, 9), // Sapphire ring
            Drop(1639, 7..11, 9), // Emerald ring
            Drop(1641, 6..8, 9), // Ruby ring
            Drop(1643, 3..4, 9), // Diamond ring
            Drop(1656, 10..14, 9), // Sapphire necklace
            Drop(1658, 7..11, 9), // Emerald necklace
            Drop(1660, 6..8, 9), // Ruby necklace
            Drop(1662, 3..4, 9), // Diamond necklace
            Drop(11071, 10..14, 9), // Sapphire bracelet
            Drop(11076, 7..11, 9), // Emerald bracelet
            Drop(11085, 6..8, 9), // Ruby bracelet
            Drop(11092, 3..4, 9), // Diamond bracelet
            Drop(1637, 20..28, 1), // Sapphire ring
            Drop(1639, 14..22, 1), // Emerald ring
            Drop(1641, 12..16, 1), // Ruby ring
            Drop(1643, 6..8, 1), // Diamond ring
            Drop(1656, 20..28, 1), // Sapphire necklace
            Drop(1658, 14..22, 1), // Emerald necklace
            Drop(1660, 12..16, 1), // Ruby necklace
            Drop(1662, 4..8, 1), // Diamond necklace
            Drop(11071, 20..28, 1), // Sapphire bracelet
            Drop(11076, 14..22, 1), // Emerald bracelet
            Drop(11085, 12..16, 1), // Ruby bracelet
            Drop(11092, 3..8, 1), // Diamond bracelet
            Drop(2355, 50..100, 9), // Silver bar
            Drop(2357, 50..100, 9), // Gold bar
            Drop(1625, 25..50, 9), // Uncut opal
            Drop(1627, 25..50, 9), // Uncut jade
            Drop(1629, 10..15, 9), // Uncut red topaz
            Drop(1623, 20..30, 1), // Uncut sapphire
            Drop(1621, 15..25, 1), // Uncut emerald
            Drop(1619, 10..20, 1), // Uncut ruby
            Drop(1617, 8..13, 1), // Uncut diamond
            Drop(1631, 2..3, 1), // Uncut dragonstone
            Drop(995, 4000..6000, 9), // Coins
            Drop(995, 25000..50000, 1), // Coins
            Drop(987, 1..1, 9), // Loop half of key
            Drop(985, 1..1, 9), // Tooth half of key
        )

    fun casket(random: GameRandom, types: ObjTypeList): InvObj {
        var roll = random.of(230)
        for (drop in casket) {
            if (roll < drop.weight) {
                val type = types.getValue(drop.id)
                val reward = if (drop.id == 995) type else types.cert(type)
                return InvObj(reward, random.of(drop.quantity))
            }
            roll -= drop.weight
        }
        error("Invalid casket roll")
    }

    fun pool(random: GameRandom, types: ObjTypeList, level: Int): InvObj {
        fun noted(id: Int, quantity: IntRange) =
            InvObj(types.cert(types.getValue(id)), random.of(quantity))
        return when (random.of(8000)) {
            0 -> InvObj(types.getValue(25602), 1)
            1 -> InvObj(types.getValue(21028), 1)
            in 2..6 -> InvObj(types.getValue(25576), 1)
            in 7..11 -> InvObj(types.getValue(25559), 1)
            in 12..31 -> InvObj(types.getValue(25580), 1)
            in 32..51 -> InvObj(types.getValue(25582), 1)
            in 52..200 -> InvObj(types.getValue(25578), random.of(5..9))
            in 201..2200 -> InvObj(types.getValue(25588), random.of(32..64))
            in 2201..2600 -> InvObj(types.getValue(25590), 1)
            in 2601..2950 -> noted(960, 20..30)
            in 2951..3200 -> noted(8778, 15..25)
            in 3201..3400 -> noted(401, 20..60)
            in 3401..3500 -> InvObj(types.getValue(1539), random.of(300..500))
            in 3501..3600 -> InvObj(types.getValue(314), random.of(800..1600))
            in 3601..3700 -> InvObj(types.getValue(313), random.of(800..1600))
            else -> {
                val group = fishLevels.count { level >= it }
                val position =
                    when (random.of(40)) {
                        in 0..9 -> 0
                        in 10..18 -> 1
                        in 19..26 -> 2
                        in 27..33 -> 3
                        else -> 4
                    }
                noted(fish[group + position], quantities[position])
            }
        }
    }
}
