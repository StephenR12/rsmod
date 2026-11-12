package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BoltEnchantSpellScript
@Inject
constructor(
    private val enchant: BoltEnchantSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_xbows_enchant) {
            protectedAccess.launch(player) { enchant.select(this) }
        }
        onPlayerQueueWithArgs<BoltEnchantRequest>(bolt_enchant_queues.batch) {
            enchant.castQueued(this, it.args)
        }
    }
}
