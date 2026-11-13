package org.rsmod.api.player

import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.inv.InvType

public object CostumeRoomInventory : InvReferences() {
    public val storage: InvType = find("poh_costumes")
}

/** Cache enum 5732 lists the eighteen consecutive Chompy hats in rank order. */
public fun Player.hasStoredChompyHat(index: Int): Boolean =
    index in 0..17 && invMap[CostumeRoomInventory.storage]?.any { it?.id == 2978 + index } == true
