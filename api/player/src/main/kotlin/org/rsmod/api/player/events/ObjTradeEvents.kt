package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.UnpackedObjType

public class ObjTradeEvents {
    /** Allows content to refine cache tradeability for stateful objects. */
    public class Query(
        public val player: Player,
        public val obj: InvObj,
        public val type: UnpackedObjType,
    ) : UnboundEvent {
        public var denied: Boolean = false
            private set

        public fun deny() {
            denied = true
        }
    }
}
