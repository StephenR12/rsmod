package org.rsmod.content.minigames.cox

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType

public class CoxCombat
@Inject
constructor(private val controller: CoxController, private val accuracy: AccuracyFormulae) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) return
            when (npc.id) {
                CoxNpcs.tekton.id -> melee(target, 45, 4, CoxSeqs.tektonAttack)
                CoxNpcs.vasa.id -> ranged(target, 33, 4, CoxSeqs.vasaAttack)
                CoxNpcs.vanguardMelee.id -> melee(target, 25, 4, CoxSeqs.vanguardMelee)
                CoxNpcs.vanguardRanged.id -> ranged(target, 25, 4, CoxSeqs.vanguardRanged)
                CoxNpcs.vanguardMagic.id -> magic(target, 25, 4, CoxSeqs.vanguardMagic)
                CoxNpcs.vespula.id -> ranged(target, 20, 2, CoxSeqs.vespulaAttack)
                CoxNpcs.vespulaPortal.id -> {
                    actionDelay = mapClock + 2
                    if (target.stat(stats.prayer) > 0) target.statSub(stats.prayer, 3, 0)
                    else target.queueHit(npc, 1, HitType.Typeless, 3)
                }
                CoxNpcs.babyMuttadile.id -> {
                    if (isWithinDistance(target, 1)) melee(target, 30, 4) else ranged(target, 30, 4)
                }
                CoxNpcs.muttadile.id -> hybrid(target, 45, 4)
                CoxNpcs.shamanA.id,
                CoxNpcs.shamanB.id -> ranged(target, 31, 4)
                CoxNpcs.mysticA.id,
                CoxNpcs.mysticB.id,
                CoxNpcs.mysticC.id -> {
                    if (isWithinDistance(target, 1) && target.vars[varbits.protect_from_magic] != 0) {
                        melee(target, 30, 4)
                    } else {
                        magic(target, 30, 4)
                    }
                }
                CoxNpcs.guardianLeft.id,
                CoxNpcs.guardianRight.id -> melee(target, 30, 5)
                CoxNpcs.iceDemon.id -> magic(target, 30, 4)
            }
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        if (access.npc.id != CoxNpcs.olmHead.id || access.actionDelay > access.mapClock) return
        val target = controller.ownerFor(access.npc) ?: return
        access.actionDelay = access.mapClock + 4
        access.anim(CoxSeqs.olmHeadAttack)
        when (access.random.of(0..4)) {
            0 ->
                target.queueHit(
                    access.npc,
                    3,
                    HitType.Typeless,
                    access.random.of(0..controller.scaledMaxHit(access.npc, 20)),
                )
            1,
            2 -> access.queue(target, HitType.Ranged, 29, 3)
            else -> access.queue(target, HitType.Magic, 29, 3)
        }
    }

    private fun StandardNpcAccess.hybrid(target: Player, maxHit: Int, rate: Int) {
        when {
            isWithinDistance(target, 1) -> melee(target, maxHit, rate)
            random.of(0..1) == 0 -> ranged(target, maxHit, rate)
            else -> magic(target, maxHit, rate)
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
        target.queueHit(npc, delay, type, if (accurate) random.of(0..cap) else 0)
    }
}
