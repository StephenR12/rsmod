package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.skills.magic.spell.attacks.standard.config.teleblock_queues
import org.rsmod.content.skills.magic.spell.attacks.standard.config.teleblock_timers
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TeleBlockImpactScript
@Inject
constructor(private val states: TeleBlockStateRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<TeleBlockImpact>(teleblock_queues.impact) {
            val impact = it.args
            val duration =
                if (impact.protectedFromMagicAtCast) {
                    TeleBlockStateRepository.PROTECTED_DURATION_CYCLES
                } else {
                    TeleBlockStateRepository.FULL_DURATION_CYCLES
                }
            val state =
                states.apply(player, impact.source, duration) ?: return@onPlayerQueueWithArgs
            preventLogout("You can't log out until 10 seconds after the end of combat.", 16)
            val expiry =
                if (duration == TeleBlockStateRepository.FULL_DURATION_CYCLES) {
                    "5 minutes"
                } else {
                    "2 minutes and 30 seconds"
                }
            mes(
                "A Tele Block has been cast on you by ${state.source.displayName}. " +
                    "It will expire in $expiry."
            )
        }
        onPlayerSoftTimer(teleblock_timers.status) { states.tick(player) }
        onPlayerLogin { states.clear(player) }
        onPlayerLogout { states.clear(player) }
    }
}
