package org.rsmod.content.skills.fletching.ammunition

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class AmmunitionFletchingModule : PluginModule() {
    override fun bind() {
        bind(AmmunitionFletchingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(AmmunitionFletching::class.java).`in`(Scopes.SINGLETON)
        bind(PoisonedAmmunitionRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(PoisonedAmmunition::class.java).`in`(Scopes.SINGLETON)
    }
}
