package org.rsmod.content.other.special.weapons.magic

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.weapons.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.varbit.VarBitType

internal object StaffSeqs : SeqReferences() {
    val warped = find("pog_warped_sceptre_attack")
}

internal object StaffSpots : SpotanimReferences() {
    val warpedCast = find("vfx_warped_sceptre_cast")
    val warpedTravel = find("vfx_warped_sceptre_projectile_projectile")
    val warpedImpact = find("vfx_warped_sceptre_projectile_impact")
    val sangCast = find("sanguinesti_staff_casting")
    val sangTravel = find("sanguinesti_staff_travel")
    val sangImpact = find("sanguinesti_staff_impact")
    val sangHeal = find("sanguinesti_staff_heal")
    val holyCast = find("sanguinesti_staff_casting_justiciar")
    val holyTravel = find("sanguinesti_staff_travel_justiciar")
    val holyImpact = find("sanguinesti_staff_impact_justiciar")
    val holyHeal = find("sanguinesti_staff_heal_justiciar")
}

internal data class StaffEffects(
    val seq: SeqType,
    val cast: SpotanimType,
    val travel: SpotanimType,
    val impact: SpotanimType,
) {
    companion object {
        val seas
            get() =
                StaffEffects(
                    seqs.human_castwave_staff,
                    TridentEffects.casting,
                    TridentEffects.travel,
                    TridentEffects.splash,
                )

        val swamp
            get() =
                StaffEffects(
                    seqs.human_castwave_staff,
                    TridentEffects.toxicCasting,
                    TridentEffects.toxicTravel,
                    TridentEffects.toxicSplash,
                )
    }
}

internal object StaffBits : VarBitReferences() {
    val warped = find("charges_warped_sceptre_quantity")
    val sang = find("charges_sanguinesti_staff_quantity")
    val shadow = find("charges_tumekens_shadow_quantity")
}

internal object OtherStaves : ObjReferences() {
    val warped =
        Staff(
            find("warped_sceptre"),
            find("warped_sceptre_uncharged"),
            62,
            StaffBits.warped,
            listOf(objs.chaos_rune to 2, objs.earth_rune to 5),
            StaffEffects(
                StaffSeqs.warped,
                StaffSpots.warpedCast,
                StaffSpots.warpedTravel,
                StaffSpots.warpedImpact,
            ),
        )
    val sang =
        Staff(
            find("sanguinesti_staff"),
            find("sanguinesti_staff_uncharged"),
            82,
            StaffBits.sang,
            listOf(objs.blood_rune to 3),
            StaffEffects(
                seqs.human_castwave_staff,
                StaffSpots.sangCast,
                StaffSpots.sangTravel,
                StaffSpots.sangImpact,
            ),
        )
    val holy =
        Staff(
            find("sanguinesti_staff_or"),
            find("sanguinesti_staff_uncharged_or"),
            82,
            StaffBits.sang,
            listOf(objs.blood_rune to 3),
            StaffEffects(
                seqs.human_castwave_staff,
                StaffSpots.holyCast,
                StaffSpots.holyTravel,
                StaffSpots.holyImpact,
            ),
        )
    val all = listOf(warped, sang, holy)

    fun type(id: Int) = all.firstOrNull { id == it.active.id || id == it.empty.id }
}

internal data class Staff(
    val active: ObjType,
    val empty: ObjType,
    val level: Int,
    val target: VarBitType,
    val materials: List<Pair<ObjType, Int>>,
    val effects: StaffEffects,
) {
    fun charges(item: InvObj) = if (item.id == active.id) item.vars.coerceIn(0, 20000) else 0

    fun item(count: Int) =
        if (count > 0) InvObj(active, vars = count.coerceAtMost(20000)) else InvObj(empty)

    fun maxHit(magic: Int) =
        if (this == OtherStaves.warped) (8 * magic + 96) / 37 else (magic / 3 - 1).coerceAtLeast(1)
}

internal fun castPoweredStaff(
    access: ProtectedAccess,
    manager: WeaponAttackManager,
    target: PathingEntity,
    attack: CombatAttack.Staff,
    max: Int,
    effects: StaffEffects,
) =
    with(access) {
        manager.setNextAttackDelay(this, 4)
        anim(effects.seq)
        spotanim(effects.cast)
        val projectile =
            manager.spawnProjectile(this, target, effects.travel, projanims.magic_spell)
        val (server, client) = projectile.durations
        val sound = synths.waterwave_cast_and_fire
        if (manager.rollStaffSplash(this, target, attack)) {
            manager.playSplashFx(this, target, client, sound, soundRadius = 10)
            manager.queueSplashHit(this, target, client, server)
        } else {
            val damage = manager.rollStaffMaxHit(this, target, max)
            manager.playMagicHitFx(
                this,
                target,
                client,
                sound,
                10,
                effects.impact,
                124,
                synths.waterwave_hit,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, client, server)
        }
    }

class OtherPoweredStaves @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val weapon = Attack(manager)
        for (staff in OtherStaves.all) {
            register(staff.active, weapon)
            register(staff.empty, weapon)
        }
    }

    private class Attack(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            val permit = PlayerCombatPermission(player, target)
            publish(permit)
            if (permit.allowed == true) cast(target, attack)
            else {
                manager.stopCombat(this)
                mes("You cannot use this powered spell against that player.")
            }
            return true
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff) {
            val held = player.righthand ?: return
            val staff = OtherStaves.type(held.id) ?: return
            val count = staff.charges(held)
            if (count == 0) {
                mes("Your staff is uncharged.")
                manager.stopCombat(this)
                return
            }
            castPoweredStaff(
                this,
                manager,
                target,
                attack,
                staff.maxHit(player.magicLvl),
                staff.effects,
            )
            player.righthand = staff.item(count - 1)
            if (count == 1) {
                mes("Your staff has run out of charges.")
                manager.stopCombat(this)
            } else manager.continueCombat(this, target)
        }
    }
}
