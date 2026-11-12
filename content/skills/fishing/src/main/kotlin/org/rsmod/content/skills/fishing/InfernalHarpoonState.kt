package org.rsmod.content.skills.fishing

import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class InfernalHarpoonState(val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid infernal harpoon charges: $charges" }
    }

    public fun pack(): Int = 0.withBits(varobjs.infernal_tool_charges.bits, charges)

    public fun consume(obj: InvObj, depletedType: ObjType, count: Int = 1): InvObj {
        require(count > 0) { "Infernal harpoon charge consumption must be positive: $count" }
        val remaining = charges - count
        return if (remaining <= 0) {
            InvObj(depletedType)
        } else {
            obj.copy(vars = InfernalHarpoonState(remaining).pack())
        }
    }

    public companion object {
        public const val MAX_CHARGES: Int = 5_000

        public fun fromCharged(obj: InvObj): InfernalHarpoonState {
            val stored = obj.vars.getBits(varobjs.infernal_tool_charges.bits)
            return InfernalHarpoonState(if (stored == 0) MAX_CHARGES else stored)
        }
    }
}
