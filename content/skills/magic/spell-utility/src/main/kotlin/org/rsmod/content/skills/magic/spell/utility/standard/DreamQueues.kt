package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias dream_queues = DreamQueues

public object DreamQueues : QueueReferences() {
    public val channel = find("lunar_dream_channel")
}

internal data class DreamChannel(val token: Int, val elapsedTicks: Int, val observedHitpoints: Int)
