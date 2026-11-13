package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

/** House layout scripts can block a telescope when its view of the sky is obstructed. */
public data class TelescopeObservation(
    public val player: Player,
    public val telescope: BoundLocInfo,
    public var blocked: Boolean = false,
) : UnboundEvent
