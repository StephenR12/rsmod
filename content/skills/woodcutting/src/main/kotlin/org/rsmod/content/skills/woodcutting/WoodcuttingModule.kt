package org.rsmod.content.skills.woodcutting

import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.plugin.module.PluginModule

class WoodcuttingModule : PluginModule() {
    override fun bind() {
        addSetBinding<org.rsmod.api.stats.xpmod.XpMod>(org.rsmod.content.skills.woodcutting.configs.WoodcuttingOutfit::class.java)
        addSetBinding<CrystalSingingRecipe>(CrystalFellingAxeSinging::class.java)
        newSetBinding<CrystalSingingRecipe>()
        addSetBinding<CrystalSingingRecipe>(CrystalPickaxeSinging::class.java)
        addSetBinding<CrystalSingingRecipe>(CrystalHarpoonSinging::class.java)
        addSetBinding<InvisibleLevelMod>(WoodcuttingLevelBoosts::class.java)
    }
}
