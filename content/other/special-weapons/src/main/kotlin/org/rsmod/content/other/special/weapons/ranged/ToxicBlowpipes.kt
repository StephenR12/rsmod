package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.content.skills.fletching.toxicblowpipe.ToxicBlowpipeState
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjTypeList

class ToxicBlowpipes
@Inject
constructor(
    private val weapons: WeaponAttackManager,
    private val ammunition: RangedAmmoManager,
    private val objTypes: ObjTypeList,
) : WeaponMap, SpecialAttackMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val ordinary = Shot(false)
        for (pipe in
            listOf(
                objs.toxic_blowpipe,
                objs.blazing_blowpipe,
                objs.toxic_blowpipe_empty,
                objs.blazing_blowpipe_empty,
            )) {
            register(pipe, ordinary)
        }
    }

    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val siphon = Shot(true)
        registerRanged(objs.toxic_blowpipe, siphon)
        registerRanged(objs.blazing_blowpipe, siphon)
    }

    private inner class Shot(private val special: Boolean) : RangedWeapon, RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(this, target, attack, special) || !special

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(this, target, attack, special) || !special
    }

    fun fire(
        access: ProtectedAccess,
        target: PathingEntity,
        attack: CombatAttack.Ranged,
        special: Boolean,
    ): Boolean =
        with(access) {
            val pipe = player.righthand
            if (pipe == null || pipe != attack.weapon) return false
            val contents = ToxicBlowpipeState.unpack(pipe)
            val dart = contents.dart
            if (dart == null || contents.scales == 0) {
                weapons.stopCombat(this)
                mes(
                    if (dart == null) "Your blowpipe is out of darts."
                    else "Your blowpipe has run out of scales."
                )
                return false
            }
            val blazing = pipe.isType(objs.blazing_blowpipe)
            val dartType = objTypes[dart.obj]
            val flight =
                when {
                    special && blazing -> BlowpipeSpots.blazingSiphon
                    special -> BlowpipeSpots.siphon
                    else -> dartType.param(params.proj_travel)
                }
            val animation =
                when {
                    special -> BlowpipeSeqs.siphon
                    blazing -> BlowpipeSeqs.blazing
                    else -> BlowpipeSeqs.fire
                }
            anim(animation)
            val projectile = weapons.spawnProjectile(this, target, flight, projanims.thrown)
            val damage =
                weapons.rollRangedDamage(
                    this,
                    target,
                    attack,
                    if (special) 2.0 else 1.0,
                    if (special) 1.5 else 1.0,
                )
            weapons.giveCombatXp(this, target, attack, damage)
            weapons.queueRangedHit(
                this,
                target,
                dart.obj,
                damage,
                projectile.clientCycles,
                projectile.serverCycles,
            )
            if (special && damage >= 2) statHeal(stats.hitpoints, damage / 2, 0)
            val consumed =
                ammunition.consumeStoredDart(
                    player,
                    dartType,
                    target.coords,
                    projectile.serverCycles,
                )
            val remainingDarts = contents.dartCount - if (consumed) 1 else 0
            val remainingScales = contents.scales - if (random.of(3) == 0) 0 else 1
            val next =
                ToxicBlowpipeState(
                    dart.takeIf { remainingDarts > 0 },
                    remainingDarts,
                    remainingScales,
                )
            val type =
                when {
                    !next.isEmpty -> objTypes[pipe]
                    blazing -> objs.blazing_blowpipe_empty
                    else -> objs.toxic_blowpipe_empty
                }
            if (contents != next) player.righthand = InvObj(type, vars = next.pack())
            val baseSpeed = if (target is Player) 4 else 3
            weapons.setNextAttackDelay(
                this,
                baseSpeed - if (attack.style == RangedAttackStyle.Rapid) 1 else 0,
            )
            weapons.continueCombat(this, target)
            true
        }
}

internal object BlowpipeSeqs : SeqReferences() {
    val fire = find("snakeboss_blowpipe_attack")
    val blazing = find("snakeboss_blowpipe_attack_ornament")
    val siphon = find("toxic_blowpipe_special_updated")
}

internal object BlowpipeSpots : SpotanimReferences() {
    val siphon = find("toxic_blowpipe_specialattack")
    val blazingSiphon = find("toxic_blowpipe_specialattack_league04")
}
