package org.rsmod.content.skills.crafting.jewellery

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.crafting.commons.CraftingQueues
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.game.type.obj.ObjType

public typealias handcraft_queues = CraftingQueues

internal fun ProtectedAccess.clearHandcraftQueues() {
    clearCraftingQueues()
}

public data class GemCuttingRequest(val uncut: ObjType)

public data class AmuletStringingRequest(val unstrung: ObjType)
