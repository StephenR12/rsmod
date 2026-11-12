package org.rsmod.content.skills.magic.spell.attacks

import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.content.skills.magic.spell.attacks.ancient.AncientSpells
import org.rsmod.content.skills.magic.spell.attacks.arceuus.DemonbaneSpells
import org.rsmod.content.skills.magic.spell.attacks.arceuus.GraspSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.CrumbleUndeadSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.ElementalSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.GodSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.IbanBlastSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.MagicDartSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.StatDrainSpells
import org.rsmod.content.skills.magic.spell.attacks.standard.TeleBlockSpells
import org.rsmod.plugin.module.PluginModule

class SpellAttacksModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpellAttackMap>(ElementalSpells::class.java)
        addSetBinding<SpellAttackMap>(IbanBlastSpells::class.java)
        addSetBinding<SpellAttackMap>(GodSpells::class.java)
        addSetBinding<SpellAttackMap>(MagicDartSpells::class.java)
        addSetBinding<SpellAttackMap>(BindingSpells::class.java)
        addSetBinding<SpellAttackMap>(CrumbleUndeadSpells::class.java)
        addSetBinding<SpellAttackMap>(StatDrainSpells::class.java)
        addSetBinding<SpellAttackMap>(TeleBlockSpells::class.java)
        addSetBinding<SpellAttackMap>(AncientSpells::class.java)
        addSetBinding<SpellAttackMap>(GraspSpells::class.java)
        addSetBinding<SpellAttackMap>(DemonbaneSpells::class.java)
    }
}
