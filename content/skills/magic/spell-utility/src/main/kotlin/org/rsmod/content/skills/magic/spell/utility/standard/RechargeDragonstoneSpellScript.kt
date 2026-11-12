package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class RechargeDragonstoneSpellScript
@Inject
constructor(
    private val rechargeDragonstone: RechargeDragonstoneSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_recharge_dragonstone) {
            protectedAccess.launch(player) { rechargeDragonstone.cast(this) }
        }
    }
}
