package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias string_jewellery_queues = StringJewelleryQueues

public object StringJewelleryQueues : QueueReferences() {
    public val batch = find("string_jewellery")
}
