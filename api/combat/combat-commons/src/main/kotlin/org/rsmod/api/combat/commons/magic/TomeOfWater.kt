package org.rsmod.api.combat.commons.magic

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.isType
import org.rsmod.utils.bits.getBits

/** Shared cache-233 Tome of water charge rules used by rune and combat systems. */
public object TomeOfWater {
    public const val CHARGES_PER_PAGE: Int = 20
    public const val MAX_CHARGES: Int = 20_000

    public fun charges(obj: InvObj?): Int =
        if (obj.isType(objs.tome_of_water)) {
            obj.vars.getBits(varobjs.tome_of_water_charges.bits)
        } else {
            0
        }

    public fun isCharged(obj: InvObj?): Boolean = charges(obj) > 0

    public fun consumesCharge(spell: MagicSpell, book: Spellbook): Boolean =
        book == Spellbook.Standard &&
            spell.type == MagicSpellType.Combat &&
            spell.objReqs.any { it.wornSlot == null && it.obj.isType(objs.water_rune) }
}
