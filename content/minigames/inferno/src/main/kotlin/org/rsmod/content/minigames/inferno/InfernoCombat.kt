package org.rsmod.content.minigames.inferno

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType

public class InfernoCombat
@Inject
constructor(private val controller: InfernoController, private val accuracy: AccuracyFormulae) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) return
            when (npc.id) {
                InfernoNpcs.nibbler.id -> melee(target, 4, 4, InfernoSeqs.nibblerAttack)
                InfernoNpcs.bat.id -> bat(target)
                InfernoNpcs.blob.id -> blob(target, 29)
                InfernoNpcs.blobMelee.id -> melee(target, 18, 4, InfernoSeqs.blobMelee)
                InfernoNpcs.blobRange.id -> ranged(target, 18, 4, 3, InfernoSeqs.blobRanged)
                InfernoNpcs.blobMage.id -> magic(target, 18, 4, 3, InfernoSeqs.blobMagic)
                InfernoNpcs.melee.id -> melee(target, 49, 4, InfernoSeqs.meleeAttack)
                InfernoNpcs.ranger.id,
                InfernoNpcs.finalRanger.id ->
                    hybrid(target, HitType.Ranged, 46, 4, InfernoSeqs.rangerRanged)
                InfernoNpcs.mager.id,
                InfernoNpcs.finalMager.id ->
                    hybrid(target, HitType.Magic, 70, 4, InfernoSeqs.magerMagic)
                InfernoNpcs.jad.id,
                InfernoNpcs.finalJad.id -> jad(target)
                InfernoNpcs.jadHealer.id,
                InfernoNpcs.finalJadHealer.id -> melee(target, 18, 4, InfernoSeqs.meleeAttack)
                InfernoNpcs.zukHealer.id -> magic(target, 10, 3, 2, InfernoSeqs.magerMagic)
            }
        }
    }

    public fun pulse(access: StandardNpcAccess) {
        if (access.npc.id != InfernoNpcs.zuk.id || access.actionDelay > access.mapClock) return
        val target = controller.ownerFor(access.npc) ?: return
        access.actionDelay = access.mapClock + if (access.npc.hitpoints <= 240) 7 else 10
        access.anim(InfernoSeqs.zukAttack)
        if (!controller.isShielded(access.npc, target)) {
            target.queueHit(access.npc, 5, HitType.Typeless, access.random.of(0..148))
        }
    }

    private fun StandardNpcAccess.bat(target: Player) {
        actionDelay = mapClock + 3
        anim(InfernoSeqs.batAttack)
        val hit = target.queueHit(npc, 2, HitType.Ranged, roll(target, HitType.Ranged, 19))
        if (hit.damage > 0) {
            target.runEnergy = max(0, target.runEnergy - 300)
            UpdateRun.energy(target, target.runEnergy)
            val combatStats =
                listOf(stats.attack, stats.strength, stats.defence, stats.ranged, stats.magic)
            target.statSub(combatStats[random.of(combatStats.indices)], constant = 1, percent = 0)
        }
    }

    private fun StandardNpcAccess.blob(target: Player, maxHit: Int) {
        if (isWithinDistance(target, 1) && target.vars[varbits.protect_from_melee] == 0) {
            melee(target, maxHit, 6, InfernoSeqs.blobMelee)
            return
        }
        actionDelay = mapClock + 6
        when {
            target.vars[varbits.protect_from_magic] != 0 ->
                ranged(target, maxHit, 6, 3, InfernoSeqs.blobRanged, setDelay = false)
            target.vars[varbits.protect_from_missiles] != 0 ->
                magic(target, maxHit, 6, 3, InfernoSeqs.blobMagic, setDelay = false)
            random.of(0..1) == 0 ->
                ranged(target, maxHit, 6, 3, InfernoSeqs.blobRanged, setDelay = false)
            else -> magic(target, maxHit, 6, 3, InfernoSeqs.blobMagic, setDelay = false)
        }
    }

    private fun StandardNpcAccess.jad(target: Player) {
        if (isWithinDistance(target, 1)) {
            melee(target, 113, 8, InfernoSeqs.jadMelee)
            return
        }
        actionDelay = mapClock + 8
        if (random.of(0..1) == 0) {
            ranged(target, 113, 8, 4, InfernoSeqs.jadRanged, setDelay = false)
        } else {
            magic(target, 113, 8, 4, InfernoSeqs.jadMagic, setDelay = false)
        }
    }

    private fun StandardNpcAccess.hybrid(
        target: Player,
        distant: HitType,
        maxHit: Int,
        rate: Int,
        animation: SeqType,
    ) {
        if (isWithinDistance(target, 1)) {
            val meleeAnim =
                if (distant == HitType.Ranged) InfernoSeqs.rangerMelee else InfernoSeqs.magerMelee
            melee(target, maxHit, rate, meleeAnim)
        } else if (distant == HitType.Ranged) {
            ranged(target, maxHit, rate, 3, animation)
        } else {
            magic(target, maxHit, rate, 3, animation)
        }
    }

    private fun StandardNpcAccess.melee(
        target: Player,
        maxHit: Int,
        rate: Int,
        animation: SeqType,
    ) {
        actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, 1, HitType.Melee, roll(target, HitType.Melee, maxHit))
    }

    private fun StandardNpcAccess.ranged(
        target: Player,
        maxHit: Int,
        rate: Int,
        delay: Int,
        animation: SeqType,
        setDelay: Boolean = true,
    ) {
        if (setDelay) actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, delay, HitType.Ranged, roll(target, HitType.Ranged, maxHit))
    }

    private fun StandardNpcAccess.magic(
        target: Player,
        maxHit: Int,
        rate: Int,
        delay: Int,
        animation: SeqType,
        setDelay: Boolean = true,
    ) {
        if (setDelay) actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, delay, HitType.Magic, roll(target, HitType.Magic, maxHit))
    }

    private fun StandardNpcAccess.roll(target: Player, type: HitType, maxHit: Int): Int {
        val accurate =
            when (type) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }
}
