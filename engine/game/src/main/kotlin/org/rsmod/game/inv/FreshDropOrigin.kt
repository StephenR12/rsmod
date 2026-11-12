package org.rsmod.game.inv

import org.rsmod.map.CoordGrid

/** Packs a ground-drop origin into inventory object vars without exposing it to the client. */
public object FreshDropOrigin {
    private const val PRESENT_BIT: Int = 1 shl 30
    private const val COORD_MASK: Int = (1 shl 30) - 1

    /** Marks [coords] as the immutable origin associated with a fresh dropped item. */
    public fun pack(coords: CoordGrid): Int = PRESENT_BIT or coords.packed

    /** Returns the recorded origin, or `null` for an ordinary/unprovenanced item. */
    public fun unpack(vars: Int): CoordGrid? {
        if (vars and PRESENT_BIT == 0) {
            return null
        }
        return CoordGrid(vars and COORD_MASK)
    }

    /** Whether [coords] lies in the same-level square centered on the recorded origin. */
    public fun contains(vars: Int, coords: CoordGrid, radius: Int): Boolean {
        require(radius >= 0) { "Fresh-drop radius must not be negative: $radius" }
        val origin = unpack(vars) ?: return false
        return origin.level == coords.level && origin.chebyshevDistance(coords) <= radius
    }
}
