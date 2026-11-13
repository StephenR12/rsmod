package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicImbueStateRepository
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MagicImbueSpellScript
@Inject
constructor(
    private val magicImbue: MagicImbueSpell,
    private val states: MagicImbueStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_magic_imbue) {
            protectedAccess.launch(player) { magicImbue.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_imbue_active) { states.tick(player) }
        onPlayerLogin { states.clear(player) }
        onPlayerLogout { states.clear(player) }
    }
}
