package org.rsmod.content.bosses.corporealbeast

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

public class CorporealBeastCombat
@Inject
constructor(
    private val controller: CorporealBeastController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
) {
    public fun bossAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            actionDelay = mapClock + CorporealBeastStats.ATTACK_RATE
            val state = controller.state(npc)
            state.attacks++
            when {
                state.attacks % 7 == 0 && isWithinDistance(target, 2) ->
                    target.queueHit(
                        npc,
                        1,
                        HitType.Typeless,
                        random.of(0..CorporealBeastStats.STOMP_MAX_HIT),
                    )
                isWithinDistance(target, 1) && state.attacks % 3 == 0 ->
                    target.queueHit(
                        npc,
                        1,
                        HitType.Melee,
                        damage(target, HitType.Melee, CorporealBeastStats.MELEE_MAX_HIT),
                    )
                else -> {
                    val hit = damage(target, HitType.Magic, CorporealBeastStats.MAGIC_MAX_HIT)
                    target.queueHit(npc, 2, HitType.Magic, hit)
                    if (state.attacks % 5 == 0) {
                        target.statSub(stats.prayer, constant = 5, percent = 0)
                    }
                }
            }
        }
    }

    public fun coreAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock || !isWithinDistance(target, 1)) {
                return
            }
            actionDelay = mapClock + 4
            target.queueHit(npc, 1, HitType.Typeless, CorporealBeastController.DARK_CORE_DRAIN)
            controller.healFromCore(npc, CorporealBeastController.DARK_CORE_DRAIN)
        }
    }

    private fun StandardNpcAccess.damage(target: Player, type: HitType, maxHit: Int): Int {
        val accurate =
            when (type) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                else -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }
}
