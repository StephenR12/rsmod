package org.rsmod.content.skills.crafting.pottery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PotteryScript
@Inject
constructor(
    private val shaping: PotteryShaping,
    private val firing: PotteryFiring,
    private val recipes: PotteryRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (wheel in pottery_locs.wheels) {
            registerWheel(wheel)
        }
        for (oven in pottery_locs.ovens) {
            registerOven(oven)
        }
        onPlayerQueueWithArgs<PotteryShapingRequest>(crafting_queues.potteryShaping) {
            shaping.process(this, it.args)
        }
        onPlayerQueueWithArgs<PotteryFiringRequest>(crafting_queues.potteryFiring) {
            firing.process(this, it.args)
        }
    }

    private fun ScriptContext.registerWheel(wheel: LocType) {
        onOpLoc1(wheel) { shaping.select(this, it.loc) }
        onOpLocU(wheel, objs.soft_clay) { shaping.select(this, it.loc) }
    }

    private fun ScriptContext.registerOven(oven: LocType) {
        onOpLoc1(oven) { firing.select(this, it.loc) }
        for (recipe in recipes.recipes) {
            onOpLocU(oven, recipe.unfired) { firing.select(this, it.loc, recipe.unfired) }
        }
    }
}
