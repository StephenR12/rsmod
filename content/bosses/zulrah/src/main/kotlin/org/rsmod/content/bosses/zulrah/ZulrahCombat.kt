package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

public class ZulrahCombat
@Inject
constructor(
    private val controller: ZulrahController,
    private val accuracy: AccuracyFormulae,
    private val protectedAccess: ProtectedAccessLauncher,
    private val poison: PoisonProtectionRepository,
) {
    public fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (controller.ownerFor(npc) !== target) {
                resetMode()
                return
            }
            if (actionDelay > mapClock) {
                return
            }
            if (!controller.isBoss(npc)) {
                snakelingAttack(target)
                return
            }
            val phase = controller.phaseFor(npc) ?: return
            if (controller.isStandingInCloud(npc, target)) {
                target.queueHit(npc, 1, HitType.Typeless, random.of(4..10))
            }
            actionDelay = mapClock + ATTACK_RATE
            when {
                phase.jad -> jadAttack(target)
                phase.form == ZulrahForm.Ranged -> rangedAttack(target)
                phase.form == ZulrahForm.Magic -> magicPhaseAttack(target)
                else -> tailAttack(target)
            }
            controller.consumeBossAttack(npc)
        }
    }

    public suspend fun transition(access: StandardNpcAccess) {
        with(access) {
            if (!controller.isBoss(npc) || npc.hitpoints <= 0) {
                return
            }
            noneMode()
            hideAllOps()
            anim(ZulrahSeqs.sink)
            delay(TRANSITION_TICKS)
            controller.advance(npc)
        }
    }

    private fun StandardNpcAccess.rangedAttack(target: Player) {
        anim(ZulrahSeqs.attack)
        target.spotanim(ZulrahSpotanims.orb)
        val damage = roll(target, HitType.Ranged, 41)
        val hit = target.queueHit(npc, PROJECTILE_DELAY, HitType.Ranged, damage)
        envenom(target, hit.damage)
    }

    private fun StandardNpcAccess.magicPhaseAttack(target: Player) {
        // The turquoise form primarily uses magic but can throw an off-style ranged attack.
        if (random.of(3) == 0) {
            rangedAttack(target)
            return
        }
        anim(ZulrahSeqs.alternateAttack)
        target.spotanim(ZulrahSpotanims.fireball)
        val damage = roll(target, HitType.Magic, 41)
        val hit = target.queueHit(npc, PROJECTILE_DELAY, HitType.Magic, damage)
        envenom(target, hit.damage)
    }

    private fun StandardNpcAccess.jadAttack(target: Player) {
        if (controller.alternateJadStyle(npc)) {
            anim(ZulrahSeqs.alternateAttack)
            target.spotanim(ZulrahSpotanims.doubleOrb)
            val damage = roll(target, HitType.Magic, 41)
            val hit = target.queueHit(npc, PROJECTILE_DELAY, HitType.Magic, damage)
            envenom(target, hit.damage)
        } else {
            rangedAttack(target)
        }
    }

    private fun StandardNpcAccess.tailAttack(target: Player) {
        val access = this
        val threatenedTile = target.coords
        anim(if (random.of(2) == 0) ZulrahSeqs.tailLeft else ZulrahSeqs.tailRight)
        protectedAccess.launch(target) {
            delay(TAIL_DELAY)
            if (
                player.coords == threatenedTile &&
                    controller.ownerFor(npc) === player &&
                    npc.slotId != INVALID_SLOT
            ) {
                val damage = rollMelee(npc, player, access)
                val hit = player.queueHit(npc, 1, HitType.Melee, damage)
                envenom(player, hit.damage)
            }
        }
    }

    private fun StandardNpcAccess.snakelingAttack(target: Player) {
        actionDelay = mapClock + SNAKELING_RATE
        val magic = npc.id == ZulrahNpcs.magicSnakeling.id
        val type = if (magic) HitType.Magic else HitType.Melee
        if (magic) {
            target.spotanim(ZulrahSpotanims.snakelingSpell)
        }
        val damage = roll(target, type, SNAKELING_MAX_HIT)
        val hit = target.queueHit(npc, 1, type, damage)
        envenom(target, hit.damage)
    }

    /** Successful Zulrah and snakeling attacks apply the shared escalating venom state. */
    public fun envenom(target: Player, damage: Int): Boolean =
        damage > 0 && poison.inflictVenom(target)

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

    private fun rollMelee(npc: Npc, target: Player, access: StandardNpcAccess): Int {
        val accurate = accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, access.random)
        return if (accurate) access.random.of(0..41) else 0
    }

    public companion object {
        public const val ATTACK_RATE: Int = 6
        public const val PROJECTILE_DELAY: Int = 3
        public const val TRANSITION_TICKS: Int = 2
        public const val TAIL_DELAY: Int = 2
        public const val SNAKELING_RATE: Int = 4
        public const val SNAKELING_MAX_HIT: Int = 15
    }
}
