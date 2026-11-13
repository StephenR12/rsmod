package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

/** An ordinary ground pickup has removed the object and committed its delivery. */
public data class GroundItemTaken(public val player: Player, public val obj: Obj) : UnboundEvent
