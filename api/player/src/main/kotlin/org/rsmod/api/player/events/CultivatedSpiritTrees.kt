package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** The spirit network queries player-grown destinations before showing its menu or reserving a house. */
public class CultivatedSpiritTrees(public val player: Player, public val reserveHouse: Boolean = false) : UnboundEvent {
    public val plots: MutableSet<CoordGrid> = mutableSetOf()
    public var hasHouseTree: Boolean = false
    public var houseDestination: CoordGrid? = null
}
