package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.script.onNpcQueueWithArgs
import org.rsmod.api.script.onNpcWalkTrigger
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerWalkTrigger
import org.rsmod.content.skills.magic.spell.attacks.standard.config.binding_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BindingImpactScript @Inject constructor(private val states: BindingStateRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<BindingImpact>(binding_queues.impact) {
            val impact = it.args
            val state = states.bind(player, impact.source, impact.duration)
            if (impact.duration > WARNING_LEAD_CYCLES) {
                strongQueue(
                    binding_queues.warning,
                    impact.duration - WARNING_LEAD_CYCLES,
                    BindingWarning(state.heldUntil),
                )
            }
        }
        onNpcQueueWithArgs<BindingImpact>(binding_queues.impact) {
            val impact = it.args
            states.bind(npc, impact.source, impact.duration)
        }
        onPlayerQueueWithArgs<BindingWarning>(binding_queues.warning) {
            if (states.isCurrent(player, it.args)) {
                mes("<col=ef1020>The holding effect is beginning to fade.</col>")
            }
        }
        onPlayerWalkTrigger(walktriggers.frozen) { states.blockMovement(player) }
        onPlayerWalkTrigger(walktriggers.pvp_frozen) { states.blockMovement(player) }
        onNpcWalkTrigger(walktriggers.frozen) { states.blockMovement(npc) }
        onNpcWalkTrigger(walktriggers.pvp_frozen) { states.blockMovement(npc) }
    }

    private companion object {
        // The 2023 game update warns shortly before release. Cache 233 has no named synth for the
        // accompanying cue, so the verified message is retained while sound timing is tracked.
        const val WARNING_LEAD_CYCLES = 4
    }
}
