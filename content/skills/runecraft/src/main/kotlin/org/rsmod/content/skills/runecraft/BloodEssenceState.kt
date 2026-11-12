package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class BloodEssenceState(val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid blood essence charges: $charges" }
    }

    public fun pack(): Int = 0.withBits(varobjs.blood_essence_charges.bits, charges)

    public companion object {
        public const val MAX_CHARGES: Int = 1_000

        public fun fromActive(obj: InvObj): BloodEssenceState {
            val stored = obj.vars.getBits(varobjs.blood_essence_charges.bits)
            return BloodEssenceState(if (stored == 0) MAX_CHARGES else stored)
        }
    }
}
