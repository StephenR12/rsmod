package org.rsmod.content.skills.crafting.leathershields

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LeatherShieldCraftingScript
@Inject
constructor(
    private val crafting: LeatherShieldCrafting,
    private val recipes: LeatherShieldRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(recipe.leather, recipe.base) { crafting.start(this, recipe) }
        }
        onPlayerQueueWithArgs<LeatherShieldCraftingRequest>(crafting_queues.leatherShieldCrafting) {
            crafting.process(this, it.args)
        }
    }
}
