package org.rsmod.content.skills.magic.spell.attacks.ancient

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.content.skills.magic.spell.attacks.ancient.config.ancient_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType

@Singleton
public class AncientNpcPoisonRepository
@Inject
constructor(private val hitModifier: NpcHitModifier) {
    private val severity = WeakHashMap<Npc, Int>()

    public fun inflict(npc: Npc, initialSeverity: Int): Boolean {
        if (npc.visType.param(params.poison_immunity) != constants.npc_immunity_none) return false
        if ((severity[npc] ?: 0) > 0) return false
        severity[npc] = initialSeverity
        npc.queue(ancient_queues.poison, TOXIN_INTERVAL_CYCLES, NpcPoisonTick)
        return true
    }

    public fun tick(npc: Npc) {
        val current = severity[npc] ?: return
        val damage = (current + STATE_PER_DAMAGE - 1) / STATE_PER_DAMAGE
        npc.queueHit(
            delay = 1,
            type = HitType.Typeless,
            damage = damage,
            modifier = hitModifier,
            hitmark = hitmark_groups.poison_damage,
        )
        val next = current - 1
        if (next <= 0 || npc.hitpoints <= 0) {
            severity.remove(npc)
        } else {
            severity[npc] = next
            npc.queue(ancient_queues.poison, TOXIN_INTERVAL_CYCLES, NpcPoisonTick)
        }
    }

    public fun severity(npc: Npc): Int = severity[npc] ?: 0

    private companion object {
        const val TOXIN_INTERVAL_CYCLES = 30
        const val STATE_PER_DAMAGE = 5
    }
}

internal data object NpcPoisonTick
