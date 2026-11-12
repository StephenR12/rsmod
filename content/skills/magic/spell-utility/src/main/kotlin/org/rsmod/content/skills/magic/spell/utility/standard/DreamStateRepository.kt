package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Player

/** Tracks a player's one interruptible Lunar Dream channel. */
@Singleton
public class DreamStateRepository {
    private val active = WeakHashMap<Player, Int>()
    private var nextToken = 1

    public fun begin(player: Player): Int {
        val token = nextToken++
        active[player] = token
        return token
    }

    public fun isActive(player: Player): Boolean = active.containsKey(player)

    public fun isActive(player: Player, token: Int): Boolean = active[player] == token

    public fun finish(player: Player, token: Int): Boolean {
        if (!isActive(player, token)) {
            return false
        }
        active.remove(player)
        return true
    }

    public fun cancel(player: Player): Boolean = active.remove(player) != null
}
