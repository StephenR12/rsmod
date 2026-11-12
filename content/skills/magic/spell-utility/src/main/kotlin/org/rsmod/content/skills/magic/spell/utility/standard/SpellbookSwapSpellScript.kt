package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicSpellEvents
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SpellbookSwapSpellScript
@Inject
constructor(
    private val spellbookSwap: SpellbookSwapSpell,
    private val states: SpellbookSwapStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_spellbook_swap) {
            protectedAccess.launch(player) { spellbookSwap.cast(this) }
        }
        onEvent<MagicSpellEvents.Cast> { states.consume(player) }
        onPlayerSoftTimer(timers.magic_spellbook_swap_timeout) { states.tick(player) }
        onPlayerLogin { states.syncOnLogin(player) }
    }
}
