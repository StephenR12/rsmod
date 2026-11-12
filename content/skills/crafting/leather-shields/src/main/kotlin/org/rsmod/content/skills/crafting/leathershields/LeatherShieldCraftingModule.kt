package org.rsmod.content.skills.crafting.leathershields

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class LeatherShieldCraftingModule : PluginModule() {
    override fun bind() {
        bind(LeatherShieldRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(LeatherShieldCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
