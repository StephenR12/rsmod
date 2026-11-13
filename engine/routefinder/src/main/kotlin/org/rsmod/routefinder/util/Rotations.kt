package org.rsmod.routefinder.util

public object Rotations {
    public fun rotate(angle: Int, dimensionA: Int, dimensionB: Int): Int =
        if (angle and 0x1 != 0) {
            dimensionB
        } else {
            dimensionA
        }

    public fun rotate(angle: Int, blockAccessFlags: Int): Int {
        // Cache locs can carry additional bits, for example 0x1E on directional chests.
        // Only the four approach directions participate in this circular shift.
        val sides = blockAccessFlags and 0xF
        return ((sides shl angle) and 0xF) or (sides ushr (4 - angle))
    }
}
