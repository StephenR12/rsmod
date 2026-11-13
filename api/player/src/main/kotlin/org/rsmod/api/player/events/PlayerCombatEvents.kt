package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** Events emitted by validated combat actions, before their queued hits reach the target. */
public class PlayerCombatEvents {
    /** Published after Chainhit has consumed the thrown weapon and special energy. */
    public data class Chainhit(public val player: Player, public val target: Npc) : UnboundEvent

    /**
     * Published when [player] makes or receives a combat attack.
     *
     * A multi-hit attack may publish this event more than once in the same cycle. Consumers that
     * model combat-entry behavior should therefore make their handling idempotent per engagement.
     */
    public data class Engage(public val player: Player) : UnboundEvent
}
