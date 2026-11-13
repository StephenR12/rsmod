package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class PlayerDeathEvent {
    /** A safe minigame may return its participant to the arena or its lobby. */
    public data class ResolveRespawn(public val player: Player, public var safeDestination: CoordGrid? = null) : UnboundEvent

    /** Fired at the death tile after safe minigames have resolved their exemptions. */
    public data class Unsafe(public val player: Player) : UnboundEvent

    public data class Completed(public val player: Player) : UnboundEvent
}
