package org.rsmod.content.minigames.tob

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType

public class TobCombat
@Inject
constructor(private val controller: TobController, private val accuracy: AccuracyFormulae) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (controller.isCombatPaused(npc)) return
            if (actionDelay > mapClock) return
            when (controller.roomFor(npc)) {
                TobRoom.Maiden -> magic(target, 36, 4, TobSeqs.maidenAttack)
                TobRoom.Nylocas ->
                    when (controller.requiredStyle(npc)) {
                        HitType.Melee -> melee(target, 28, 4)
                        HitType.Ranged -> ranged(target, 28, 4)
                        HitType.Magic -> magic(target, 28, 4)
                        else -> Unit
                    }
                TobRoom.Sotetseg -> {
                    controller.onSotetsegAttack(npc)
                    sotetseg(target)
                }
                TobRoom.Xarpus -> {
                    controller.onXarpusAttack(npc)
                    ranged(target, 32, 4, TobSeqs.xarpusAttack)
                }
                TobRoom.Verzik ->
                    when (controller.requiredStyle(npc)) {
                        HitType.Melee -> melee(target, 28, 4)
                        HitType.Ranged -> ranged(target, 28, 4)
                        HitType.Magic -> magic(target, 28, 4)
                        else -> Unit
                    }
                else -> Unit
            }
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        val target = controller.ownerFor(access.npc) ?: return
        if (
            controller.roomFor(access.npc) != TobRoom.Verzik || access.actionDelay > access.mapClock
        )
            return
        when (controller.verzikPhaseFor(access.npc)) {
            1 ->
                if (controller.useVerzikPillar(access.npc)) {
                    access.actionDelay = access.mapClock + 6
                    access.anim(TobSeqs.verzik1Attack)
                } else {
                    access.magic(target, 45, 6, TobSeqs.verzik1Attack)
                }
            2 ->
                if (access.isWithinDistance(target, 1)) {
                    access.melee(target, 50, 4, TobSeqs.verzik2Melee)
                } else {
                    access.ranged(target, 35, 4, TobSeqs.verzik2Attack)
                }
            3 -> {
                val rate = if (access.npc.hitpoints <= access.npc.baseHitpointsLvl / 5) 5 else 7
                when (access.random.of(0..2)) {
                    0 -> access.melee(target, 63, rate, TobSeqs.verzik3Melee)
                    1 -> access.ranged(target, 40, rate, TobSeqs.verzik3Ranged)
                    else -> access.magic(target, 40, rate, TobSeqs.verzik3Magic)
                }
                controller.onVerzikAttack(access.npc)
            }
        }
    }

    private fun StandardNpcAccess.sotetseg(target: Player) {
        if (isWithinDistance(target, 1)) {
            melee(target, 45, 5, TobSeqs.sotetsegMelee)
        } else if (random.of(0..1) == 0) {
            ranged(target, 45, 5, TobSeqs.sotetsegRanged)
        } else {
            magic(target, 45, 5, TobSeqs.sotetsegRanged)
        }
    }

    private fun StandardNpcAccess.melee(
        target: Player,
        maxHit: Int,
        rate: Int,
        animation: SeqType? = null,
    ) {
        actionDelay = mapClock + rate
        animation?.let(::anim)
        queue(target, HitType.Melee, maxHit, 1)
    }

    private fun StandardNpcAccess.ranged(
        target: Player,
        maxHit: Int,
        rate: Int,
        animation: SeqType? = null,
    ) {
        actionDelay = mapClock + rate
        animation?.let(::anim)
        queue(target, HitType.Ranged, maxHit, 3)
    }

    private fun StandardNpcAccess.magic(
        target: Player,
        maxHit: Int,
        rate: Int,
        animation: SeqType? = null,
    ) {
        actionDelay = mapClock + rate
        animation?.let(::anim)
        queue(target, HitType.Magic, maxHit, 3)
    }

    private fun StandardNpcAccess.queue(target: Player, type: HitType, maxHit: Int, delay: Int) {
        val accurate =
            when (type) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        val cap = controller.scaledMaxHit(npc, maxHit)
        val hit = target.queueHit(npc, delay, type, if (accurate) random.of(0..cap) else 0)
        controller.recordDamage(npc, hit.damage)
    }
}
