package org.rsmod.api.obj.plugin

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj

/** Selects a non-standard inventory destination for a ground-object Take operation. */
public interface ObjTakeDestination {
    /** Higher-priority destinations are considered first. */
    public val priority: Int
        get() = 0

    /** Returns the destination inventory, or `null` when this policy does not accept [obj]. */
    public fun destination(player: Player, obj: Obj): Inventory?

    /** Runs after the ground object has been removed and the destination transaction committed. */
    public fun onTake(access: ProtectedAccess, obj: Obj) {}
}
