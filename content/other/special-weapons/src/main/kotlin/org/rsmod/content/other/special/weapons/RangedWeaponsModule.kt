package org.rsmod.content.other.special.weapons

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.ranged.BasicBowSinging
import org.rsmod.content.other.special.weapons.ranged.Chinchompas
import org.rsmod.content.other.special.weapons.ranged.CorruptedBowSinging
import org.rsmod.content.other.special.weapons.ranged.CrystalBows
import org.rsmod.content.other.special.weapons.ranged.DarkBowWeapons
import org.rsmod.content.other.special.weapons.ranged.EnhancedBowSinging
import org.rsmod.content.other.special.weapons.ranged.ToxicBlowpipes
import org.rsmod.content.skills.woodcutting.CrystalSingingRecipe
import org.rsmod.plugin.module.PluginModule

class RangedWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(ToxicBlowpipes::class.java)
        addSetBinding<SpecialAttackMap>(ToxicBlowpipes::class.java)
        addSetBinding<WeaponMap>(Chinchompas::class.java)
        addSetBinding<WeaponMap>(CrystalBows::class.java)
        addSetBinding<CrystalSingingRecipe>(BasicBowSinging::class.java)
        addSetBinding<CrystalSingingRecipe>(EnhancedBowSinging::class.java)
        addSetBinding<CrystalSingingRecipe>(CorruptedBowSinging::class.java)
        addSetBinding<WeaponMap>(DarkBowWeapons::class.java)
    }
}
