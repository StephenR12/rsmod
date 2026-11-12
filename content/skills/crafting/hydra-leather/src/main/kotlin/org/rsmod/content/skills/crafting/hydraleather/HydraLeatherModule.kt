package org.rsmod.content.skills.crafting.hydraleather

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class HydraLeatherModule : PluginModule() {
    override fun bind() {
        bind(HydraLeatherCrafting::class.java).`in`(Scopes.SINGLETON)
    }
}
