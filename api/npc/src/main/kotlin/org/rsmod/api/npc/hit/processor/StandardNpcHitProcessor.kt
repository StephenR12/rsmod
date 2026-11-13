package org.rsmod.api.npc.hit.processor

import jakarta.inject.Inject
import org.rsmod.api.config.refs.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.headbar.InternalNpcHeadbars
import org.rsmod.api.player.output.soundSynth
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hit
import org.rsmod.game.type.headbar.HeadbarType

public class StandardNpcHitProcessor
@Inject
constructor(private val playerList: PlayerList, private val eventBus: EventBus) : NpcHitProcessor {
    override fun StandardNpcAccess.process(hit: Hit) {
        // TODO(combat): Show ironman_blocked hitmark if source is an ironman and target has been
        // damaged by other sources.

        val resolve = NpcHitEvents.ResolveDamage(npc, hit, hit.damage)
        eventBus.publish(resolve)
        val damage = resolve.damage.coerceIn(0, npc.hitpoints)
        val changedDamage = damage.takeIf { it != hit.damage }

        if (changedDamage == 0) {
            val zeroDamageHitmark = hitmark_groups.zero_damage
            val modifiedHitmark =
                hit.hitmark.copy(
                    self = zeroDamageHitmark.lit.id,
                    source = zeroDamageHitmark.lit.id,
                    public = if (hit.hitmark.isPrivate) null else zeroDamageHitmark.tint?.id,
                    damage = changedDamage,
                )
            val modifiedHit = hit.copy(hitmark = modifiedHitmark)
            takeHit(modifiedHit)
            return
        }

        if (changedDamage != null) {
            val modifiedHitmark = hit.hitmark.copy(damage = changedDamage)
            val modifiedHit = hit.copy(hitmark = modifiedHitmark)
            takeHit(modifiedHit)
            return
        }

        takeHit(hit)
    }

    private fun StandardNpcAccess.takeHit(hit: Hit) {
        check(hit.damage <= npc.hitpoints) {
            "Expected hit damage to be less than or equal to available hitpoints: " +
                "health=${npc.hitpoints}, hit=$hit"
        }
        // TODO(combat): Process recoils, retribution(?), etc.
        npc.hitpoints -= hit.damage

        playDefendSound(hit)

        val playerSource = if (hit.isFromPlayer) hit.resolvePlayerSource(playerList) else null
        if (playerSource != null && hit.damage > 0) npc.heroPoints(playerSource, hit.damage)

        val queueDeath = npc.hitpoints == 0 && queues.death !in npc.queueList
        val creditedPlayer = playerSource.takeIf { queueDeath }
        if (queueDeath) {
            queueDeath()
        }

        npc.showHitmark(hit.hitmark)

        val visHeadbar = npc.visHeadbar(params.headbar)
        val headbar = hit.createHeadbar(npc.hitpoints, npc.baseHitpointsLvl, visHeadbar)
        npc.showHeadbar(headbar)

        npc.publishHitEvent(hit)
        hit.secondaryObj?.let { eventBus.publish(NpcHitEvents.SecondaryImpact(npc, hit, it.toLong())) }
        if (playerSource != null && hit.damage > 0 && npc.hitpoints > 0) {
            hit.righthandObj?.let { eventBus.publish(NpcHitEvents.WeaponImpact(npc, hit, playerSource, it.toLong())) }
        }
        if (creditedPlayer != null) {
            eventBus.publish(NpcKillEvents.Credited(creditedPlayer, npc, hit))
        }
    }

    private fun StandardNpcAccess.playDefendSound(hit: Hit) {
        val source = if (hit.isFromPlayer) hit.resolvePlayerSource(playerList) else null
        if (source == null) {
            return
        }
        val defendSound = npc.visType.paramOrNull(params.defend_sound) ?: return
        source.soundSynth(defendSound)
    }

    private fun Hit.createHeadbar(currHp: Int, maxHp: Int, headbar: HeadbarType): Headbar =
        InternalNpcHeadbars.createFromHitmark(hitmark, currHp, maxHp, headbar)

    private fun Npc.publishHitEvent(hit: Hit) {
        val event = NpcHitEvents.Impact(this, hit)
        eventBus.publish(event)
    }
}
