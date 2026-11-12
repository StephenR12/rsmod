package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AmuletStringingScript
@Inject
constructor(
    private val stringing: AmuletStringing,
    private val recipes: AmuletStringingRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(objs.ball_of_wool, recipe.unstrung) { stringing.start(this, recipe.unstrung) }
        }
        onPlayerQueueWithArgs<AmuletStringingRequest>(handcraft_queues.amuletStringing) {
            stringing.process(this, it.args)
        }
    }
}
