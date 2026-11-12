package org.rsmod.content.skills.magic.spell.attacks.standard

import org.rsmod.api.script.onNpcQueueWithArgs
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.magic.spell.attacks.standard.config.stat_drain_queues
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StatDrainImpactScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<StatDrainImpact>(stat_drain_queues.impact) {
            player.applyStatDrain(it.args)
        }
        onNpcQueueWithArgs<StatDrainImpact>(stat_drain_queues.impact) {
            npc.applyStatDrain(it.args)
        }
    }
}
