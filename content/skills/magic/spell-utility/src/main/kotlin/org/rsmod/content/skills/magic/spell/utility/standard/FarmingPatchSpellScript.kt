package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.script.onApLocT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FarmingPatchSpellScript @Inject constructor(private val spells: FarmingPatchSpells) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApLocT(LunarUtilityComponents.curePlant) { spells.curePlant(this, it) }
        onApLocT(LunarUtilityComponents.fertileSoil) { spells.fertileSoil(this, it) }
        onApLocT(FarmingPatchComponents.resurrectCrops) { spells.resurrectCrops(this, it) }
    }
}
