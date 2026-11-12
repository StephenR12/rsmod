package org.rsmod.content.skills.magic.tomeofwater

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.lefthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TomeOfWaterScript @Inject constructor(private val lifecycle: TomeOfWaterLifecycle) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (tome in listOf(objs.tome_of_water_uncharged, objs.tome_of_water)) {
            onOpHeldU(tome, objs.soaked_page) {
                lifecycle.charge(this, tomeSlot = it.firstSlot, pageSlot = it.secondSlot)
            }
        }
        onOpHeld3(objs.tome_of_water) { lifecycle.check(this, it.inventory[it.slot]) }
        onOpWorn1(objs.tome_of_water) { lifecycle.check(this, player.lefthand) }
    }
}
