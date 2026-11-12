package org.rsmod.content.skills.crafting.spinning

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SpinningScript
@Inject
constructor(private val spinning: Spinning, private val recipes: SpinningRecipeRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (wheel in spinning_locs.all) {
            registerWheel(wheel)
        }
        onPlayerQueueWithArgs<SpinningRequest>(crafting_queues.spinning) {
            spinning.process(this, it.args)
        }
    }

    private fun ScriptContext.registerWheel(wheel: LocType) {
        onOpLoc1(wheel) { spinning.select(this, it.loc) }
        for (input in recipes.recipes.flatMap(SpinningRecipe::inputs).toSet()) {
            onOpLocU(wheel, input) { spinning.select(this, it.loc, input) }
        }
    }
}
