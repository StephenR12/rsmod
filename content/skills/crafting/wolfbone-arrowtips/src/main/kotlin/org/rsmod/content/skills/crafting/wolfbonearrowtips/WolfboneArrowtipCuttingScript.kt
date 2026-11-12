package org.rsmod.content.skills.crafting.wolfbonearrowtips

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WolfboneArrowtipCuttingScript
@Inject
constructor(private val cutting: WolfboneArrowtipCutting) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.chisel, objs.wolf_bones) { cutting.start(this) }
        onPlayerQueue(fletching_queues.wolfboneArrowtips) { cutting.process(this) }
    }
}
