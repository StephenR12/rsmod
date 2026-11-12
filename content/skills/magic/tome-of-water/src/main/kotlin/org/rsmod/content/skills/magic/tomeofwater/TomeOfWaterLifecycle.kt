package org.rsmod.content.skills.magic.tomeofwater

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.magic.TomeOfWater
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

public class TomeOfWaterLifecycle @Inject constructor(private val chargeManager: ObjChargeManager) {
    public fun charge(access: ProtectedAccess, tomeSlot: Int, pageSlot: Int) {
        val tome = access.inv[tomeSlot]
        val pages = access.inv[pageSlot]
        if (
            !tome.isAnyType(objs.tome_of_water_uncharged, objs.tome_of_water) ||
                !pages.isType(objs.soaked_page)
        ) {
            return
        }

        val current = charges(tome)
        val capacity = TomeOfWater.MAX_CHARGES - current
        if (capacity <= 0) {
            access.mes(FULL_MESSAGE)
            return
        }

        val pagesNeeded =
            (capacity + TomeOfWater.CHARGES_PER_PAGE - 1) / TomeOfWater.CHARGES_PER_PAGE
        val pagesUsed = min(pages.count, pagesNeeded)
        val chargesAdded = min(capacity, pagesUsed * TomeOfWater.CHARGES_PER_PAGE)
        if (pagesUsed <= 0 || chargesAdded <= 0) {
            return
        }

        val deletion =
            access.invDel(
                inv = access.inv,
                type = objs.soaked_page,
                count = pagesUsed,
                slot = pageSlot,
            )
        if (deletion.failure) {
            return
        }

        check(access.inv[tomeSlot].isAnyType(objs.tome_of_water_uncharged, objs.tome_of_water))
        val result =
            chargeManager.addCharges(
                inventory = access.inv,
                slot = tomeSlot,
                add = chargesAdded,
                varobj = varobjs.tome_of_water_charges,
                max = TomeOfWater.MAX_CHARGES,
            )
        check(result is ObjChargeManager.Charge.Success)
        access.mes(
            "You add $pagesUsed soaked ${if (pagesUsed == 1) "page" else "pages"} to the tome."
        )
    }

    public fun check(access: ProtectedAccess, tome: InvObj?) {
        access.mes("Your tome of water has ${charges(tome)} charges remaining.")
    }

    public fun charges(tome: InvObj?): Int =
        chargeManager.getCharges(tome, varobjs.tome_of_water_charges)

    public companion object {
        public const val FULL_MESSAGE: String = "Your tome of water is already fully charged."
    }
}
