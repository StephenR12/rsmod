package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.script.onApPlayerT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CureOtherSpellScript @Inject constructor(private val cureOther: CureOtherSpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApPlayerT(components.magic_spellbook_cure_other) { cureOther.cast(this, it.target) }
    }
}
