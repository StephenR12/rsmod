package org.rsmod.content.skills.crafting.battlestaves

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BattlestaffCraftingScript
@Inject
constructor(
    private val crafting: BattlestaffCrafting,
    private val bryophytasStaff: BryophytasStaffCrafting,
    private val recipes: BattlestaffRecipeRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in recipes.recipes) {
            onOpHeldU(objs.battlestaff, recipe.orb) { crafting.select(this, recipe.orb) }
        }
        onOpHeldU(objs.battlestaff, objs.bryophytas_essence) {
            bryophytasStaff.craft(this, battlestaffSlot = it.firstSlot, essenceSlot = it.secondSlot)
        }
        onPlayerQueueWithArgs<BattlestaffCraftingRequest>(crafting_queues.battlestaffCrafting) {
            crafting.process(this, it.args)
        }
    }
}
