package org.rsmod.content.skills.fletching.crossbowassembly

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class CrossbowAssemblyModule : PluginModule() {
    override fun bind() {
        bind(CrossbowAssemblyRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(CrossbowAssembly::class.java).`in`(Scopes.SINGLETON)
    }
}
