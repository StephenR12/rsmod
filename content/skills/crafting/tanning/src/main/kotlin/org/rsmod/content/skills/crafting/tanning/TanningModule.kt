package org.rsmod.content.skills.crafting.tanning

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class TanningModule : PluginModule() {
    override fun bind() {
        bind(TanningRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(Tanning::class.java).`in`(Scopes.SINGLETON)
    }
}
