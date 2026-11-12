package org.rsmod.content.skills.crafting.customfur

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class CustomFurClothingModule : PluginModule() {
    override fun bind() {
        bind(CustomFurRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(CustomFurClothing::class.java).`in`(Scopes.SINGLETON)
    }
}
