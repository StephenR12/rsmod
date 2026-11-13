package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

/** Published after a witnessed attempt or successfully delivered stall loot, for merchant cooldowns. */
public data class PlayerStallTheftEvent(
    val player: Player,
    val stall: BoundLocInfo,
    val reward: ObjType?,
    val caught: Boolean,
) : UnboundEvent
