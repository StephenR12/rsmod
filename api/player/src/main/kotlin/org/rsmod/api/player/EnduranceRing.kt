package org.rsmod.api.player

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** The same saved per-item charges control the potion effect and the run-energy passive. */
public object EnduranceRing {
    public const val MAX_CHARGES: Int = 1000

    public fun charges(item: InvObj?): Int =
        if (item?.id == objs.ring_of_endurance.id)
            item.vars.getBits(varobjs.stamina_charges.bits).coerceAtMost(MAX_CHARGES)
        else 0

    public fun withCharges(item: InvObj, amount: Int): InvObj {
        require(amount in 0..MAX_CHARGES)
        return if (amount == 0) InvObj(objs.ring_of_endurance_nocharges)
        else
            InvObj(
                objs.ring_of_endurance,
                1,
                item.vars.withBits(varobjs.stamina_charges.bits, amount),
            )
    }
}
