package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

public class WildernessBossCombat
@Inject
constructor(
    private val controller: WildernessBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val protectedAccess: ProtectedAccessLauncher,
) {
    public fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: WildernessBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            val state = controller.state(npc)
            state.attacks++
            actionDelay = mapClock + if (state.enraged) 4 else definition.attackRate
            when (definition.family) {
                WildernessBossFamily.Callisto -> callisto(target, definition, state)
                WildernessBossFamily.Venenatis -> venenatis(target, definition, state)
                WildernessBossFamily.Vetion -> vetion(target, definition, state)
                WildernessBossFamily.Scorpia ->
                    melee(target, definition.maxHit, WildernessBossSeqs.scorpion)
                WildernessBossFamily.ChaosElemental -> chaosElemental(target, definition, state)
                WildernessBossFamily.ChaosFanatic ->
                    archaeologist(target, definition, state, fanatic = true)
                WildernessBossFamily.CrazyArchaeologist ->
                    archaeologist(target, definition, state, fanatic = false)
                WildernessBossFamily.KingBlackDragon -> kingBlackDragon(target, definition, state)
            }
        }
    }

    public fun minionAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock || !isWithinDistance(target, 1)) {
                return
            }
            actionDelay = mapClock + 4
            anim(WildernessBossSeqs.scorpion)
            val maxHit =
                when (npc.id) {
                    WildernessBossNpcs.venenatisSpiderling.id,
                    WildernessBossNpcs.spindelSpiderling.id -> 4
                    WildernessBossNpcs.scorpiaGuardian.id -> 8
                    else -> 18
                }
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, maxHit))
            if (
                npc.id == WildernessBossNpcs.venenatisSpiderling.id ||
                    npc.id == WildernessBossNpcs.spindelSpiderling.id
            ) {
                target.statSub(stats.prayer, constant = 1, percent = 0)
            }
            if (npc.id == WildernessBossNpcs.scorpiaGuardian.id) {
                controller.healScorpia(npc)
            }
        }
    }

    private fun StandardNpcAccess.callisto(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
    ) {
        if (state.attacks % 6 == 0) {
            anim(WildernessBossSeqs.bearMagic)
            target.spotanim(WildernessBossSpots.callistoTrap)
            target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 50))
            target.delay = max(target.delay, mapClock + 2)
            return
        }
        if (isWithinDistance(target, 1)) {
            melee(target, definition.maxHit, WildernessBossSeqs.bearMelee)
        } else {
            anim(WildernessBossSeqs.bearRanged)
            target.spotanim(WildernessBossSpots.callistoRanged)
            target.queueHit(npc, 2, HitType.Ranged, damage(target, HitType.Ranged, 31))
        }
    }

    private fun StandardNpcAccess.venenatis(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
    ) {
        if (isWithinDistance(target, 1)) {
            melee(target, definition.maxHit, WildernessBossSeqs.spiderMelee)
            return
        }
        val cycle = (state.attacks - 1) % 16
        if (cycle == 0) {
            controller.spawnSpiderlings(npc, definition.lesser)
        }
        if (cycle == 10) {
            anim(WildernessBossSeqs.spiderMagic)
            target.spotanim(WildernessBossSpots.venenatisWeb)
            target.queueHit(npc, 2, HitType.Typeless, 3)
            target.statSub(stats.prayer, constant = 3, percent = 0)
            target.runEnergy = (target.runEnergy - 100).coerceAtLeast(0)
            return
        }
        val ranged = cycle < 8
        anim(if (ranged) WildernessBossSeqs.spiderRanged else WildernessBossSeqs.spiderMagic)
        target.spotanim(
            if (ranged) WildernessBossSpots.venenatisRanged else WildernessBossSpots.venenatisMagic
        )
        val type = if (ranged) HitType.Ranged else HitType.Magic
        target.queueHit(npc, 2, type, damage(target, type, if (ranged) 35 else 30))
    }

    private fun StandardNpcAccess.vetion(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
    ) {
        if (state.attacks % 5 == 0 && isWithinDistance(target, 2)) {
            anim(WildernessBossSeqs.vetionMelee)
            target.queueHit(npc, 2, HitType.Typeless, random.of(20..definition.maxHit))
            target.delay = max(target.delay, mapClock + 3)
            return
        }
        anim(WildernessBossSeqs.vetionMagic)
        target.spotanim(WildernessBossSpots.vetionMagic)
        target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, definition.maxHit))
    }

    private fun StandardNpcAccess.chaosElemental(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
    ) {
        anim(WildernessBossSeqs.chaosElemental)
        target.spotanim(WildernessBossSpots.chaosImpact)
        target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, definition.maxHit))
        if (state.attacks % 4 == 0) {
            protectedAccess.launch(target) {
                val occupied = worn.indices.filter { worn[it] != null }
                val slot = occupied.randomOrNull()
                if (slot != null && inv.hasFreeSpace()) {
                    wornUnequip(slot)
                }
            }
        }
        if (state.attacks % 6 == 0) {
            val x = random.of(-4..4)
            val z = random.of(-4..4)
            protectedAccess.launch(target) {
                telejump(CoordGrid(coords.x + x, coords.z + z, coords.level))
            }
        }
    }

    private fun StandardNpcAccess.archaeologist(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
        fanatic: Boolean,
    ) {
        val special = state.attacks % 4 == 0
        target.spotanim(if (special) WildernessBossSpots.bookSpecial else WildernessBossSpots.book)
        if (special) {
            npc.say(if (fanatic) "Burn!" else "Rain of knowledge!")
        }
        target.queueHit(
            npc,
            if (special) 3 else 2,
            HitType.Magic,
            damage(target, HitType.Magic, if (special) definition.maxHit else 16),
        )
    }

    private fun StandardNpcAccess.kingBlackDragon(
        target: Player,
        definition: WildernessBossDefinition,
        state: WildernessBossState,
    ) {
        if (isWithinDistance(target, 1) && state.attacks % 3 == 0) {
            melee(target, 25, WildernessBossSeqs.dragonMelee)
            return
        }
        anim(WildernessBossSeqs.dragonfire)
        val breath = state.attacks % 4
        val shields =
            setOf(
                WildernessBossObjs.antiDragonShield.id,
                WildernessBossObjs.dragonfireShield.id,
                WildernessBossObjs.dragonfireShieldUncharged.id,
                WildernessBossObjs.dragonfireWard.id,
            )
        val shielded = target.lefthand?.id in shields
        val antifire = target.vars[varbits.antifire_potion] > 0
        val superAntifire = target.vars[varbits.super_antifire_potion] > 0
        val maxHit =
            when {
                superAntifire && shielded -> 0
                superAntifire || (antifire && shielded) -> 10
                antifire || shielded -> 20
                else -> definition.maxHit
            }
        val hit = damage(target, HitType.Magic, maxHit)
        target.queueHit(npc, 2, HitType.Magic, hit)
        when (breath) {
            1 -> target.statSub(stats.prayer, constant = 5, percent = 0)
            2 -> {
                target.statSub(stats.attack, constant = 2, percent = 0)
                target.statSub(stats.strength, constant = 2, percent = 0)
            }
            3 -> target.delay = max(target.delay, mapClock + 2)
        }
    }

    private fun StandardNpcAccess.melee(
        target: Player,
        maxHit: Int,
        animation: org.rsmod.game.type.seq.SeqType,
    ) {
        if (!isWithinDistance(target, 1)) {
            return
        }
        anim(animation)
        target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, maxHit))
    }

    private fun StandardNpcAccess.damage(target: Player, type: HitType, maxHit: Int): Int {
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
