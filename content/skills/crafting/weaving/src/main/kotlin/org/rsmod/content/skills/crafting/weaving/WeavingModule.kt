package org.rsmod.content.skills.crafting.weaving

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class WeavingModule : PluginModule() {
    override fun bind() {
        bind(WeavingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(Weaving::class.java).`in`(Scopes.SINGLETON)
    }
}
