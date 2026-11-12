package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LunarUtilitySpellScript
@Inject
constructor(
    private val spells: LunarUtilitySpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(LunarUtilityComponents.superglassMake) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { spells.superglassMake(this) }
            }
        }
        onIfOverlayButton(LunarUtilityComponents.geomancy) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { spells.geomancy(this) }
            }
        }
        onIfOverlayButton(LunarUtilityComponents.npcContact) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { spells.npcContact(this) }
            }
        }
    }
}
