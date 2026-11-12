package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.script.onNpcQueueWithArgs
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.magic.spell.attacks.arceuus.config.arceuus_queues
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStatus
import org.rsmod.game.entity.PathingEntity
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GraspImpactScript
@Inject
constructor(
    private val bindings: BindingStateRepository,
    private val wardOfArceuus: WardOfArceuusStateRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<GraspImpact>(arceuus_queues.grasp_impact) { apply(player, it.args) }
        onNpcQueueWithArgs<GraspImpact>(arceuus_queues.grasp_impact) { apply(npc, it.args) }
    }

    internal fun apply(target: PathingEntity, impact: GraspImpact) {
        if (impact.bind && bindings.status(target) == BindingStatus.Available) {
            val duration =
                if (target is org.rsmod.game.entity.Player && wardOfArceuus.isActive(target)) {
                    WARD_BIND_DURATION_CYCLES
                } else {
                    impact.bindDuration
                }
            bindings.bind(
                target,
                impact.source,
                duration = duration,
                immunityDuration = GRASP_IMMUNITY_CYCLES,
            )
        }
    }

    private companion object {
        const val GRASP_IMMUNITY_CYCLES = 2
        const val WARD_BIND_DURATION_CYCLES = 1
    }
}
