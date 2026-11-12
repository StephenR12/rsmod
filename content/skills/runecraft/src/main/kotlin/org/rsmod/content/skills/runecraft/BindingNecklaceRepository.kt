package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class BindingNecklaceRepository @Inject constructor() {
    private var Player.storedCharges by intVarp(RunecraftVarps.bindingNecklace)

    public fun remaining(player: Player): Int =
        player.storedCharges.takeIf { it in 1..MAX_CHARGES } ?: MAX_CHARGES

    /** Returns true when this use exhausts the necklace and resets the shared player counter. */
    public fun consume(player: Player): Boolean {
        val next = remaining(player) - 1
        if (next <= 0) {
            player.storedCharges = MAX_CHARGES
            return true
        }
        player.storedCharges = next
        return false
    }

    public fun reset(player: Player) {
        player.storedCharges = MAX_CHARGES
    }

    public companion object {
        public const val MAX_CHARGES: Int = 16
    }
}
