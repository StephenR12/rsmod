package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** Signals that a new player action has replaced any interruptible action already in progress. */
public class PlayerActionEvent {
    public class Interrupt(public val player: Player) : UnboundEvent
}
