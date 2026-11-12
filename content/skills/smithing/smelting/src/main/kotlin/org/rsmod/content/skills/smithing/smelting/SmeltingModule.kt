package org.rsmod.content.skills.smithing.smelting

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class SmeltingModule : PluginModule() {
    override fun bind() {
        bind(SmeltingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(CannonballRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(RingOfForgingRepository::class.java).`in`(Scopes.SINGLETON)
    }
}
