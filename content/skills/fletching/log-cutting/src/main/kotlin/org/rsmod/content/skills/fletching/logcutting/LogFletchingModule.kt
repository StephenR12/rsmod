package org.rsmod.content.skills.fletching.logcutting

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class LogFletchingModule : PluginModule() {
    override fun bind() {
        bind(LogFletchingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(LogFletching::class.java).`in`(Scopes.SINGLETON)
    }
}
