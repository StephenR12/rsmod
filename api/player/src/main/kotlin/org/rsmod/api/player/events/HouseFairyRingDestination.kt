package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Resolves DIQ against the player's furnished house and reserves the instance for arrival. */
public class HouseFairyRingDestination(public val player: Player) : UnboundEvent {
    public var destination: CoordGrid? = null
}
