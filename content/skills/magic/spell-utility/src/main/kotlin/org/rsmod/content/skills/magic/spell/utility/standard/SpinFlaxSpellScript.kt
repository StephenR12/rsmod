package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SpinFlaxSpellScript
@Inject
constructor(
    private val spinFlax: SpinFlaxSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_spin_flax) {
            protectedAccess.launch(player) { spinFlax.cast(this) }
        }
        onPlayerQueue(spin_flax_queues.cast) { spinFlax.cast(this, allowQueue = false) }
    }
}
