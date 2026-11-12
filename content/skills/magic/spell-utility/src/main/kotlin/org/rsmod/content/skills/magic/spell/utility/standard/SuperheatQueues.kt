package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias superheat_queues = SuperheatQueues

public object SuperheatQueues : QueueReferences() {
    val cast = find("superheat_item_cast")
}

internal data class SuperheatCastRequest(val targetSlot: Int, val targetId: Int)
