package org.rsmod.api.npc

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
public class EntTrunkOwnership @Inject constructor() {
    private val killers = mutableMapOf<Npc, Long>()

    public fun assign(trunk: Npc, uuid: Long) { killers[trunk] = uuid }

    public fun canChop(player: Player, trunk: Npc): Boolean =
        player.uuid?.let { it == killers[trunk] } == true

    public fun forget(trunk: Npc) { killers.remove(trunk) }
}
