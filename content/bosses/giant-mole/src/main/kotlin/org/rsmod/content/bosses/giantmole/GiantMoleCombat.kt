package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

public class GiantMoleCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
) {
    fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock || !isWithinDistance(target, 1)) {
                return
            }
            actionDelay = mapClock + ATTACK_RATE
            anim(GiantMoleSeqs.attack)
            val accurate = accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
            val damage = if (accurate) random.of(0..MAX_HIT) else 0
            target.queueHit(npc, delay = 1, HitType.Melee, damage)
        }
    }

    fun considerBurrow(npc: Npc, hit: Hit) {
        val canBurrow =
            hit.isFromPlayer &&
                hit.damage > 0 &&
                npc.hitpoints > 0 &&
                npc.hitpoints < npc.baseHitpointsLvl / 2 &&
                queues.death !in npc.queueList &&
                queues.generic_queue1 !in npc.queueList
        if (canBurrow && random.of(BURROW_CHANCE_DENOMINATOR) == 0) {
            npc.queue(queues.generic_queue1, 1)
        }
    }

    suspend fun burrow(access: StandardNpcAccess) {
        with(access) {
            if (npc.hitpoints <= 0 || queues.death in npc.queueList) {
                return
            }
            actionDelay = mapClock + BURROW_LOCKOUT
            noneMode()
            anim(GiantMoleSeqs.burrowDown)
            delay(BURROW_ANIM_TICKS)
            telejump(burrowDestination(coords), collision)
            anim(GiantMoleSeqs.burrowUp)
            delay(BURROW_ANIM_TICKS)
            npc.mode = NpcMode.Wander
        }
    }

    fun burrowDestination(current: CoordGrid): CoordGrid {
        val destinations = GiantMoleLair.burrowDestinations.filterNot { it == current }
        return random.pick(destinations)
    }

    companion object {
        const val MAX_HIT = 21
        const val ATTACK_RATE = 6
        const val BURROW_CHANCE_DENOMINATOR = 4
        const val BURROW_ANIM_TICKS = 2
        const val BURROW_LOCKOUT = BURROW_ANIM_TICKS * 2
    }
}
