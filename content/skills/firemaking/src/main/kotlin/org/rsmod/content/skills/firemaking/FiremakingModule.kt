package org.rsmod.content.skills.firemaking

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class FiremakingModule : PluginModule() {
    override fun bind() {
        bind(Firemaking::class.java).`in`(Scopes.SINGLETON)
        bind(FiremakingXp::class.java).`in`(Scopes.SINGLETON)
        bind(PyreLogPreparation::class.java).`in`(Scopes.SINGLETON)
        bind(FuneralPyre::class.java).`in`(Scopes.SINGLETON)
        bind(BarbarianPyreShip::class.java).`in`(Scopes.SINGLETON)
        bind(HandheldLightSources::class.java).`in`(Scopes.SINGLETON)
        bind(LampOilStill::class.java).`in`(Scopes.SINGLETON)
    }
}
