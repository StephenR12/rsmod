package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias tan_leather_queues = TanLeatherQueues

public object TanLeatherQueues : QueueReferences() {
    public val cast = find("tan_leather_cast")
}
