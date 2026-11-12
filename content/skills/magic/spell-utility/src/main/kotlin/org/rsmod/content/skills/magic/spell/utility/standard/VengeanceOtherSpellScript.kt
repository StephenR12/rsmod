package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.script.onApPlayerT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class VengeanceOtherSpellScript @Inject constructor(private val spell: VengeanceOtherSpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApPlayerT(components.magic_spellbook_vengeance_other) { spell.cast(this, it.target) }
    }
}
