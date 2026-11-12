package org.rsmod.content.skills.magic.bryophytasstaff

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BryophytasStaffScript
@Inject
constructor(private val lifecycle: BryophytasStaffLifecycle) : PluginScript() {
    override fun ScriptContext.startup() {
        for (staff in listOf(objs.bryophytas_staff_uncharged, objs.bryophytas_staff)) {
            onOpHeldU(staff, objs.nature_rune) {
                lifecycle.charge(this, staffSlot = it.firstSlot, runeSlot = it.secondSlot)
            }
            onOpHeld3(staff) { lifecycle.check(this, it.inventory[it.slot]) }
            onOpWorn2(staff) { lifecycle.check(this, player.righthand) }
        }
        onOpHeld4(objs.bryophytas_staff) { lifecycle.uncharge(this, it.slot) }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(objs.bryophytas_staff.id) {
            lifecycle.configureCharges(this)
        }
    }
}
