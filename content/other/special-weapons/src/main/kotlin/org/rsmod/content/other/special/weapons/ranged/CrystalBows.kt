package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType

class CrystalBows @Inject constructor(private val weapons: WeaponAttackManager) :
    WeaponMap, RangedWeapon {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (bow in CrystalBowItems.all) register(bow, this@CrystalBows)
    }

    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean =
        shoot(this, target, attack)

    override suspend fun ProtectedAccess.attack(
        target: Player,
        attack: CombatAttack.Ranged,
    ): Boolean = shoot(this, target, attack)

    private fun shoot(
        access: ProtectedAccess,
        target: PathingEntity,
        attack: CombatAttack.Ranged,
    ): Boolean =
        with(access) {
            val bow = attack.weapon
            val charges = CrystalBowItems.charges(bow)
            if (charges == 0) {
                mes("Your bow has run out of charges.")
                weapons.stopCombat(this)
                return true
            }
            val colour = CrystalBowItems.colour(bow)
            anim(seqs.human_bow)
            spotanim(
                CrystalBowSpots.launch[colour],
                height = 96,
                slot = constants.spotanim_slot_combat,
            )
            val arrow =
                weapons.spawnProjectile(
                    this,
                    target,
                    CrystalBowSpots.flight[colour],
                    projanims.arrow,
                )
            val damage = weapons.rollRangedDamage(this, target, attack)
            weapons.giveCombatXp(this, target, attack, damage)
            weapons.queueRangedHit(
                this,
                target,
                null,
                damage,
                arrow.clientCycles,
                arrow.serverCycles,
            )
            if (charges != Int.MAX_VALUE) {
                val regular = CrystalBowItems.regular(bow)
                val conserve =
                    regular &&
                        player.ring.isAnyType(
                            objs.elven_signet,
                            CrystalBowItems.celestial,
                            CrystalBowItems.celestialCharged,
                        ) &&
                        random.randomBoolean(10)
                val remaining = charges - if (conserve) 0 else 1
                val updated = CrystalBowItems.withCharges(bow, remaining)
                if (!regular && CrystalBowItems.usedCharges(updated) >= 200000) {
                    player.righthand = InvObj(objs.bow_of_faerdhinen_c_hefin)
                    mes("Your bow of Faerdhinen has become corrupted.")
                } else player.righthand = updated
            }
            weapons.continueCombat(this, target)
            true
        }
}

internal object CrystalBowItems : ObjReferences() {
    val inactive = find("crystal_bow_inactive")
    val newBow = find("crystal_bow_2500")
    val faerdhinenInactive = find("bow_of_faerdhinen_inactive")
    val seed = find("crystal_seed_old")
    val enhancedSeed = find("prif_weapon_seed_enhanced")
    val celestial = find("celestial_signet")
    val celestialCharged = find("celestial_signet_charged")
    val colours =
        listOf("infinite", "ithell", "iorwerth", "trahaearn", "cadarn", "crwys", "meilyr", "amlodd")
    val infinite =
        colours.map {
            find(
                if (it == "infinite") "bow_of_faerdhinen_infinite"
                else "bow_of_faerdhinen_infinite_$it"
            )
        }
    val finite
        get() =
            listOf(objs.crystal_bow, newBow, inactive, objs.bow_of_faerdhinen, faerdhinenInactive)

    val all
        get() = finite + infinite

    fun regular(bow: InvObj): Boolean = bow.isAnyType(objs.crystal_bow, newBow, inactive)

    fun colour(bow: InvObj): Int =
        if (regular(bow)) 0 else infinite.indexOfFirst { bow.isType(it) } + 2

    fun charges(bow: InvObj): Int =
        when {
            bow.isType(newBow) -> 2500
            bow.isAnyType(inactive, faerdhinenInactive) -> 0
            infinite.any { bow.isType(it) } -> Int.MAX_VALUE
            else -> (bow.vars and 32767).coerceAtMost(20000)
        }

    // Store total lifetime charge purchases in whole shards. Remaining charges retain shot
    // precision.
    fun paidShards(bow: InvObj): Int = maxOf(bow.vars ushr 15, (charges(bow) + 99) / 100)

    fun usedCharges(bow: InvObj): Int = (paidShards(bow) * 100 - charges(bow)).coerceAtLeast(0)

    fun withCharges(bow: InvObj, remaining: Int, addedShards: Int = 0): InvObj {
        val regular = regular(bow)
        val next: ObjType =
            if (regular) {
                if (remaining > 0) objs.crystal_bow else inactive
            } else {
                if (remaining > 0) objs.bow_of_faerdhinen else faerdhinenInactive
            }
        val contribution = if (regular) 0 else (paidShards(bow) + addedShards).coerceAtMost(2200)
        return InvObj(next, vars = (contribution shl 15) or remaining)
    }
}

internal object CrystalBowSpots : SpotanimReferences() {
    private val variants =
        listOf(
            "",
            "faerdhinen",
            "faerdhinen_infinite",
            "faerdhinen_ithell",
            "faerdhinen_iorwerth",
            "faerdhinen_trahaearn",
            "faerdhinen_cadarn",
            "faerdhinen_crwys",
            "faerdhinen",
            "faerdhinen_amlodd",
        )
    val flight =
        variants.map {
            find(if (it.isEmpty()) "sp_attack_glow_arrow_travel" else "sp_attack_arrow_travel_$it")
        }
    val launch =
        variants.map {
            find(if (it.isEmpty()) "sp_attack_glow_arrow_launch" else "sp_attack_arrow_launch_$it")
        }
}
