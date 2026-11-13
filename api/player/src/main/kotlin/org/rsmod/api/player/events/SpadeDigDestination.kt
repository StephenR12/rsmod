package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Allows a dig site to resolve its entrance after the spade animation completes. */
public class SpadeDigDestination(public val player: Player, public val tile: CoordGrid) :
    UnboundEvent {
    public var destination: CoordGrid? = null
}
