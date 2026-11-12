package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public data class GnomishFirelighterState(val colour: ColouredLogRecipe, val charges: Int) {
    init {
        require(charges in 1..MAX_CHARGES) { "Invalid gnomish firelighter charges: $charges" }
    }

    public fun toInvObj(): InvObj =
        InvObj(
            objs.gnomish_firelighter_charged,
            vars =
                0.withBits(varobjs.gnomish_firelighter_charges.bits, charges)
                    .withBits(
                        varobjs.gnomish_firelighter_colour.bits,
                        ColouredLogRecipes.all.indexOf(colour) + 1,
                    ),
        )

    public companion object {
        public const val MAX_CHARGES: Int = 1_000

        public fun from(obj: InvObj?): GnomishFirelighterState? {
            if (obj?.id != objs.gnomish_firelighter_charged.id) {
                return null
            }
            val charges = obj.vars.getBits(varobjs.gnomish_firelighter_charges.bits)
            val colourCode = obj.vars.getBits(varobjs.gnomish_firelighter_colour.bits)
            val colour = ColouredLogRecipes.all.getOrNull(colourCode - 1) ?: return null
            if (charges !in 1..MAX_CHARGES) {
                return null
            }
            return GnomishFirelighterState(colour, charges)
        }
    }
}
