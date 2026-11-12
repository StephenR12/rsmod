package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.script.onApPlayerT
import org.rsmod.api.script.onIfModalButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class StatSpySpellScript @Inject constructor(private val statSpy: StatSpySpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApPlayerT(components.magic_spellbook_stat_spy) { statSpy.cast(this, it.target) }
        onIfModalButton(components.dream_player_stats_close) {
            ifCloseSub(interfaces.dream_player_stats)
        }
    }
}
