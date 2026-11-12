package org.rsmod.content.skills.fletching.ballistaassembly

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BallistaAssemblyModule : PluginModule() {
    override fun bind() {
        bind(BallistaAssemblyRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(BallistaAssembly::class.java).`in`(Scopes.SINGLETON)
    }
}
