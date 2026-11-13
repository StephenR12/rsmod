package org.rsmod.api.death

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.market.MarketPrices
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

/** The carried objects in the same value order used by the native items-kept-on-death preview. */
public class DeathKeeps @Inject constructor(private val prices: MarketPrices, private val objects: ObjTypeList) {
    public fun ordered(player: Player): List<InvObj> =
        (player.inv.filterNotNull() + player.worn.filterNotNull())
            .filterNot { objects[it].param(params.bond_item) }
            .sortedByDescending { prices[objects[it]] ?: 1 }

    public fun kept(player: Player, keepCount: Int): List<InvObj> {
        var remaining = keepCount
        return ordered(player).asSequence().takeWhile { remaining > 0 }.map { item ->
            val quantity = minOf(item.count,remaining)
            remaining -= quantity
            item.copy(count=quantity)
        }.toList()
    }
}
