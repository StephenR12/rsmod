package org.rsmod.content.skills.crafting.amethystcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AmethystCuttingScript @Inject constructor(private val cutting: AmethystCutting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.chisel, objs.amethyst) { cutting.select(this) }
        onPlayerQueueWithArgs<AmethystCuttingRequest>(crafting_queues.amethystCutting) {
            cutting.process(this, it.args)
        }
    }
}
