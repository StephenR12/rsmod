package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType

/** Applies an armed Vengeance effect when a sourced, damaging hit reaches a player. */
public class VengeanceReflection
@Inject
constructor(
    private val states: VengeanceStateRepository,
    private val players: PlayerList,
    private val npcs: NpcList,
    private val playerHitProcessor: InstantPlayerHitProcessor,
) {
    public fun impact(event: PlayerHitEvents.Impact) {
        val target = event.player
        if (!states.isActive(target) || event.damage <= 0) {
            return
        }

        val sourcePlayer = event.hit.resolvePlayerSourceOrNull(players)
        val sourceNpc = event.hit.resolveNpcSourceOrNull(npcs)
        if (sourcePlayer == null && sourceNpc == null) {
            return
        }
        if (sourceNpc != null && VengeanceReflectionPolicy.isImmune(sourceNpc)) {
            return
        }

        states.clearActive(target)
        target.say("Taste vengeance!")

        val reflected = reflectedDamage(event.damage)
        if (sourcePlayer != null) {
            sourcePlayer.takeInstantHit(
                type = HitType.Typeless,
                damage = reflected,
                processor = playerHitProcessor,
            )
        } else if (sourceNpc != null) {
            sourceNpc.queueHit(
                source = target,
                delay = 1,
                type = HitType.Typeless,
                damage = reflected,
                modifier = NO_MODIFICATION,
            )
        }
    }

    public companion object {
        private val NO_MODIFICATION = NpcHitModifier {}

        public fun reflectedDamage(damage: Int): Int {
            require(damage > 0) { "Damage must be positive: $damage" }
            return maxOf(1, damage * 3 / 4)
        }
    }
}

private fun org.rsmod.game.hit.Hit.resolvePlayerSourceOrNull(players: PlayerList): Player? =
    if (isFromPlayer) resolvePlayerSource(players) else null

private fun org.rsmod.game.hit.Hit.resolveNpcSourceOrNull(npcs: NpcList): Npc? =
    if (isFromNpc) resolveNpcSource(npcs) else null

/** Cache-233 Wiki recoil-immunity roster, matched against the resolved visible NPC type. */
internal object VengeanceReflectionPolicy {
    private val immuneNames =
        setOf(
            "Callisto",
            "Champion of Scabaras",
            "Fragment of Seren",
            "Galvek",
            "Hespori",
            "Judge of Yama",
            "Menaphite Akh",
            "Nex",
            "The Nightmare",
            "Phosani's Nightmare",
            "Ranis Drakan",
            "Skotizo",
            "Slug Prince",
            "Undead tree",
            "Vanstrom Klause",
            "Venenatis",
            "Vet'ion",
            "Vorkath",
            "Wighted Leech",
            "Xamphur",
        )

    fun isImmune(npc: Npc): Boolean = npc.visType.name in immuneNames
}
