package org.rsmod.api.obj.plugin

import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

/** Selects a replacement object identity for an ordinary ground-object Take operation. */
public interface ObjTakeTransform {
    /** Higher-priority transforms are considered first. */
    public val priority: Int
        get() = 0

    /**
     * Returns the acquired object identity, or `null` when this policy does not transform [obj].
     */
    public fun transform(player: Player, obj: Obj): Int?
}
