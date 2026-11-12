package org.rsmod.content.skills.crafting.specialistleatherworking

import com.google.inject.Scopes
import org.rsmod.plugin.module.PluginModule

public class SpecialistLeatherworkingModule : PluginModule() {
    override fun bind() {
        bind(SpecialistLeatherworkingRecipeRegistry::class.java).`in`(Scopes.SINGLETON)
        bind(SpecialistLeatherworking::class.java).`in`(Scopes.SINGLETON)
    }
}
