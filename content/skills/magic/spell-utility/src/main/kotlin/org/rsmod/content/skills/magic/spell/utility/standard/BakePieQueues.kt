package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias bake_pie_queues = BakePieQueues

public object BakePieQueues : QueueReferences() {
    public val batch = find("bake_pie")
}
