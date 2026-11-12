package org.rsmod.content.skills.crafting.lunarclothing

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class LunarClothingModule : PluginModule() {
    override fun bind() {
        bind(LunarClothingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(LunarClothingCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
