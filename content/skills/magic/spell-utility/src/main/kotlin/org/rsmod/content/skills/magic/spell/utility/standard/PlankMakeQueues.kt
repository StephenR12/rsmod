package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias plank_make_queues = PlankMakeQueues

public object PlankMakeQueues : QueueReferences() {
    public val batch = find("plank_make_batch")
    public val cast = find("plank_make_cast")
}

public data class PlankMakeCastRequest(val targetSlot: Int, val targetId: Int)
