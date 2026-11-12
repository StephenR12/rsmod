package org.rsmod.content.skills.crafting.glassmaking

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class GlassmakingModule : PluginModule() {
    override fun bind() {
        bind(GlassblowingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
    }
}
