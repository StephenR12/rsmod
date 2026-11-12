package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GemCuttingScript
@Inject
constructor(private val cutting: GemCutting, private val recipes: GemCuttingRecipeRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(objs.chisel, recipe.uncut) { cutting.start(this, recipe.uncut) }
        }
        onPlayerQueueWithArgs<GemCuttingRequest>(handcraft_queues.gemCutting) {
            cutting.process(this, it.args)
        }
    }
}
