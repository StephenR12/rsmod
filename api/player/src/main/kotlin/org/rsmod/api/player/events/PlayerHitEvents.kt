package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

public class PlayerHitEvents {
    /**
     * Published after a queued hit has been applied to a player.
     *
     * [damage] is the amount actually removed from the player's Hitpoints. It can be lower than
     * [Hit.damage] when the hit exceeds the player's remaining health.
     */
    public data class Impact(
        public val player: Player,
        public val hit: Hit,
        public val damage: Int,
    ) : UnboundEvent
}
