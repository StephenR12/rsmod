package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlankMakeSpellScript
@Inject
constructor(
    private val plankMake: PlankMakeSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButtonT(components.magic_spellbook_plank_make, components.inv_items) {
            castPlankMake()
        }
        onPlayerQueue(plank_make_queues.batch) { plankMake.process(this) }
        onPlayerQueueWithArgs<PlankMakeCastRequest>(plank_make_queues.cast) {
            val request = it.args
            val target = player.inv[request.targetSlot]
            if (target?.id == request.targetId) {
                plankMake.cast(this, request.targetSlot, allowQueue = false)
            }
        }
    }

    private fun IfOverlayButtonT.castPlankMake() {
        val target = targetObj ?: return
        protectedAccess.launch(player) {
            val current = player.inv[targetSlot]
            if (!current.isType(target)) {
                return@launch
            }
            plankMake.cast(this, targetSlot)
        }
    }
}
