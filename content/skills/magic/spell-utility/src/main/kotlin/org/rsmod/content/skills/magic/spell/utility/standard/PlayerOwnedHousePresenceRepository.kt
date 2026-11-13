package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/**
 * Shared presence seam for player-owned-house restrictions.
 *
 * The Construction instance controller is expected to call [enter] and [leave] when that system is
 * implemented. Keeping this separate from house destinations avoids inferring instance presence
 * from a saved house entrance.
 */
@Singleton
public class PlayerOwnedHousePresenceRepository @Inject constructor() {
    private val occupants = WeakHashMap<Player, (Player) -> Boolean>()

    public fun isInside(player: Player): Boolean = occupants[player]?.invoke(player) == true

    public fun enter(player: Player, contains: (Player) -> Boolean = { true }) {
        occupants[player] = contains
    }

    public fun leave(player: Player) {
        occupants.remove(player)
    }
}
