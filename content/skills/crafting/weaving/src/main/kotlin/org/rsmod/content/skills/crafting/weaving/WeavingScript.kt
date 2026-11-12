package org.rsmod.content.skills.crafting.weaving

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WeavingScript
@Inject
constructor(private val weaving: Weaving, private val recipes: WeavingRecipeRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (loom in weaving_locs.all) {
            registerLoom(loom)
        }
        onPlayerQueueWithArgs<WeavingRequest>(crafting_queues.weaving) {
            weaving.process(this, it.args)
        }
    }

    private fun ScriptContext.registerLoom(loom: LocType) {
        onOpLoc1(loom) { weaving.select(this, it.loc) }
        for (input in recipes.recipes.map(WeavingRecipe::input).toSet()) {
            onOpLocU(loom, input) { weaving.select(this, it.loc, input) }
        }
    }
}
