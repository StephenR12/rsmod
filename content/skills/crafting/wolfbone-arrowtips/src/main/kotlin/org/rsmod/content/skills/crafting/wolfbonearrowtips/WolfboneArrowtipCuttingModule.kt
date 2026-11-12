package org.rsmod.content.skills.crafting.wolfbonearrowtips

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class WolfboneArrowtipCuttingModule : PluginModule() {
    override fun bind() {
        bind(WolfboneArrowtipCutting::class.java).`in`(Scopes.SINGLETON)
    }
}
