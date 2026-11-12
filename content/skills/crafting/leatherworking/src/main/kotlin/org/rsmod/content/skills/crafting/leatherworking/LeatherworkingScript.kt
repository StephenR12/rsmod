package org.rsmod.content.skills.crafting.leatherworking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LeatherworkingScript
@Inject
constructor(
    private val leatherworking: Leatherworking,
    private val recipes: LeatherworkingRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (material in recipes.materials) {
            registerNeedle(material, objs.needle, costumeNeedle = false)
            registerNeedle(material, objs.costume_needle, costumeNeedle = true)
        }
        onPlayerQueueWithArgs<LeatherworkingRequest>(crafting_queues.leatherworking) {
            leatherworking.process(this, it.args)
        }
    }

    private fun ScriptContext.registerNeedle(
        material: ObjType,
        needle: ObjType,
        costumeNeedle: Boolean,
    ) {
        onOpHeldU(needle, material) { leatherworking.select(this, material, costumeNeedle) }
    }
}
