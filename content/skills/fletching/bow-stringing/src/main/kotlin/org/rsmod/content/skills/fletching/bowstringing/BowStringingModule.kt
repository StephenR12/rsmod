package org.rsmod.content.skills.fletching.bowstringing

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BowStringingModule : PluginModule() {
    override fun bind() {
        bind(BowStringingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(BowStringSpool::class.java).`in`(Scopes.SINGLETON)
        bind(BowStringing::class.java).`in`(Scopes.SINGLETON)
    }
}
