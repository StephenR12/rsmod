package org.rsmod.content.skills.fletching.ammunition

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AmmunitionFletchingScript
@Inject
constructor(
    private val fletching: AmmunitionFletching,
    private val recipes: AmmunitionFletchingRecipeRegistry,
    private val poisoning: PoisonedAmmunition,
    private val poisoningRecipes: PoisonedAmmunitionRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(recipe.primary, recipe.secondary) { fletching.select(this, recipe) }
        }
        for (recipe in poisoningRecipes.recipes) {
            onOpHeldU(recipe.ammunition, recipe.poison) { poisoning.poison(this, recipe) }
        }
        onPlayerQueueWithArgs<AmmunitionFletchingRequest>(fletching_queues.ammunition) {
            fletching.process(this, it.args)
        }
    }
}
