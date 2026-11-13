package org.rsmod.api.player.worn

import org.rsmod.api.type.refs.varbit.VarBitReferences

internal object BarbarianHastaTraining : VarBitReferences() {
    val completed = find("brut_smith_hasta")
    // Standard, gilded and poisoned hasta variants, the Zamorakian hasta and the dragon hunter lance.
    val weapons = setOf(11367, 11369, 11371, 11373, 11375, 11377, 11379, 11381, 11382, 11384, 11386, 11388, 11389, 11391, 11393, 11395, 11396, 11398, 11400, 11402, 11403, 11405, 11407, 11409, 11410, 11412, 11414, 11416, 11417, 11419, 11889, 20161, 22731, 22734, 22737, 22740, 22743, 22978)
}
