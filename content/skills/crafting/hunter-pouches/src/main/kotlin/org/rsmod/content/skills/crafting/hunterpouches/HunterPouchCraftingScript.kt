package org.rsmod.content.skills.crafting.hunterpouches

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HunterPouchCraftingScript
@Inject
constructor(
    private val crafting: HunterPouchCrafting,
    private val recipes: HunterPouchRecipeRegistry,
    private val storage: HunterPouchStorage,
    private val storageRegistry: HunterPouchStorageRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            registerNeedle(recipe.fur, objs.needle, costumeNeedle = false)
            registerNeedle(recipe.fur, objs.costume_needle, costumeNeedle = true)
        }
        onPlayerQueueWithArgs<HunterPouchCraftingRequest>(crafting_queues.hunterPouchCrafting) {
            crafting.process(this, it.args)
        }
        registerStorageOps()
    }

    private fun ScriptContext.registerNeedle(
        fur: ObjType,
        needle: ObjType,
        costumeNeedle: Boolean,
    ) {
        onOpHeldU(needle, fur) { crafting.select(this, fur, costumeNeedle) }
    }

    private fun ScriptContext.registerStorageOps() {
        for (descriptor in storageRegistry.descriptors) {
            for (variant in descriptor.variants) {
                onOpHeld1(variant) { storage.fill(this, descriptor) }
                onOpHeld2(variant) { storage.emptyToInventory(this, descriptor) }
                onOpHeld3(variant) { storage.check(this, descriptor) }
                onOpHeld4(variant) { storage.toggle(this, descriptor, variant, it.slot) }
                onOpBankside(variant) { storage.emptyToBank(player, descriptor) }
            }
        }
        onDestroyHeld {
            val descriptor = storageRegistry.find(type.id) ?: return@onDestroyHeld
            storage.clear(player, descriptor.family)
        }
    }
}
