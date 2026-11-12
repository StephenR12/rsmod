package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.obj.Obj

public typealias telekinetic_grab_queues = TelekineticGrabQueues

public object TelekineticGrabQueues : QueueReferences() {
    val cast = find("telekinetic_grab_cast")
    val impact = find("telekinetic_grab_impact")
}

internal data class TelekineticGrabImpact(val obj: Obj, val castLevel: Int)
