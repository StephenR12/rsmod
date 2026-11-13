package org.rsmod.api.npc.events

import org.rsmod.events.KeyedEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder

public class NpcHitEvents {
    public data class Modify(
        public val npc: Npc,
        public val hit: HitBuilder,
        override val id: Long = npc.visType.id.toLong(),
    ) : KeyedEvent

    /** Applied to current health at impact, after any attacks queued in earlier cycles. */
    public data class ResolveDamage(
        public val npc: Npc,
        public val hit: Hit,
        public var damage: Int,
        override val id: Long = npc.visType.id.toLong(),
    ) : KeyedEvent

    /** Dispatched after damage resolution for ammunition and other secondary damage sources. */
    public data class WeaponImpact(public val npc: Npc, public val hit: Hit, public val source: org.rsmod.game.entity.Player, override val id: Long) : KeyedEvent

    public data class SecondaryImpact(
        public val npc: Npc,
        public val hit: Hit,
        override val id: Long,
    ) : KeyedEvent

    public data class Impact(
        public val npc: Npc,
        public val hit: Hit,
        override val id: Long = npc.visType.id.toLong(),
    ) : KeyedEvent
}

/** Lets an NPC substitute one attack after normal targeting, single-combat and delay validation. */
public class NpcMeleeAlternative(
    public val npc: Npc,
    public val player: org.rsmod.game.entity.Player,
    override val id: Long = npc.visType.id.toLong(),
) : KeyedEvent {
    public var attack: Attack? = null

    public data class Attack(
        public val animation: org.rsmod.game.type.seq.SeqType,
        public val type: org.rsmod.game.hit.HitType,
        public val damage: Int,
    )
}
