package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TanLeatherSpellScript
@Inject
constructor(
    private val tanLeather: TanLeatherSpell,
    private val setup: TanLeatherSetupRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_tan_leather) {
            when (op) {
                IfButtonOp.Op1 -> protectedAccess.launch(player) { tanLeather.cast(this) }
                IfButtonOp.Op2 -> protectedAccess.launch(player) { tanLeather.configure(this) }
                else -> Unit
            }
        }
        onPlayerQueue(tan_leather_queues.cast) { tanLeather.cast(this, allowQueue = false) }
        onPlayerLogout { setup.clear(player) }
    }
}
