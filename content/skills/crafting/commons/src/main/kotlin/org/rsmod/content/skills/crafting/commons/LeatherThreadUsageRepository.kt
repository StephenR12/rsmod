package org.rsmod.content.skills.crafting.commons

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Shares an ordinary spool's five-action lifecycle across every leather-crafting plugin. */
@Singleton
public class LeatherThreadUsageRepository {
    private val uses: MutableMap<Player, Int> = WeakHashMap()

    public fun consumesThread(player: Player): Boolean = uses(player) == USES_PER_SPOOL - 1

    public fun completeUse(player: Player) {
        uses[player] = (uses(player) + 1) % USES_PER_SPOOL
    }

    public fun uses(player: Player): Int = uses.getOrDefault(player, 0)

    private companion object {
        const val USES_PER_SPOOL = 5
    }
}
