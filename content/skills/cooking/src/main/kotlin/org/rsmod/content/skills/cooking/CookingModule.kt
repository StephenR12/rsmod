package org.rsmod.content.skills.cooking

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class CookingModule : PluginModule() {
    override fun bind() {
        bind(StandardCooking::class.java).`in`(Scopes.SINGLETON)
        bind(KarambwanCooking::class.java).`in`(Scopes.SINGLETON)
        bind(SinewCooking::class.java).`in`(Scopes.SINGLETON)
        bind(IronSpitCooking::class.java).`in`(Scopes.SINGLETON)
        bind(ChompyFireCooking::class.java).`in`(Scopes.SINGLETON)
        bind(OgreSpitRoast::class.java).`in`(Scopes.SINGLETON)
        bind(FoodPreparation::class.java).`in`(Scopes.SINGLETON)
        bind(DairyChurning::class.java).`in`(Scopes.SINGLETON)
        bind(WineMaking::class.java).`in`(Scopes.SINGLETON)
        bind(FoodConsumptionRepository::class.java).`in`(Scopes.SINGLETON)
        bind(TeaDrinking::class.java).`in`(Scopes.SINGLETON)
    }
}
