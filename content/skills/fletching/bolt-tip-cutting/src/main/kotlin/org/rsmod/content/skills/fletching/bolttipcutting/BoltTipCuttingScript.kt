package org.rsmod.content.skills.fletching.bolttipcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BoltTipCuttingScript
@Inject
constructor(
    private val cutting: BoltTipCutting,
    private val recipes: BoltTipCuttingRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(objs.chisel, recipe.input) { cutting.select(this, recipe.input) }
        }
        onPlayerQueueWithArgs<BoltTipCuttingRequest>(fletching_queues.boltTipCutting) {
            cutting.process(this, it.args)
        }
    }
}
