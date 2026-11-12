package org.rsmod.content.skills.crafting.birdhouses

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BirdhouseCraftingScript @Inject constructor(private val crafting: BirdhouseCrafting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in BirdhouseRecipes.entries) {
            onOpHeldU(birdhouse_objs.clockwork, recipe.logs) { crafting.start(this, recipe) }
        }
        onPlayerQueueWithArgs<BirdhouseCraftingRequest>(crafting_queues.birdhouseCrafting) {
            crafting.process(this, it.args)
        }
    }
}
