package org.rsmod.content.skills.runecraft

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class RunecraftModule : PluginModule() {
    override fun bind() {
        bind(BindingNecklaceRepository::class.java).`in`(Scopes.SINGLETON)
        bind(BloodEssenceLifecycle::class.java).`in`(Scopes.SINGLETON)
        bind(EssencePouchStorage::class.java).`in`(Scopes.SINGLETON)
        bind(KourendRuneCrafting::class.java).`in`(Scopes.SINGLETON)
        bind(RunecraftOutputModifiers::class.java).`in`(Scopes.SINGLETON)
        bind(StandardRuneCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
