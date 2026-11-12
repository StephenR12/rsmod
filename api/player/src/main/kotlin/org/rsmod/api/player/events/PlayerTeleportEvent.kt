package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** Signals that a standard player teleport has been accepted by protected access. */
public class PlayerTeleportEvent {
    public data class Start(public val player: Player, public val destination: CoordGrid) :
        UnboundEvent
}
