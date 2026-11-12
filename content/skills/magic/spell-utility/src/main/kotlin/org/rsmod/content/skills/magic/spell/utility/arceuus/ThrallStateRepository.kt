package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Singleton
import java.util.IdentityHashMap
import java.util.WeakHashMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Singleton
public class ThrallStateRepository {
    private val byPlayer = WeakHashMap<Player, ActiveThrall>()
    private val byNpc = IdentityHashMap<Npc, ActiveThrall>()

    public fun active(player: Player): ActiveThrall? = byPlayer[player]

    public fun active(npc: Npc): ActiveThrall? = byNpc[npc]

    public fun register(owner: Player, npc: Npc, recipe: ThrallRecipe): ActiveThrall {
        check(owner !in byPlayer) { "Player already has an active thrall." }
        val active = ActiveThrall(owner, npc, recipe)
        byPlayer[owner] = active
        byNpc[npc] = active
        return active
    }

    public fun remove(npc: Npc): ActiveThrall? {
        val active = byNpc.remove(npc) ?: return null
        if (byPlayer[active.owner]?.npc === npc) {
            byPlayer.remove(active.owner)
        }
        return active
    }

    public fun remove(owner: Player): ActiveThrall? {
        val active = byPlayer.remove(owner) ?: return null
        byNpc.remove(active.npc)
        return active
    }

    public data class ActiveThrall(val owner: Player, val npc: Npc, val recipe: ThrallRecipe)
}
