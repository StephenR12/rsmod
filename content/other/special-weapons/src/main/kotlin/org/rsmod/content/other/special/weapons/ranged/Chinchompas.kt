package org.rsmod.content.other.special.weapons.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.AreaAttackTargets
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.formulas.maxhit.ranged.PvNRangedMaxHit
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList

class Chinchompas
@Inject
constructor(
    private val weapons: WeaponAttackManager,
    private val ammunition: RangedAmmoManager,
    private val targets: AreaAttackTargets,
    private val npcMax: PvNRangedMaxHit,
    private val objTypes: ObjTypeList,
) : WeaponMap, RangedWeapon {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (item in listOf(objs.chinchoma, objs.red_chinchoma, objs.black_chinchompa)) register(
            item,
            this@Chinchompas,
        )
    }

    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean =
        throwAt(this, target, attack)

    override suspend fun ProtectedAccess.attack(
        target: Player,
        attack: CombatAttack.Ranged,
    ): Boolean = throwAt(this, target, attack)

    private fun throwAt(
        access: ProtectedAccess,
        primary: PathingEntity,
        attack: CombatAttack.Ranged,
    ): Boolean =
        with(access) {
            val chin = objTypes[attack.weapon]
            anim(if (primary is Npc) ChinAnimations.npc else ChinAnimations.player)
            val projectile =
                weapons.spawnProjectile(
                    this,
                    primary,
                    chin.param(params.proj_travel),
                    projanims.chinchompa,
                )
            val victims = targets.targets(player, primary, true, mapMultiway())
            val accurate =
                weapons.rollRangedAccuracy(
                    this,
                    primary,
                    attack.type,
                    attack.style,
                    RangedAttackType.Heavy,
                    1.0,
                )
            val primaryMax =
                if (accurate)
                    weapons.calculateRangedMaxHit(this, primary, attack.type, attack.style, 1.0, 0)
                else 0
            val splashMax =
                if (accurate && victims.size > 1 && primary is Npc)
                    npcMax.chinchompaAreaMaxHit(player, primary, attack.type, attack.style)
                else primaryMax
            for (target in victims) {
                val maximum = if (target === primary) primaryMax else splashMax
                val damage = if (maximum > 0) random.of(1..maximum) else 0
                weapons.giveCombatXp(this, target, attack, damage)
                weapons.queueRangedHit(
                    this,
                    target,
                    chin,
                    damage,
                    projectile.clientCycles,
                    projectile.serverCycles,
                )
            }
            ammunition.useThrownWeapon(player, chin, primary.coords, projectile.serverCycles)
            weapons.continueCombat(this, primary)
            true
        }
}

internal object ChinAnimations : SeqReferences() {
    val npc = find("human_chinchompa_attack_pvn")
    val player = find("human_chinchompa_attack")
}
