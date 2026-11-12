package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Signals that a player has successfully consumed an inventory object. */
public class PlayerConsumableEvent {
    public data class Consumed(
        public val player: Player,
        public val obj: ObjType,
        public val effectObj: ObjType = obj,
    ) : UnboundEvent
}
