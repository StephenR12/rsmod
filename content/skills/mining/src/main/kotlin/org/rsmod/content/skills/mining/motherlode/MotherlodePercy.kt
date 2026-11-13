package org.rsmod.content.skills.mining.motherlode

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.content.skills.mining.ProspectorOutfits
import org.rsmod.game.type.obj.ObjTypeList

/** Dated fixed nugget prices; native full-screen shop presentation remains unverified. */
public class MotherlodePercy
@Inject
constructor(private val types: ObjTypeList, private val storage: MotherlodeStorage) {
    public suspend fun talk(access: ProtectedAccess): Unit =
        with(access) {
            when (
                choice4(
                    "Buy equipment.",
                    0,
                    "Sell back Prospector clothing.",
                    1,
                    "Buy a mine upgrade.",
                    2,
                    "Leave.",
                    3,
                )
            ) {
                0 -> shop(this)
                1 -> {
                    var index =
                        choice5(
                            "Helmet: 32 nuggets.",
                            0,
                            "Jacket: 48 nuggets.",
                            1,
                            "Legs: 40 nuggets.",
                            2,
                            "Boots: 24 nuggets.",
                            3,
                            "Consumable packs.",
                            4,
                        )
                    if (index == 4)
                        index =
                            choice3(
                                "Soft clay pack: 8 nuggets.",
                                6,
                                "Bag full of gems: 32 nuggets.",
                                7,
                                "Leave.",
                                -1,
                            )
                    if (index >= 0) sell(this, index)
                }
                2 -> {
                    val index =
                        choice3(
                            "Upper level: 100 nuggets.",
                            0,
                            "Super Hopper: 50 nuggets.",
                            1,
                            "Larger sack: 200 nuggets.",
                            2,
                        )
                    upgrade(this, index)
                }
            }
        }

    public suspend fun shop(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val index =
                choice5(
                    "Prospector helmet: 40 nuggets.",
                    0,
                    "Prospector jacket: 60 nuggets.",
                    1,
                    "Prospector legs: 50 nuggets.",
                    2,
                    "Prospector boots: 30 nuggets.",
                    3,
                    "Other equipment.",
                    4,
                )
            if (index < 4) buy(this, index)
            else {
                val other =
                    choice5(
                        "Coal bag: 100 nuggets.",
                        4,
                        "Gem bag: 100 nuggets.",
                        5,
                        "Soft clay pack: 10 nuggets.",
                        6,
                        "Bag full of gems: 40 nuggets.",
                        7,
                        "Leave.",
                        -1,
                    )
                if (other >= 0) buy(this, other)
            }
        }

    public fun buy(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in GOODS.indices) return false
            val item = GOODS[index]
            val cost = COSTS[index]
            val equivalents =
                when (index) {
                    4 -> listOf(objs.coal_bag, objs.coal_bag_open)
                    5 -> listOf(objs.gem_bag, objs.gem_bag_open)
                    else -> emptyList()
                }
            if (equivalents.any { obj -> player.invMap.values.any { obj in it } }) {
                mes("You already own that bag.")
                return false
            }
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, MlmObjs.nugget.id, cost)
                    add(i, item.id, 1, vars = if (index < 4) PERCY_PURCHASED else 0)
                }
            if (!tx.success) mes("You need $cost golden nuggets and room for the item.")
            tx.success
        }

    public fun sell(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in GOODS.indices || index in 4..5) return false
            val slot =
                inv.indices.firstOrNull {
                    inv[it]?.id == GOODS[index].id &&
                        (index >= 6 || inv[it]?.vars == PERCY_PURCHASED)
                }
            if (slot == null) {
                mes("You can't sell this item back to Percy as you did not buy it from this shop.")
                return false
            }
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, GOODS[index].id, 1, slot = slot)
                    add(i, MlmObjs.nugget.id, COSTS[index] * 8 / 10)
                }
                .success
        }

    public fun upgrade(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (!player.members || index !in 0..2) return false
            val unlocked = vars[MlmVars.unlocks]
            if (index == 0 && player.statBase(stats.mining) < 57) {
                mes("You need an unboosted Mining level of 57 to unlock the upper level.")
                return false
            }
            if (index == 1 && unlocked and 1 == 0) {
                mes("Unlock the upper level before buying its hopper.")
                return false
            }
            if (
                index < 2 && unlocked and (1 shl index) != 0 || index == 2 && vars[MlmBits.big] != 0
            ) {
                mes("You already own that upgrade.")
                return false
            }
            val cost = listOf(100, 50, 200)[index]
            val tx = player.invTransaction(inv) { delete(select(inv), MlmObjs.nugget.id, cost) }
            if (!tx.success) {
                mes("You need $cost golden nuggets.")
                return false
            }
            if (index == 2) vars[MlmBits.big] = 1
            else vars[MlmVars.unlocks] = unlocked or (1 shl index)
            storage.refresh(player)
            true
        }

    public companion object {
        public val GOODS =
            listOf(
                ProspectorOutfits.hat,
                ProspectorOutfits.top,
                ProspectorOutfits.legs,
                ProspectorOutfits.boots,
                objs.coal_bag,
                objs.gem_bag,
                MlmObjs.clayPack,
                MlmObjs.gemPack,
            )
        public val COSTS = listOf(40, 60, 50, 30, 100, 100, 10, 40)
        public const val PERCY_PURCHASED: Int = 1
    }
}
