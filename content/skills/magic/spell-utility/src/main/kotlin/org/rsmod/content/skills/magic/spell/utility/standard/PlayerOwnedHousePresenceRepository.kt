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
    private val occupants = WeakHashMap<Player, Unit>()

    public fun isInside(player: Player): Boolean = player in occupants

    public fun enter(player: Player) {
        occupants[player] = Unit
    }

    public fun leave(player: Player) {
        occupants.remove(player)
    }
}
