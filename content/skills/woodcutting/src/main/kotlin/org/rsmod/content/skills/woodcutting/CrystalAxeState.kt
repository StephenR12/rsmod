package org.rsmod.content.skills.woodcutting

import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class CrystalAxeState(val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid crystal axe charges: $charges" }
    }

    public fun pack(): Int = 0.withBits(varobjs.crystal_tool_charges.bits, charges)

    public fun consume(obj: InvObj, depletedType: ObjType): InvObj {
        val remaining = charges - 1
        return if (remaining == 0) {
            InvObj(depletedType)
        } else {
            obj.copy(vars = CrystalAxeState(remaining).pack())
        }
    }

    public companion object {
        public const val INITIAL_CHARGES: Int = 10_000
        public const val MAX_CHARGES: Int = 20_000

        public fun fromCharged(obj: InvObj): CrystalAxeState {
            val stored = obj.vars.getBits(varobjs.crystal_tool_charges.bits)
            return CrystalAxeState(if (stored == 0) INITIAL_CHARGES else stored)
        }
    }
}
