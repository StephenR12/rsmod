package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Entry coordinates prepared by the player-owned-house instance system.
 *
 * The house plugin registers these only after its dynamic zone is ready. This keeps the spell from
 * fabricating an instance or teleporting into an unloaded map.
 */
@Singleton
public class PlayerOwnedHouseRepository @Inject constructor() {
    private val entrances = WeakHashMap<Player, PlayerOwnedHouseEntrances>()

    private var prepare: ((Player, Boolean) -> CoordGrid?)? = null

    public fun onPrepareHouse(provider: (Player, Boolean) -> CoordGrid?) {
        prepare = provider
    }

    public fun register(player: Player, regular: CoordGrid, buildingMode: CoordGrid? = null) {
        entrances[player] = PlayerOwnedHouseEntrances(regular, buildingMode)
    }

    public fun destination(player: Player, buildingMode: Boolean): CoordGrid? {
        val entry = entrances[player] ?: return prepare?.invoke(player, buildingMode)
        return if (buildingMode) entry.buildingMode else entry.regular
    }

    public fun clear(player: Player) {
        entrances.remove(player)
    }
}

public data class PlayerOwnedHouseEntrances(
    public val regular: CoordGrid,
    public val buildingMode: CoordGrid?,
)
