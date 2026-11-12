package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public typealias orb_charging_queues = OrbChargingQueues

public object OrbChargingQueues : QueueReferences() {
    val manual = find("orb_charge_manual")
    val auto = find("orb_charge_auto")
}

internal data class OrbChargingRequest(
    val spell: ObjType,
    val obelisk: LocType,
    val target: CoordGrid,
    val origin: CoordGrid,
    val expectedCycle: Int,
)
