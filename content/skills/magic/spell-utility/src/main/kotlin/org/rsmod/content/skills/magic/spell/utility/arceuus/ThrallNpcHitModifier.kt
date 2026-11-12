package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitBuilder

/** Publishes encounter hit hooks while preserving the official thrall bypass of flat armour. */
public class ThrallNpcHitModifier @Inject constructor(private val eventBus: EventBus) :
    NpcHitModifier {
    override fun HitBuilder.modify(target: Npc) {
        eventBus.publish(NpcHitEvents.Modify(target, this))
    }
}
