package org.rsmod.content.skills.crafting.leatheraddons

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LeatherAddonCraftingScript
@Inject
constructor(
    private val crafting: LeatherAddonCrafting,
    private val recipes: LeatherAddonRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(recipe.base, recipe.addon) { crafting.start(this, recipe) }
        }
        onPlayerQueueWithArgs<LeatherAddonCraftingRequest>(crafting_queues.leatherAddonCrafting) {
            crafting.process(this, it.args)
        }
    }
}
