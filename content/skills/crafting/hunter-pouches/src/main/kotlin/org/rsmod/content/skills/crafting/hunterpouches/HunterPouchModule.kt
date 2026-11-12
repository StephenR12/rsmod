package org.rsmod.content.skills.crafting.hunterpouches

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class HunterPouchModule : PluginModule() {
    override fun bind() {
        bind(HunterPouchRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(HunterPouchCrafting::class.java).`in`(Scopes.SINGLETON)
        bind(HunterPouchStorageRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(HunterPouchStorage::class.java).`in`(Scopes.SINGLETON)
    }
}
