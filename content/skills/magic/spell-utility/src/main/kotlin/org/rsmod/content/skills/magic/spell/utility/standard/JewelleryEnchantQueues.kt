package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public typealias jewellery_enchant_queues = JewelleryEnchantQueues

public object JewelleryEnchantQueues : QueueReferences() {
    val manual = find("jewellery_enchant_manual")
    val auto = find("jewellery_enchant_auto")
}

internal data class JewelleryEnchantRequest(
    val spell: ObjType,
    val source: ObjType,
    val origin: CoordGrid,
    val expectedCycle: Int,
)
