package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.events.PlayerActionEvent
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DreamSpellScript
@Inject
constructor(private val dream: DreamSpell, private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_dream) {
            protectedAccess.launch(player) { dream.cast(this) }
        }
        onPlayerQueueWithArgs<DreamChannel>(dream_queues.channel) { dream.advance(this, it.args) }
        onEvent<PlayerActionEvent.Interrupt> { dream.interrupt(player) }
        onEvent<SessionStateEvent.Login> { dream.clear(player) }
        onEvent<SessionStateEvent.Logout> { dream.clear(player) }
    }
}
