package org.rsmod.content.skills.crafting.pottery

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class PotteryModule : PluginModule() {
    override fun bind() {
        bind(PotteryRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
    }
}
