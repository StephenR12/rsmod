package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LunarGroupTeleportSpellScript
@Inject
constructor(
    private val teleports: LunarGroupTeleportSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (definition in LunarGroupTeleportDefinition.entries) {
            onIfOverlayButton(definition.component) {
                if (op == IfButtonOp.Op1) {
                    protectedAccess.launch(player) { teleports.cast(this, definition) }
                }
            }
        }
    }
}
