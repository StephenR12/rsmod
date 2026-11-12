package org.rsmod.content.skills.woodcutting

import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class InfernalAxeState(val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid infernal axe charges: $charges" }
    }

    public fun pack(): Int = 0.withBits(varobjs.infernal_tool_charges.bits, charges)

    public fun consume(obj: InvObj, depletedType: ObjType): InvObj {
        val remaining = charges - 1
        return if (remaining == 0) {
            InvObj(depletedType)
        } else {
            obj.copy(vars = InfernalAxeState(remaining).pack())
        }
    }

    public companion object {
        public const val MAX_CHARGES: Int = 5_000

        public fun fromCharged(obj: InvObj): InfernalAxeState {
            val stored = obj.vars.getBits(varobjs.infernal_tool_charges.bits)
            return InfernalAxeState(if (stored == 0) MAX_CHARGES else stored)
        }
    }
}
