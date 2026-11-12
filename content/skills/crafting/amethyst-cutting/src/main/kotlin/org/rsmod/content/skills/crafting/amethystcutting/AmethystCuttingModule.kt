package org.rsmod.content.skills.crafting.amethystcutting

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class AmethystCuttingModule : PluginModule() {
    override fun bind() {
        bind(AmethystCuttingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(AmethystCutting::class.java).`in`(Scopes.SINGLETON)
    }
}
