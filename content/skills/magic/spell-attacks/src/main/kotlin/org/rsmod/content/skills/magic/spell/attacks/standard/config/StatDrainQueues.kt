package org.rsmod.content.skills.magic.spell.attacks.standard.config

import org.rsmod.api.type.refs.queue.QueueReferences

internal typealias stat_drain_queues = StatDrainQueues

object StatDrainQueues : QueueReferences() {
    val impact = find("magic_stat_drain_impact")
}
