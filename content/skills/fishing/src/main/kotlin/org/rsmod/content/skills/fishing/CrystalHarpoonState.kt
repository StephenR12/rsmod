package org.rsmod.content.skills.fishing

import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class CrystalHarpoonState(val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid crystal harpoon charges: $charges" }
    }

    public fun pack(): Int = 0.withBits(varobjs.crystal_tool_charges.bits, charges)

    public fun consume(obj: InvObj, depletedType: ObjType, count: Int = 1): InvObj {
        require(count > 0) { "Crystal harpoon charge consumption must be positive: $count" }
        val remaining = charges - count
        return if (remaining <= 0) {
            InvObj(depletedType)
        } else {
            obj.copy(vars = CrystalHarpoonState(remaining).pack())
        }
    }

    public companion object {
        public const val INITIAL_CHARGES: Int = 10_000
        public const val MAX_CHARGES: Int = 20_000

        public fun fromCharged(obj: InvObj): CrystalHarpoonState {
            val stored = obj.vars.getBits(varobjs.crystal_tool_charges.bits)
            return CrystalHarpoonState(if (stored == 0) INITIAL_CHARGES else stored)
        }
    }
}
