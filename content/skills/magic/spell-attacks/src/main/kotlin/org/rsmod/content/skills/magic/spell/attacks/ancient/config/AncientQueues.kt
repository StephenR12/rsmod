package org.rsmod.content.skills.magic.spell.attacks.ancient.config

import org.rsmod.api.type.refs.queue.QueueReferences

internal typealias ancient_queues = AncientQueues

internal object AncientQueues : QueueReferences() {
    val impact = find("magic_ancient_impact")
    val poison = find("magic_ancient_poison")
}
