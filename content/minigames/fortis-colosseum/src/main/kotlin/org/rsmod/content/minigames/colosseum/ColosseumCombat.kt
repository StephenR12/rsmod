package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType

public class ColosseumCombat
@Inject
constructor(
    private val controller: ColosseumController,
    private val accuracy: AccuracyFormulae,
    private val protectedAccess: ProtectedAccessLauncher,
) {
    private val attackCounts = java.util.IdentityHashMap<Npc, Int>()

    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (!controller.isCurrent(npc, target)) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) return
            when (npc.id) {
                ColosseumNpcs.meleer.id ->
                    melee(target, scaled(npc, 29), 5, ColosseumSeqs.meleerAttack)
                ColosseumNpcs.ranger.id ->
                    ranged(target, scaled(npc, 29), 5, 2, ColosseumSeqs.rangerAttack)
                ColosseumNpcs.mager.id ->
                    magic(target, scaled(npc, 29), 5, 2, ColosseumSeqs.magerAttack)
                ColosseumNpcs.shaman.id ->
                    magic(target, scaled(npc, 18), 5, 2, ColosseumSeqs.shamanAttack)
                ColosseumNpcs.jaguar.id -> jaguar(target)
                ColosseumNpcs.javelin.id -> javelin(target)
                ColosseumNpcs.manticore.id -> manticore(target)
                ColosseumNpcs.shockwave.id ->
                    magic(target, scaled(npc, 56), 5, 3, ColosseumSeqs.shockwaveAttack)
                ColosseumNpcs.minotaur.id,
                ColosseumNpcs.routefindingMinotaur.id -> minotaur(target)
                ColosseumNpcs.sol.id -> sol(target)
            }
        }
    }

    private fun StandardNpcAccess.jaguar(target: Player) {
        actionDelay = mapClock + 4
        anim(ColosseumSeqs.jaguarAttack)
        repeat(3) { i ->
            target.queueHit(npc, i + 1, HitType.Melee, roll(target, HitType.Melee, scaled(npc, 15)))
        }
    }

    private fun StandardNpcAccess.javelin(target: Player) {
        val count = (attackCounts[npc] ?: 0) + 1
        attackCounts[npc] = count
        if (count % 4 != 0) {
            ranged(target, scaled(npc, 48), 5, 2, ColosseumSeqs.javelinAttack)
            return
        }
        actionDelay = mapClock + 5
        anim(ColosseumSeqs.javelinArtillery)
        val marked = target.coords
        target.mes("A javelin barrage targets the ground beneath you!")
        protectedAccess.launch(target) {
            delay(6)
            if (controller.isCurrent(npc, player) && player.coords == marked) {
                player.queueHit(npc, 0, HitType.Typeless, random.of(0..scaled(npc, 40)))
                val reentry = controller.modifierLevel(npc, ColosseumModifier.Reentry)
                if (reentry > 0) {
                    delay(2)
                    if (player.coords == marked)
                        player.queueHit(npc, 0, HitType.Typeless, 4 * reentry)
                }
            }
        }
    }

    private fun StandardNpcAccess.manticore(target: Player) {
        actionDelay = mapClock + 10
        anim(ColosseumSeqs.manticoreCharge)
        val tier = controller.modifierLevel(npc, ColosseumModifier.Mantimayhem)
        val start = if (tier >= 3) random.of(0..2) else (attackCounts[npc] ?: 0) % 3
        attackCounts[npc] = (attackCounts[npc] ?: 0) + 1
        val styles = listOf(HitType.Magic, HitType.Ranged, HitType.Melee)
        repeat(3) { i ->
            val style = styles[(start + i) % styles.size]
            target.queueHit(npc, 7 + i, style, roll(target, style, scaled(npc, 22)))
        }
    }

    private fun StandardNpcAccess.minotaur(target: Player) {
        val count = (attackCounts[npc] ?: 0) + 1
        attackCounts[npc] = count
        if (count % 4 == 0) {
            actionDelay = mapClock + 5
            anim(ColosseumSeqs.minotaurMagic)
            controller.healAllies(npc)
        } else {
            melee(target, scaled(npc, 74), 5, ColosseumSeqs.minotaurMelee)
        }
    }

    private fun StandardNpcAccess.sol(target: Player) {
        val count = attackCounts[npc] ?: 0
        attackCounts[npc] = count + 1
        when (count % 5) {
            0,
            1 -> melee(target, 48, 5, ColosseumSeqs.solMelee)
            2 -> {
                actionDelay = mapClock + 6
                anim(ColosseumSeqs.solTriple)
                listOf(HitType.Melee, HitType.Ranged, HitType.Magic).forEachIndexed { index, style
                    ->
                    target.queueHit(npc, index + 2, style, roll(target, style, 35))
                }
            }
            3 -> {
                actionDelay = mapClock + 6
                anim(ColosseumSeqs.solGrapple)
                target.mes("Sol prepares to grapple you—move away!")
                val marked = target.coords
                protectedAccess.launch(target) {
                    delay(3)
                    if (controller.isCurrent(npc, player) && player.coords == marked)
                        player.queueHit(npc, 0, HitType.Typeless, random.of(20..45))
                }
            }
            else -> {
                actionDelay = mapClock + 6
                anim(ColosseumSeqs.solShieldSlam)
                if (isWithinDistance(target, 2))
                    target.queueHit(npc, 2, HitType.Melee, roll(target, HitType.Melee, 50))
            }
        }
    }

    private fun scaled(npc: Npc, maxHit: Int): Int {
        val level = controller.modifierLevel(npc, ColosseumModifier.Relentless)
        return max(1, maxHit * ColosseumModifierRules.relentlessPercent(level) / 100)
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
    ) {
        actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, delay, HitType.Ranged, roll(target, HitType.Ranged, maxHit))
    }

    private fun StandardNpcAccess.magic(
        target: Player,
        maxHit: Int,
        rate: Int,
        delay: Int,
        animation: SeqType,
    ) {
        actionDelay = mapClock + rate
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
