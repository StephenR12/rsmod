package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HunterKitSpellScript
@Inject
constructor(
    private val hunterKit: HunterKitSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_hunter_kit) {
            protectedAccess.launch(player) { hunterKit.cast(this) }
        }
        onOpHeld1(objs.hunter_kit) { hunterKit.open(this, it.slot) }
    }
}
