package org.rsmod.content.skills.crafting.battlestaves

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BattlestaffCraftingModule : PluginModule() {
    override fun bind() {
        bind(BattlestaffRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(BattlestaffCrafting::class.java).`in`(Scopes.SINGLETON)
        bind(BryophytasStaffCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
