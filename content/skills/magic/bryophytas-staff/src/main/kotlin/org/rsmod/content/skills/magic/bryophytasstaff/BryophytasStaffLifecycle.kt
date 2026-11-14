package org.rsmod.content.skills.magic.bryophytasstaff

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

public class BryophytasStaffLifecycle
@Inject
constructor(private val chargeManager: ObjChargeManager) {
    public suspend fun configureCharges(access: ProtectedAccess) {
        with(access) {
            val requested = countDialog("How many charges should the staff hold on withdrawal?")
            player.bryophytasStaffAutochargeQuantity = requested.coerceIn(0, MAX_CHARGES)
        }
    }

    public fun autocharge(access: ProtectedAccess, event: BankedObjEvents.Autocharge) {
        val staff = access.inv[event.slot] ?: return
        if (
            staff != event.obj ||
                !staff.isAnyType(objs.bryophytas_staff, objs.bryophytas_staff_uncharged)
        )
            return
        val current = charges(staff)
        val needed =
            access.player.bryophytasStaffAutochargeQuantity.coerceIn(0, MAX_CHARGES) - current
        if (needed <= 0) return
        val result =
            chargeManager.addCharges(access.inv, event.slot, needed, varobjs.charges_16383, MAX_CHARGES) {
                event.takeMaterial(objs.nature_rune, it)
            }
        if ((result as? ObjChargeManager.Charge.Success)?.added != needed)
            access.mes("There aren't enough nature runes in your bank to charge the staff fully.")
    }

    public fun charge(access: ProtectedAccess, staffSlot: Int, runeSlot: Int) {
        val staff = access.inv[staffSlot]
        val runes = access.inv[runeSlot]
        if (
            !staff.isAnyType(objs.bryophytas_staff_uncharged, objs.bryophytas_staff) ||
                !runes.isType(objs.nature_rune)
        ) {
            return
        }

        val current = charges(staff)
        val add = min(MAX_CHARGES - current, runes.count)
        if (add <= 0) {
            access.mes(FULL_MESSAGE)
            return
        }

        val result =
            chargeManager.addCharges(access.inv, staffSlot, add, varobjs.charges_16383, MAX_CHARGES) {
                count ->
                val removed = access.invDel(access.inv, objs.nature_rune, count, slot = runeSlot)
                if (removed.success) count else 0
            }
        if (result is ObjChargeManager.Charge.Success) {
            access.mes("You add ${result.added} nature runes to Bryophyta's staff.")
        }
    }

    public fun check(access: ProtectedAccess, staff: InvObj?) {
        access.mes("Bryophyta's staff has ${charges(staff)} charges remaining.")
    }

    public fun uncharge(access: ProtectedAccess, staffSlot: Int) {
        val staff = access.inv[staffSlot]
        if (!staff.isType(objs.bryophytas_staff)) {
            return
        }
        val removed =
            chargeManager.removeAllCharges(
                inventory = access.inv,
                slot = staffSlot,
                varobj = varobjs.charges_16383,
            )
        if (removed > 0) {
            access.mes("You uncharge Bryophyta's staff. The nature runes are lost.")
        }
    }

    public fun charges(staff: InvObj?): Int = chargeManager.getCharges(staff, varobjs.charges_16383)

    public companion object {
        public const val MAX_CHARGES: Int = 1_000
        public const val FULL_MESSAGE: String = "Bryophyta's staff is already fully charged."
    }
}

private var org.rsmod.game.entity.Player.bryophytasStaffAutochargeQuantity by
    intVarBit(org.rsmod.api.config.refs.varbits.charges_bryophytas_staff_quantity)
