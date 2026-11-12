package org.rsmod.content.skills.fletching.ballistaassembly

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BallistaAssemblyScript
@Inject
constructor(
    private val assembly: BallistaAssembly,
    private val recipes: BallistaAssemblyRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(recipe.primary, recipe.secondary) { assembly.assemble(this, recipe) }
        }
    }
}
