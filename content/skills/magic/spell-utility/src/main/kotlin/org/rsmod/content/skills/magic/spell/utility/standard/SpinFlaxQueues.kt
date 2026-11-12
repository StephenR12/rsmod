package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias spin_flax_queues = SpinFlaxQueues

public object SpinFlaxQueues : QueueReferences() {
    public val cast = find("spin_flax_cast")
}
