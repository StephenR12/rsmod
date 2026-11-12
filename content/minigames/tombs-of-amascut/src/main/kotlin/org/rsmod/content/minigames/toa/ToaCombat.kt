package org.rsmod.content.minigames.toa

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType

public class ToaCombat
@Inject
constructor(private val controller: ToaController, private val accuracy: AccuracyFormulae) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) return
            when (controller.encounterFor(npc)) {
                ToaEncounter.ScabarasChallenge,
                ToaEncounter.Kephri -> ranged(target, 28, 5, ToaSeqs.kephriAttack)
                ToaEncounter.ApmekenChallenge ->
                    when (controller.requiredStyle(npc)) {
                        HitType.Melee -> melee(target, 18, 4)
                        HitType.Ranged -> ranged(target, 18, 4)
                        HitType.Magic -> magic(target, 18, 4)
                        else -> magic(target, 15, 5)
                    }
                ToaEncounter.Baba -> melee(target, 35, 5, ToaSeqs.babaAttack)
                ToaEncounter.Zebak -> ranged(target, 34, 5)
                ToaEncounter.Akkha ->
                    when (npc.visType.id) {
                        ToaNpcs.akkhaRanged.id -> ranged(target, 30, 4, ToaSeqs.akkhaRanged)
                        ToaNpcs.akkhaMage.id,
                        ToaNpcs.akkhaEnrage.id -> magic(target, 30, 4, ToaSeqs.akkhaMagic)
                        else -> melee(target, 30, 4, ToaSeqs.akkhaMelee)
                    }
                ToaEncounter.Wardens ->
                    when (npc.visType.id) {
                        ToaNpcs.wardenP2Range.id -> ranged(target, 36, 5, ToaSeqs.wardenRanged)
                        ToaNpcs.wardenP2Mage.id -> magic(target, 36, 5, ToaSeqs.wardenMagic)
                        else -> melee(target, 42, 5, ToaSeqs.wardenMelee)
                    }
                else -> Unit
            }
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
        target.queueHit(
            npc,
            delay,
            type,
            if (accurate) random.of(0..controller.scaledMaxHit(npc, maxHit)) else 0,
        )
    }
}
