package org.rsmod.content.skills.crafting.spinning

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class SpinningModule : PluginModule() {
    override fun bind() {
        bind(SpinningRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
    }
}
