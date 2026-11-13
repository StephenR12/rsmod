package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StaffObjs : ObjReferences() {
    val enchantment = find("slayer_enchantment")
}

internal object SlayerStaffConfig : ObjEditor() {
    init {
        edit(objs.slayers_staff) { param[params.charged_variant] = objs.slayers_staff_e }
        edit(objs.slayers_staff_e) { param[params.uncharged_variant] = objs.slayers_staff }
    }
}

public class SlayerStaff @Inject constructor(private val charges: ObjChargeManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.slayers_staff, StaffObjs.enchantment) {
            if (!player.members || !inv[it.firstSlot].isType(objs.slayers_staff)) return@onOpHeldU
            if (invDel(inv, StaffObjs.enchantment, slot = it.secondSlot).failure) return@onOpHeldU
            charges.addCharges(inv, it.firstSlot, capacity, varobjs.charges_16383, capacity)
            mes("You enchant the Slayer staff with 2,500 charges.")
        }
        onOpHeld3(objs.slayers_staff_e) { checkCharges(it.inventory[it.slot]) }
        onOpWorn2(objs.slayers_staff_e) { checkCharges(player.righthand) }
        onOpHeld5(objs.slayers_staff_e) {
            val slot = it.slot
            if (
                choice2(
                    "Revert the staff and lose its remaining charges.",
                    true,
                    "Keep the charges.",
                    false,
                ) && inv[slot].isType(objs.slayers_staff_e)
            ) {
                inv[slot] = InvObj(objs.slayers_staff)
                mes("The staff loses its enchantment.")
            }
        }
    }

    private fun ProtectedAccess.checkCharges(staff: InvObj?) {
        val remaining =
            charges.getCharges(staff, varobjs.charges_16383).takeIf { it > 0 } ?: capacity
        mes("Your Slayer's staff has $remaining charges remaining.")
    }

    public fun consume(access: ProtectedAccess): Unit =
        with(access) {
            val staff = player.righthand
            if (!staff.isType(objs.slayers_staff_e)) return@with
            // Enchanted staves saved before charges were tracked still represent a full
            // enchantment.
            if (charges.getCharges(staff, varobjs.charges_16383) == 0) {
                player.righthand = staff.copy(vars = capacity)
            }
            val result =
                charges.reduceWornCharges(
                    player,
                    Wearpos.RightHand,
                    varobjs.charges_16383,
                    decrement = 1,
                )
            if (result is ObjChargeManager.Uncharge.Success && result.chargesLeft == 0) {
                rebuildAppearance()
                mes("Your Slayer's staff has run out of charges.")
            }
        }

    private companion object {
        const val capacity = 2500
    }
}
