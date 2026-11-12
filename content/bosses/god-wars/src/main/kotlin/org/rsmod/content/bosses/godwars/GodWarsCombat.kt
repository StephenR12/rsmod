package org.rsmod.content.bosses.godwars

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.zone.ZoneKey

public class GodWarsCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val players: PlayerRepository,
) {
    public fun attack(access: StandardNpcAccess, target: Player, config: GodWarsCombatant) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            when (config.boss) {
                GodWarsBoss.Graardor -> graardor(target, config)
                GodWarsBoss.Kree -> kree(target, config)
                GodWarsBoss.Zilyana -> zilyana(target, config)
                GodWarsBoss.Kril -> kril(target, config)
                null -> minion(target, config)
            }
        }
    }

    private fun StandardNpcAccess.graardor(target: Player, config: GodWarsCombatant) {
        if (!isWithinDistance(target, 1)) {
            return
        }
        actionDelay = mapClock + config.attackRate
        anim(GodWarsSeqs.graardorAttack)
        if (random.of(0..2) == 0) {
            roomPlayers(target, GodWarsBoss.Graardor).forEach { player ->
                player.spotanim(GodWarsSpotanims.graardorRanged)
                val damage = if (roll(player, HitType.Ranged)) random.of(15..35) else 0
                player.queueHit(npc, 2, HitType.Ranged, damage)
            }
        } else {
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, config.maxHit))
        }
    }

    private fun StandardNpcAccess.kree(target: Player, config: GodWarsCombatant) {
        actionDelay = mapClock + config.attackRate
        if (isWithinDistance(target, 1) && random.of(0..2) == 0) {
            anim(GodWarsSeqs.kreeMelee)
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, 26))
            return
        }
        val type = if (random.of(0..2) == 0) HitType.Magic else HitType.Ranged
        anim(GodWarsSeqs.kreeRanged)
        roomPlayers(target, GodWarsBoss.Kree).forEach { player ->
            player.spotanim(
                if (type == HitType.Magic) GodWarsSpotanims.kreeMagic
                else GodWarsSpotanims.kreeRanged
            )
            player.queueHit(
                npc,
                2,
                type,
                damage(player, type, if (type == HitType.Magic) 21 else config.maxHit),
            )
        }
    }

    private fun StandardNpcAccess.zilyana(target: Player, config: GodWarsCombatant) {
        if (!isWithinDistance(target, 1)) {
            return
        }
        actionDelay = mapClock + config.attackRate
        if (random.of(0..2) == 0) {
            anim(GodWarsSeqs.zilyanaMagic)
            target.spotanim(GodWarsSpotanims.zilyanaMagic)
            val damage = if (roll(target, HitType.Magic)) random.of(10..20) else 0
            target.queueHit(npc, 1, HitType.Magic, damage)
        } else {
            anim(GodWarsSeqs.zilyanaAttack)
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, config.maxHit))
        }
    }

    private fun StandardNpcAccess.kril(target: Player, config: GodWarsCombatant) {
        if (!isWithinDistance(target, 1)) {
            return
        }
        actionDelay = mapClock + config.attackRate
        if (random.of(0..2) == 0) {
            anim(GodWarsSeqs.krilMagic)
            target.spotanim(GodWarsSpotanims.krilMagic)
            val damage = if (roll(target, HitType.Magic)) random.of(10..30) else 0
            target.queueHit(npc, 2, HitType.Magic, damage)
            return
        }

        anim(GodWarsSeqs.krilAttack)
        val usesSpecial = target.vars[varbits.protect_from_melee] == 1 && random.of(0..8) == 0
        if (usesSpecial) {
            target.queueHit(
                npc,
                1,
                HitType.Melee,
                random.of(35..49),
                modifier = NoopPlayerHitModifier,
            )
            target.statSub(stats.prayer, constant = target.stat(stats.prayer) / 2, percent = 0)
        } else {
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, config.maxHit))
        }
    }

    private fun StandardNpcAccess.minion(target: Player, config: GodWarsCombatant) {
        val range = if (config.style == GodWarsStyle.Melee) 1 else 8
        if (!isWithinDistance(target, range)) {
            return
        }
        actionDelay = mapClock + config.attackRate
        anim(config.animation)
        val type =
            when (config.style) {
                GodWarsStyle.Melee -> HitType.Melee
                GodWarsStyle.Ranged -> HitType.Ranged
                GodWarsStyle.Magic -> HitType.Magic
            }
        target.queueHit(
            npc,
            if (type == HitType.Melee) 1 else 2,
            type,
            damage(target, type, config.maxHit),
        )
    }

    private fun StandardNpcAccess.roomPlayers(primary: Player, boss: GodWarsBoss): List<Player> {
        val room = GodWarsDungeon.roomByBoss.getValue(boss)
        return (sequenceOf(primary) + players.findAll(ZoneKey.from(npc.coords), 2))
            .filter(room::contains)
            .distinct()
            .toList()
    }

    private fun StandardNpcAccess.damage(target: Player, type: HitType, maxHit: Int): Int =
        if (roll(target, type)) random.of(0..maxHit) else 0

    private fun StandardNpcAccess.roll(target: Player, type: HitType): Boolean =
        when (type) {
            HitType.Melee -> accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
            HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
            HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
            HitType.Typeless -> true
        }
}
