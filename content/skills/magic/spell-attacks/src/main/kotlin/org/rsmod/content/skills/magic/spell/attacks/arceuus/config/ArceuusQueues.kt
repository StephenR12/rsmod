package org.rsmod.content.skills.magic.spell.attacks.arceuus.config

import org.rsmod.api.type.refs.queue.QueueReferences

internal typealias arceuus_queues = ArceuusQueues

internal object ArceuusQueues : QueueReferences() {
    val grasp_impact = find("magic_arceuus_grasp_impact")
}
