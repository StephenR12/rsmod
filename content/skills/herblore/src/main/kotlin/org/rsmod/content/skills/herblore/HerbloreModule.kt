package org.rsmod.content.skills.herblore

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class HerbloreModule : PluginModule() {
    override fun bind() {
        bind(HerbCleaning::class.java).`in`(Scopes.SINGLETON)
        bind(IngredientCrushing::class.java).`in`(Scopes.SINGLETON)
        bind(CrystalShardCrushing::class.java).`in`(Scopes.SINGLETON)
        bind(LavaScaleCrushing::class.java).`in`(Scopes.SINGLETON)
        bind(GorakClawCrushing::class.java).`in`(Scopes.SINGLETON)
        bind(UnfinishedPotionMixing::class.java).`in`(Scopes.SINGLETON)
        bind(FinishedPotionMixing::class.java).`in`(Scopes.SINGLETON)
        bind(PotionProductionModifiers::class.java).`in`(Scopes.SINGLETON)
        bind(ChemistryAmuletRepository::class.java).`in`(Scopes.SINGLETON)
        bind(HerbloreEquipment::class.java).`in`(Scopes.SINGLETON)
        bind(PotionConsumptionRepository::class.java).`in`(Scopes.SINGLETON)
        bind(StandardPotionDrinking::class.java).`in`(Scopes.SINGLETON)
        bind(GoadingPotionDrinking::class.java).`in`(Scopes.SINGLETON)
        bind(SurgePotionCooldownRepository::class.java).`in`(Scopes.SINGLETON)
        bind(SurgePotionDrinking::class.java).`in`(Scopes.SINGLETON)
        bind(ForgottenBrewUpgrading::class.java).`in`(Scopes.SINGLETON)
        bind(BarbarianMixing::class.java).`in`(Scopes.SINGLETON)
        bind(BarbarianMixDrinking::class.java).`in`(Scopes.SINGLETON)
        bind(SwampTarMixing::class.java).`in`(Scopes.SINGLETON)
        bind(CoconutMilkMaking::class.java).`in`(Scopes.SINGLETON)
        bind(DoseScaledPotionMixing::class.java).`in`(Scopes.SINGLETON)
    }
}
