package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** A successful capture, including barehanded butterfly and moth catches. */
public data class HunterCapture(val player: Player, val species: String) : UnboundEvent
