package org.rsmod.content.skills.fletching.bolttipcutting

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class BoltTipCuttingModule : PluginModule() {
    override fun bind() {
        bind(BoltTipCuttingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(BoltTipCutting::class.java).`in`(Scopes.SINGLETON)
    }
}
