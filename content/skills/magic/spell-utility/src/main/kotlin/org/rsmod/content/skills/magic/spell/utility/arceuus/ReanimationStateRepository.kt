package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Singleton
import java.util.IdentityHashMap
import java.util.WeakHashMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
public class ReanimationStateRepository {
    private val byPlayer = WeakHashMap<Player, ActiveReanimation>()
    private val byNpc = IdentityHashMap<Npc, ActiveReanimation>()
    private val nextCastCycle = WeakHashMap<Player, Int>()

    public fun hasActive(player: Player): Boolean = byPlayer.containsKey(player)

    public fun active(player: Player): Npc? = byPlayer[player]?.npc

    public fun register(player: Player, npc: Npc, prayerXp: Double) {
        check(player !in byPlayer) { "Player already has an active reanimated creature." }
        val active = ActiveReanimation(player, npc, prayerXp)
        byPlayer[player] = active
        byNpc[npc] = active
    }

    public fun remove(npc: Npc): ActiveReanimation? {
        val active = byNpc.remove(npc) ?: return null
        byPlayer.remove(active.owner)
        return active
    }

    public fun remove(player: Player): ActiveReanimation? {
        val active = byPlayer.remove(player) ?: return null
        byNpc.remove(active.npc)
        return active
    }

    public fun clear(player: Player): ActiveReanimation? {
        nextCastCycle.remove(player)
        return remove(player)
    }

    public fun canCast(player: Player): Boolean =
        player.currentMapClock >= (nextCastCycle[player] ?: Int.MIN_VALUE)

    public fun setCastDelay(player: Player, cycles: Int) {
        nextCastCycle[player] = player.currentMapClock + cycles
    }

    public data class ActiveReanimation(val owner: Player, val npc: Npc, val prayerXp: Double)
}
