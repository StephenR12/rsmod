package org.rsmod.content.minigames.fightcaves

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType

public class FightCaveCombat
@Inject
constructor(
    private val controller: FightCaveController,
    private val accuracy: AccuracyFormulae,
    private val players: PlayerList,
) {
    public fun recoil(npc: org.rsmod.game.entity.Npc, hit: Hit) {
        if (!hit.isFromPlayer || hit.type != HitType.Melee || hit.damage <= 0) {
            return
        }
        hit.resolvePlayerSource(players)?.queueHit(npc, 1, HitType.Typeless, 1)
    }

    public fun aggroHealer(npc: org.rsmod.game.entity.Npc, hit: Hit) {
        if (!hit.isFromPlayer) {
            return
        }
        val player = hit.resolvePlayerSource(players) ?: return
        controller.aggroHealer(npc, player)
    }

    public fun healerPulse(access: StandardNpcAccess) {
        with(access) {
            val jad = controller.jadFor(npc)
            if (jad == null) {
                npc.clearTimer(org.rsmod.api.config.refs.timers.fight_cave_healer_heal)
                return
            }
            val fightingPlayer = npc.mode == NpcMode.OpPlayer2 || npc.mode == NpcMode.ApPlayer2
            if (isWithinDistance(jad, HEAL_RANGE)) {
                controller.healJad(npc)
            } else if (!fightingPlayer) {
                walk(jad.coords)
            }
            timer(org.rsmod.api.config.refs.timers.fight_cave_healer_heal, HEAL_RATE)
        }
    }

    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) {
                return
            }
            when (npc.id) {
                FightCaveNpcs.tzKihA.id,
                FightCaveNpcs.tzKihB.id -> tzKih(target)
                FightCaveNpcs.tzKekA.id,
                FightCaveNpcs.tzKekB.id ->
                    standardMelee(target, maxHit = 7, rate = 4, FightCaveSeqs.tzKekAttack)
                FightCaveNpcs.tzKekSplit.id ->
                    standardMelee(target, maxHit = 4, rate = 4, FightCaveSeqs.tzKekAttack)
                FightCaveNpcs.tokXilA.id,
                FightCaveNpcs.tokXilB.id ->
                    hybrid(
                        target,
                        HitType.Ranged,
                        maxHit = 13,
                        rate = 4,
                        FightCaveSeqs.tokXilAttack,
                    )
                FightCaveNpcs.ytMejKotA.id,
                FightCaveNpcs.ytMejKotB.id -> ytMejKot(target)
                FightCaveNpcs.ketZekA.id,
                FightCaveNpcs.ketZekB.id ->
                    hybrid(target, HitType.Magic, maxHit = 54, rate = 4, FightCaveSeqs.ketZekMagic)
                FightCaveNpcs.jad.id -> jad(target)
                FightCaveNpcs.healer.id ->
                    standardMelee(target, maxHit = 14, rate = 4, FightCaveSeqs.ytMejKotAttack)
            }
        }
    }

    private fun StandardNpcAccess.tzKih(target: Player) {
        actionDelay = mapClock + 4
        anim(FightCaveSeqs.tzKihAttack)
        val damage = rollDamage(target, HitType.Melee, 4)
        val hit = target.queueHit(npc, 1, HitType.Melee, damage)
        target.statSub(stats.prayer, constant = hit.damage + 1, percent = 0)
    }

    private fun StandardNpcAccess.standardMelee(
        target: Player,
        maxHit: Int,
        rate: Int,
        animation: org.rsmod.game.type.seq.SeqType,
    ) {
        actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, 1, HitType.Melee, rollDamage(target, HitType.Melee, maxHit))
    }

    private fun StandardNpcAccess.hybrid(
        target: Player,
        distantType: HitType,
        maxHit: Int,
        rate: Int,
        animation: org.rsmod.game.type.seq.SeqType,
    ) {
        if (isWithinDistance(target, 1)) {
            standardMelee(target, maxHit, rate, animation)
            return
        }
        actionDelay = mapClock + rate
        anim(animation)
        target.queueHit(npc, 3, distantType, rollDamage(target, distantType, maxHit))
    }

    private fun StandardNpcAccess.ytMejKot(target: Player) {
        if (npc.hitpoints < npc.baseHitpointsLvl / 2) {
            npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + 10)
        }
        standardMelee(target, maxHit = 25, rate = 7, FightCaveSeqs.ytMejKotAttack)
    }

    private fun StandardNpcAccess.jad(target: Player) {
        if (isWithinDistance(target, 1)) {
            standardMelee(target, maxHit = 97, rate = 8, FightCaveSeqs.jadMelee)
            return
        }
        actionDelay = mapClock + 8
        if (random.of(0..1) == 0) {
            anim(FightCaveSeqs.jadRanged)
            target.queueHit(npc, 4, HitType.Ranged, rollDamage(target, HitType.Ranged, 97))
        } else {
            anim(FightCaveSeqs.jadMagic)
            target.queueHit(npc, 4, HitType.Magic, rollDamage(target, HitType.Magic, 95))
        }
    }

    private fun StandardNpcAccess.rollDamage(target: Player, type: HitType, maxHit: Int): Int {
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

    private companion object {
        private const val HEAL_RANGE = 4
        private const val HEAL_RATE = 4
    }
}
