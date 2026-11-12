package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.events.PlayerActionEvent
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HomeTeleportScript
@Inject
constructor(
    private val teleport: HomeTeleport,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_home_teleport) {
            protectedAccess.launch(player) {
                teleport.cast(this, HomeTeleportDestination.Lumbridge)
            }
        }
        onIfOverlayButton(components.magic_spellbook_ancient_home_teleport) {
            protectedAccess.launch(player) {
                teleport.cast(this, HomeTeleportDestination.Edgeville)
            }
        }
        onIfOverlayButton(components.magic_spellbook_lunar_home_teleport) {
            protectedAccess.launch(player) {
                teleport.cast(this, HomeTeleportDestination.LunarIsle)
            }
        }
        onIfOverlayButton(components.magic_spellbook_arceuus_home_teleport) {
            protectedAccess.launch(player) { teleport.cast(this, HomeTeleportDestination.Arceuus) }
        }
        onPlayerQueueWithArgs<HomeTeleportChannel>(home_teleport_queues.channel) {
            teleport.advance(this, it.args)
        }
        onEvent<PlayerActionEvent.Interrupt> { teleport.interrupt(player) }
    }
}
