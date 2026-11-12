package org.rsmod.content.skills.fletching.battlestafffletching

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BattlestaffFletchingModule : PluginModule() {
    override fun bind() {
        bind(BattlestaffFletching::class.java).`in`(Scopes.SINGLETON)
    }
}
