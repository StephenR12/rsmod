package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** Signals that the standard player death reset has completed. */
public class PlayerDeathEvent {
    public data class Completed(public val player: Player) : UnboundEvent
}
