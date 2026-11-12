package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.type.obj.ObjType

public typealias bolt_enchant_queues = BoltEnchantQueues

public object BoltEnchantQueues : QueueReferences() {
    val batch = find("bolt_enchant_batch")
}

internal data class BoltEnchantRequest(val spell: ObjType, val remainingSets: Int)
