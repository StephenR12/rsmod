package org.rsmod.content.other.special.weapons

import org.rsmod.api.weapons.WeaponMap
import org.rsmod.content.other.special.weapons.magic.OtherPoweredStaves
import org.rsmod.content.other.special.weapons.magic.KrakenTridents
import org.rsmod.content.other.special.weapons.magic.TumekensShadowWeapons
import org.rsmod.plugin.module.PluginModule

class MagicWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(OtherPoweredStaves::class.java)
        addSetBinding<WeaponMap>(KrakenTridents::class.java)
        addSetBinding<WeaponMap>(TumekensShadowWeapons::class.java)
    }
}
