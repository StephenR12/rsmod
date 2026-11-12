package org.rsmod.content.skills.magic.spell.utility.arceuus

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcUid

public typealias dark_lure_queues = DarkLureQueues

public object DarkLureQueues : QueueReferences() {
    val impact = find("dark_lure_impact")
}

internal data class DarkLureImpact(val npc: Npc, val uid: NpcUid)
