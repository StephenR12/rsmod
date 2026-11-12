package org.rsmod.content.skills.crafting.jewellery

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class JewelleryModule : PluginModule() {
    override fun bind() {
        bind(GoldJewelleryRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(SilverCraftingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(GemCuttingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(AmuletStringingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
    }
}
