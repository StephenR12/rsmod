package org.rsmod.api.player

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType

public object HallowedLockpick : ObjReferences() {
    public val full: ObjType = find("strange_old_lockpick_full")
    public val used: ObjType = find("strange_old_lockpick")

    public fun charges(item: InvObj?): Int =
        when (item?.id) {
            full.id -> 50
            used.id -> item.vars.coerceIn(1, 49)
            else -> 0
        }

    public fun consume(inv: Inventory, slot: Int): Boolean {
        val left = charges(inv[slot])
        if (left == 0) return false
        inv[slot] = if (left == 1) null else InvObj(used, 1, left - 1)
        return true
    }
}
