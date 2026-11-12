package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BoneConversionSpellScript
@Inject
constructor(
    private val spells: BoneConversionSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_bones_to_bananas) {
            protectedAccess.launch(player) { spells.cast(this, objs.spell_bones_to_bananas) }
        }
        onIfOverlayButton(components.magic_spellbook_bones_to_peaches) {
            protectedAccess.launch(player) { spells.cast(this, objs.spell_bones_to_peaches) }
        }
    }
}
