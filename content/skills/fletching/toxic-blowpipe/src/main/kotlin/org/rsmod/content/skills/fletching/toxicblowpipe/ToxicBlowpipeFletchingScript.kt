package org.rsmod.content.skills.fletching.toxicblowpipe

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ToxicBlowpipeFletchingScript
@Inject
constructor(
    private val fletching: ToxicBlowpipeFletching,
    private val lifecycle: ToxicBlowpipeLifecycle,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.chisel, objs.tanzanite_fang) { fletching.fletch(this) }
        onOpHeldU(objs.toxic_blowpipe_empty, objs.trailblazer_reloaded_blowpipe_ornament_kit) {
            lifecycle.applyOrnament(this, it.firstSlot, it.secondSlot)
        }

        val blowpipes =
            listOf(
                objs.toxic_blowpipe_empty,
                objs.toxic_blowpipe,
                objs.blazing_blowpipe_empty,
                objs.blazing_blowpipe,
            )
        for (blowpipe in blowpipes) {
            onOpHeldU(blowpipe, objs.zulrahs_scales) {
                lifecycle.loadScales(this, it.firstSlot, it.secondSlot)
            }
            for (dart in ToxicBlowpipeDart.entries) {
                onOpHeldU(blowpipe, dart.obj) {
                    lifecycle.loadDarts(this, it.firstSlot, it.secondSlot, dart)
                }
            }
        }

        onOpHeld3(objs.toxic_blowpipe) { lifecycle.check(this, it.inventory[it.slot]) }
        onOpHeld4(objs.toxic_blowpipe) { lifecycle.unloadDarts(this, it.slot) }
        onOpHeld5(objs.toxic_blowpipe) { lifecycle.uncharge(this, it.slot) }
        onOpWorn2(objs.toxic_blowpipe) { lifecycle.check(this, player.righthand) }
        onOpHeld4(objs.toxic_blowpipe_empty) { lifecycle.dismantle(this, it.slot) }

        onOpHeld3(objs.blazing_blowpipe) { lifecycle.check(this, it.inventory[it.slot]) }
        onOpHeld4(objs.blazing_blowpipe) { lifecycle.unloadDarts(this, it.slot) }
        onOpHeld5(objs.blazing_blowpipe) { lifecycle.uncharge(this, it.slot) }
        onOpWorn2(objs.blazing_blowpipe) { lifecycle.check(this, player.righthand) }
        onOpHeld4(objs.blazing_blowpipe_empty) { lifecycle.dismantle(this, it.slot) }
    }
}
