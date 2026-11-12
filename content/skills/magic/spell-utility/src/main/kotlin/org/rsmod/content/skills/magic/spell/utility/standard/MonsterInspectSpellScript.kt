package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onApNpcT
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MonsterInspectSpellScript @Inject constructor(private val spell: MonsterInspectSpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApNpcT(components.magic_spellbook_monster_inspect) {
            spell.cast(this, it.npc, it.npcType, objs.spell_monster_inspect)
        }
        onApNpcT(LunarUtilityComponents.monsterExamine) {
            spell.cast(this, it.npc, it.npcType, objs.spell_monster_examine)
        }
    }
}

internal object LunarUtilityComponents : ComponentReferences() {
    val curePlant = find("magic_spellbook:cure_plant")
    val monsterExamine = find("magic_spellbook:monster_examine")
    val npcContact = find("magic_spellbook:npc_contact")
    val superglassMake = find("magic_spellbook:superglass")
    val fertileSoil = find("magic_spellbook:fertile_soil")
    val geomancy = find("magic_spellbook:geomancy")
}
