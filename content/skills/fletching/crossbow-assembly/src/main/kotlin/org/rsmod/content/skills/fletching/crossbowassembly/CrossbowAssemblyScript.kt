package org.rsmod.content.skills.fletching.crossbowassembly

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CrossbowAssemblyScript
@Inject
constructor(
    private val assembly: CrossbowAssembly,
    private val recipes: CrossbowAssemblyRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(recipe.stock, recipe.limbs) {
                assembly.select(this, recipe, CrossbowAssemblyStage.Limbs)
            }
            onOpHeldU(objs.crossbow_string, recipe.unstrung) {
                assembly.select(this, recipe, CrossbowAssemblyStage.String)
            }
        }
        onPlayerQueueWithArgs<CrossbowAssemblyRequest>(fletching_queues.crossbowAssembly) {
            assembly.process(this, it.args)
        }
    }
}
