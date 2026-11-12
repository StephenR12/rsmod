package org.rsmod.content.skills.crafting.leatheraddons

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class LeatherAddonCraftingModule : PluginModule() {
    override fun bind() {
        bind(LeatherAddonRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(LeatherAddonCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
