package org.rsmod.api.combat.commons.magic

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.utils.bits.getBits

/** Shared cache-233 Tome of water charge rules used by rune and combat systems. */
public object TomeOfWater {
    public const val CHARGES_PER_PAGE: Int = 20
    public const val SEARING_PAGES: Int = 1 shl 15
    public const val MAX_CHARGES: Int = 20_000

    public fun charges(obj: InvObj?): Int =
        if (obj.isAnyType(objs.tome_of_water, objs.tome_of_fire, objs.tome_of_earth)) {
            obj.vars.getBits(varobjs.tome_of_water_charges.bits)
        } else {
            0
        }

    public fun isCharged(obj: InvObj?): Boolean = charges(obj) > 0

    public fun chargedElement(item: InvObj?): ObjType? {
        if (!isCharged(item)) return null
        return when {
            item.isType(objs.tome_of_fire) -> objs.fire_rune
            item.isType(objs.tome_of_earth) -> objs.earth_rune
            else -> objs.water_rune
        }
    }

    public fun consumesCharge(spell: MagicSpell, book: Spellbook): Boolean =
        book == Spellbook.Standard &&
            spell.type == MagicSpellType.Combat &&
            (MagicSpellChecks.isWaterSpell(spell.obj) ||
                MagicSpellChecks.isCurseSpell(spell.obj) ||
                MagicSpellChecks.isBindSpell(spell.obj))
}
