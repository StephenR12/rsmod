package org.rsmod.content.skills.crafting.leatherworking

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class LeatherworkingModule : PluginModule() {
    override fun bind() {
        bind(LeatherworkingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(Leatherworking::class.java).`in`(Scopes.SINGLETON)
    }
}
