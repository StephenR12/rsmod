package org.rsmod.content.skills.smithing.smelting

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

public typealias furnace_queues = FurnaceQueues

public object FurnaceQueues : QueueReferences() {
    val smelting = find("furnace_smelting_batch")
    val cannonballs = find("cannonball_smithing_batch")
    val goldJewellery = find("gold_jewellery_crafting_batch")
    val silverJewellery = find("silver_jewellery_crafting_batch")
    val moltenGlassmaking = find("molten_glassmaking_batch")
}

public data class FurnaceSmeltingRequest(
    val furnace: BoundLocInfo,
    val kind: FurnaceKind,
    val output: ObjType,
    val remaining: Int,
)

public enum class FurnaceKind {
    Standard,
    Edgeville,
    ShiloVillage,
    Lovakite,
}
