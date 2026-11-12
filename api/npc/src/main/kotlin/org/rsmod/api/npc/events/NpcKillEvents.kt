package org.rsmod.api.npc.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public class NpcKillEvents {
    /** Published once when a player-sourced hit first reduces an NPC to zero Hitpoints. */
    public data class Credited(public val player: Player, public val npc: Npc) : UnboundEvent
}
