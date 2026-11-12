package org.rsmod.content.skills.crafting.specialistleatherworking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SpecialistLeatherworkingScript
@Inject
constructor(
    private val leatherworking: SpecialistLeatherworking,
    private val recipes: SpecialistLeatherworkingRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (material in recipes.materials) {
            registerNeedle(material, objs.needle, costumeNeedle = false)
            registerNeedle(material, objs.costume_needle, costumeNeedle = true)
        }
        onPlayerQueueWithArgs<SpecialistLeatherworkingRequest>(
            crafting_queues.specialistLeatherworking
        ) {
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
