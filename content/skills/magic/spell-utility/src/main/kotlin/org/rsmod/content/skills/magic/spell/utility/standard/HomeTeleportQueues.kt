package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.map.CoordGrid

public typealias home_teleport_queues = HomeTeleportQueues

public object HomeTeleportQueues : QueueReferences() {
    val channel = find("home_teleport_channel")
}

internal data class HomeTeleportChannel(
    val token: Int,
    val origin: CoordGrid,
    val destination: HomeTeleportDestination,
    val elapsedTicks: Int,
)
