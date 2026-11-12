package org.rsmod.content.skills.smithing.smelting

import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

public const val SHILO_FEE: Int = 20

public fun Player.hasFreeShiloFurnace(): Boolean = vars[varbits.karamja_diary_elite_complete] != 0

public fun ProtectedAccess.clearFurnaceQueues() {
    clearWeakQueue(furnace_queues.smelting)
    clearWeakQueue(furnace_queues.cannonballs)
    clearWeakQueue(furnace_queues.goldJewellery)
    clearWeakQueue(furnace_queues.silverJewellery)
    clearWeakQueue(furnace_queues.moltenGlassmaking)
}
