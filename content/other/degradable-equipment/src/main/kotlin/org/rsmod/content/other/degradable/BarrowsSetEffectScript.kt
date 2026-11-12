package org.rsmod.content.other.degradable

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.front
import org.rsmod.api.player.hat
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onPlayerHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Applies Dharok's Amulet of the damned recoil after the incoming hit has resolved. */
public class BarrowsSetEffectScript
@Inject
constructor(
    private val random: GameRandom,
    private val players: PlayerList,
    private val npcs: NpcList,
    private val playerHitProcessor: InstantPlayerHitProcessor,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHit { applyDharokRecoil(this) }
    }

    private fun applyDharokRecoil(event: PlayerHitEvents.Impact) {
        val target = event.player
        if (
            event.damage <= 0 ||
                event.hit.type == HitType.Typeless ||
                target.front?.id != objs.amulet_of_the_damned_full.id ||
                !EquipmentChecks.isDharokSet(
                    target.hat,
                    target.torso,
                    target.legs,
                    target.righthand,
                ) ||
                !random.randomBoolean(4)
        ) {
            return
        }

        val reflected = reflectedDamage(event.damage)
        val sourcePlayer = event.hit.playerSourceOrNull(players)
        val sourceNpc = event.hit.npcSourceOrNull(npcs)
        when {
            sourcePlayer != null ->
                sourcePlayer.takeInstantHit(
                    type = HitType.Typeless,
                    damage = reflected,
                    processor = playerHitProcessor,
                )
            sourceNpc != null ->
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
            return maxOf(1, damage * 15 / 100)
        }
    }
}

private fun org.rsmod.game.hit.Hit.playerSourceOrNull(players: PlayerList): Player? =
    if (isFromPlayer) resolvePlayerSource(players) else null

private fun org.rsmod.game.hit.Hit.npcSourceOrNull(npcs: NpcList): Npc? =
    if (isFromNpc) resolveNpcSource(npcs) else null
