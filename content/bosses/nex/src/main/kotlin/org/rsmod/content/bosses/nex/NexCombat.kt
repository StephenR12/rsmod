package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

public class NexCombat
@Inject
constructor(
    private val controller: NexController,
    private val accuracy: AccuracyFormulae,
    private val protectedAccess: ProtectedAccessLauncher,
    private val deathStorage: NexDeathStorage,
) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            deathStorage.markAtRisk(target)
            when {
                npc.id == NexNpcs.bloodReaver.id -> reaverAttack(target)
                npc.id in NexNpcs.mages.map { it.id } -> mageAttack(target)
                npc.id in NexNpcs.prisonFollowers.map { it.id } -> prisonFollowerAttack(target)
                else -> nexAttack(target)
            }
        }
    }

    private fun StandardNpcAccess.nexAttack(target: Player) {
        actionDelay = mapClock + ATTACK_RATE
        if (controller.consumeAttack(npc)) {
            special(target, controller.phase(npc))
            return
        }

        val phase = controller.phase(npc)
        val melee = phase != NexPhase.Shadow && isWithinDistance(target, 1) && random.of(3) == 0
        if (melee) {
            anim(NexSeqs.attack)
            val hit = queue(target, HitType.Melee, MELEE_MAX_HIT, 1)
            applyFinalCurse(hit)
            return
        }

        val type = if (phase == NexPhase.Shadow) HitType.Ranged else HitType.Magic
        anim(if (type == HitType.Magic) NexSeqs.cast else NexSeqs.alternateCast)
        val victims =
            if (phase == NexPhase.Smoke || phase == NexPhase.Blood || phase == NexPhase.Ice) {
                controller.roomPlayers(npc, target).filter {
                    distance(it.coords, target.coords) <= 1
                }
            } else {
                listOf(target)
            }
        victims.forEach { victim ->
            victim.spotanim(phase.attackSpot)
            val maxHit = if (type == HitType.Ranged) RANGED_MAX_HIT else MAGIC_MAX_HIT
            val hit = queue(victim, type, maxHit, PROJECTILE_DELAY)
            when (phase) {
                NexPhase.Blood -> {
                    heal(npc, hit.damage / 10)
                    drainPrayer(victim, hit.damage / 3)
                }
                NexPhase.Ice -> drainPrayer(victim, hit.damage / 2)
                NexPhase.Shadow -> drainPrayer(victim, hit.damage / 10)
                NexPhase.Zaros -> {
                    drainPrayer(victim, if (hit.damage > 0) 5 else 0)
                    applyFinalCurse(hit)
                }
                NexPhase.Smoke -> Unit
            }
        }
    }

    private fun StandardNpcAccess.special(target: Player, phase: NexPhase) {
        when (phase) {
            NexPhase.Smoke -> smokeSpecial(target)
            NexPhase.Shadow -> shadowSmash(target)
            NexPhase.Blood -> bloodSpecial(target)
            NexPhase.Ice -> iceSpecial(target)
            NexPhase.Zaros -> Unit
        }
    }

    private fun StandardNpcAccess.smokeSpecial(target: Player) {
        if (controller.toggleSpecial(npc)) {
            npc.say("There is no escape!")
            anim(NexSeqs.dash)
            target.queueHit(npc, 1, HitType.Typeless, random.of(35..50))
        } else {
            npc.say("Let the virus flow through you!")
            anim(NexSeqs.cast)
            controller
                .roomPlayers(npc, target)
                .filter { distance(it.coords, target.coords) <= 2 }
                .forEach { victim ->
                    victim.spotanim(NexSpotanims.smoke)
                    queue(victim, HitType.Magic, 20, PROJECTILE_DELAY)
                }
        }
    }

    private fun StandardNpcAccess.shadowSmash(target: Player) {
        npc.say("Fear the shadow!")
        anim(NexSeqs.smash)
        val threatened = target.coords
        val source = npc
        protectedAccess.launch(target) {
            delay(SHADOW_DELAY)
            if (player.coords == threatened && source.slotId != INVALID_SLOT) {
                player.spotanim(NexSpotanims.shadow)
                player.queueHit(source, 1, HitType.Typeless, random.of(40..50))
            }
        }
    }

    private fun StandardNpcAccess.bloodSpecial(target: Player) {
        if (controller.toggleSpecial(npc)) {
            npc.say("A siphon will solve this!")
            anim(NexSeqs.siphon)
            controller.beginSiphon(npc)
        } else {
            npc.say("I demand a blood sacrifice!")
            val source = npc
            target.spotanim(NexSpotanims.blood)
            protectedAccess.launch(target) {
                delay(SACRIFICE_DELAY)
                if (source.slotId == INVALID_SLOT || distance(player.coords, source.coords) >= 7) {
                    return@launch
                }
                val damage = random.of(40..50)
                val hit = player.queueHit(source, 1, HitType.Typeless, damage)
                heal(source, hit.damage)
                val current = player.stat(stats.prayer)
                player.statSub(stats.prayer, constant = current / 3, percent = 0)
            }
        }
    }

    private fun StandardNpcAccess.iceSpecial(target: Player) {
        if (controller.toggleSpecial(npc)) {
            npc.say("Contain this!")
            anim(NexSeqs.smash)
            controller
                .roomPlayers(npc, target)
                .filter { distance(it.coords, npc.coords) <= 2 }
                .forEach { victim ->
                    val hit =
                        victim.queueHit(
                            npc,
                            1,
                            HitType.Ranged,
                            random.of(40..60),
                            modifier = NexPlayerHitModifier,
                        )
                    if (hit.damage > 0) {
                        protectedAccess.launch(victim) {
                            vars[varbits.protect_from_melee] = 0
                            vars[varbits.protect_from_missiles] = 0
                            vars[varbits.protect_from_magic] = 0
                        }
                    }
                }
        } else {
            npc.say("Die now, in a prison of ice!")
            val source = npc
            val trapped = target.coords
            target.spotanim(NexSpotanims.icePrison)
            protectedAccess.launch(target) {
                delay(ICE_PRISON_DELAY)
                if (source.slotId != INVALID_SLOT && distance(player.coords, trapped) < 3) {
                    player.queueHit(source, 1, HitType.Typeless, random.of(55..75))
                }
            }
        }
    }

    private fun StandardNpcAccess.mageAttack(target: Player) {
        if (!controller.isMageActive(npc) || !isWithinDistance(target, 18)) {
            return
        }
        actionDelay = mapClock + MAGE_ATTACK_RATE
        anim(NexSeqs.cast)
        val impact =
            when (npc.id) {
                NexNpcs.fumus.id -> NexSpotanims.smoke
                NexNpcs.umbra.id -> NexSpotanims.shadow
                NexNpcs.cruor.id -> NexSpotanims.blood
                else -> NexSpotanims.ice
            }
        target.spotanim(impact)
        queue(target, HitType.Magic, MAGE_MAX_HIT, PROJECTILE_DELAY)
    }

    private fun StandardNpcAccess.reaverAttack(target: Player) {
        if (!isWithinDistance(target, 1)) {
            return
        }
        actionDelay = mapClock + REAVER_ATTACK_RATE
        anim(NexSeqs.reaverAttack)
        queue(target, HitType.Melee, REAVER_MAX_HIT, 1)
    }

    private fun StandardNpcAccess.prisonFollowerAttack(target: Player) {
        val type =
            when (npc.id) {
                NexNpcs.prisonRanger.id -> HitType.Ranged
                NexNpcs.prisonMage.id -> HitType.Magic
                else -> HitType.Melee
            }
        if (type == HitType.Melee && !isWithinDistance(target, 1)) {
            return
        }
        if (type != HitType.Melee && !isWithinDistance(target, 10)) {
            return
        }
        actionDelay = mapClock + PRISON_ATTACK_RATE
        anim(if (type == HitType.Melee) NexSeqs.reaverAttack else NexSeqs.cast)
        queue(target, type, PRISON_MAX_HIT, if (type == HitType.Melee) 1 else PROJECTILE_DELAY)
    }

    private fun StandardNpcAccess.queue(
        target: Player,
        type: HitType,
        maxHit: Int,
        delay: Int,
    ): Hit {
        deathStorage.markAtRisk(target)
        val accurate =
            when (type) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        val damage = if (accurate) random.of(0..maxHit) else 0
        return target.queueHit(npc, delay, type, damage, modifier = NexPlayerHitModifier)
    }

    private fun StandardNpcAccess.applyFinalCurse(hit: Hit) {
        if (controller.state(npc).curse == NexCurse.SoulSplit && hit.damage > 0) {
            npc.spotanim(NexSpotanims.soulSplit)
            heal(npc, max(1, hit.damage / 10))
        }
    }

    private fun drainPrayer(player: Player, amount: Int) {
        if (amount > 0) {
            player.statSub(stats.prayer, constant = amount, percent = 0)
        }
    }

    private fun heal(npc: Npc, amount: Int) {
        if (amount > 0) {
            npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + amount)
        }
    }

    private fun distance(first: CoordGrid, second: CoordGrid): Int =
        max(abs(first.x - second.x), abs(first.z - second.z))

    public companion object {
        public const val ATTACK_RATE: Int = 4
        public const val MAGIC_MAX_HIT: Int = 33
        public const val RANGED_MAX_HIT: Int = 60
        public const val MELEE_MAX_HIT: Int = 27
        public const val MAGE_MAX_HIT: Int = 29
        public const val REAVER_MAX_HIT: Int = 18
        public const val PRISON_MAX_HIT: Int = 15
        private const val PROJECTILE_DELAY: Int = 3
        private const val MAGE_ATTACK_RATE: Int = 5
        private const val REAVER_ATTACK_RATE: Int = 5
        private const val PRISON_ATTACK_RATE: Int = 5
        private const val SHADOW_DELAY: Int = 3
        private const val SACRIFICE_DELAY: Int = 7
        private const val ICE_PRISON_DELAY: Int = 3
    }
}
