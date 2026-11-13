package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Published only after an actual handmade finished gnome dish or cocktail is delivered. */
public data class PlayerGnomeCookingSuccess(val player: Player, val output: ObjType) : UnboundEvent
