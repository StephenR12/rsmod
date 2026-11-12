package org.rsmod.content.bosses.magearena

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

public class MageArenaCombat
@Inject
constructor(
    private val controller: MageArenaController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val protectedAccess: ProtectedAccessLauncher,
) {
    public fun kolodionAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            actionDelay = mapClock + 7
            target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 20))
        }
    }

    public fun followerAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            actionDelay = mapClock + 6
            val state = controller.state(npc)
            state.attacks++
            when (npc.id) {
                MageArenaNpcs.zachariah.id -> zachariah(target, state)
                MageArenaNpcs.derwen.id -> derwen(target, state)
                MageArenaNpcs.porazdir.id -> porazdir(target, state)
            }
        }
    }

    public fun energyTick(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            actionDelay = mapClock + 4
            controller.healDerwen(npc)
            if (isWithinDistance(target, 1)) {
                target.queueHit(npc, 1, HitType.Typeless, 2)
            }
        }
    }

    private fun StandardNpcAccess.zachariah(target: Player, state: MageArenaState) {
        if (state.attacks % 5 == 0) {
            protectedAccess.launch(target) {
                val destination = CoordGrid(npc.coords.x + 1, npc.coords.z, npc.coords.level)
                telejump(destination)
                delay(3)
            }
            target.queueHit(npc, 2, HitType.Typeless, random.of(0..26))
            return
        }
        if (isWithinDistance(target, 1)) {
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, 43))
        } else {
            target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 26))
        }
    }

    private fun StandardNpcAccess.derwen(target: Player, state: MageArenaState) {
        if (state.attacks % 6 == 0) {
            controller.summonDerwenEnergy(npc)
        }
        if (isWithinDistance(target, 1)) {
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, 16))
        } else {
            target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 43))
        }
        if (state.attacks % 4 == 0) {
            target.delay = max(target.delay, mapClock + 2)
        }
    }

    private fun StandardNpcAccess.porazdir(target: Player, state: MageArenaState) {
        if (state.attacks % 5 == 0) {
            val distance =
                maxOf(
                    kotlin.math.abs(coords.x - target.coords.x),
                    kotlin.math.abs(coords.z - target.coords.z),
                )
            val maxHit = (30 - distance * 3).coerceIn(0, 30)
            target.queueHit(npc, 2, HitType.Typeless, random.of(0..maxHit))
            return
        }
        if (isWithinDistance(target, 1)) {
            target.queueHit(npc, 1, HitType.Melee, damage(target, HitType.Melee, 16))
        } else {
            target.queueHit(npc, 2, HitType.Magic, damage(target, HitType.Magic, 43))
        }
    }

    private fun StandardNpcAccess.damage(target: Player, type: HitType, maxHit: Int): Int {
        val accurate =
            when (type) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Slash, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                else -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }
}
