package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import java.util.EnumSet
import kotlin.math.abs
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.npc.*
import org.rsmod.api.combat.commons.styles.*
import org.rsmod.api.combat.commons.types.*
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.combat.formulas.accuracy.melee.PvNMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.PvNRangedAccuracy
import org.rsmod.api.combat.formulas.attributes.*
import org.rsmod.api.combat.formulas.attributes.collector.CombatRangedAttributeCollector
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.*
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.mapMultiway
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.WeaponCategory
import org.rsmod.game.type.proj.ProjAnimTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

class CannonCombat
@Inject
constructor(
    private val hunt: Hunt,
    private val areas: AreaChecker,
    private val modifier: NpcHitModifier,
    private val random: GameRandom,
    private val melee: PvNMeleeAccuracy,
    private val ranged: PvNRangedAccuracy,
    private val rangedBonuses: CombatRangedAttributeCollector,
    private val styles: AttackStyles,
    private val types: AttackTypes,
    private val objs: ObjTypeList,
    private val projs: ProjAnimTypeList,
    private val world: WorldRepository,
) {
    private val Player.attacker by typeNpcUidVarp(varps.aggressive_npc)
    private val Npc.opponent by typePlayerUidVarn(varns.aggressive_player)

    fun shoot(player: Player, centre: CoordGrid, direction: Int, granite: Boolean): Boolean {
        if (!player.isWithinDistance(centre, 16)) return false
        val npc =
            hunt.findNpcs(centre, 8, HuntVis.LineOfSight).firstOrNull {
                covers(direction, it.coords.x - centre.x, it.coords.z - centre.z) &&
                    canAttack(player, it)
            } ?: return false
        val ball = if (granite) CannonObjs.granite else CannonObjs.ball
        val projectile = if (granite) CannonSpots.granite else CannonSpots.ball
        val hit = random.of(10000) < hitChance(player, npc)
        val damage = if (hit) random.of(1..if (granite) 35 else 30) else 0
        world.projAnim(
            ProjAnim.fromBoundsToNpc(Bounds(centre, 1), npc, projectile.id, projs[projanims.bolt])
        )
        npc.queueHit(
            player,
            1,
            HitType.Ranged,
            damage,
            modifier,
            sourceSecondary = ball,
            successful = hit,
        )
        npc.queueCombatRetaliate(player)
        return true
    }

    private fun canAttack(player: Player, npc: Npc): Boolean {
        if (!npc.isValidTarget() || npc.visType.op.none { it.equals("Attack", ignoreCase = true) })
            return false
        if (npc.visType.param(params.cannon_immunity) != 0 || LeafBladed.required(npc.visType))
            return false
        if (player.stat(stats.slayer) < npc.visType.param(params.slayer_levelrequire)) return false
        val owner = npc.slayerSuperiorOwner
        if (owner != null && owner != player.uid) return false
        if (
            owner == null &&
                npc.requiresSlayerAssignment(player) &&
                !npc.isAssignedSlayerTarget(player)
        )
            return false
        if (npc.mapMultiway(areas)) return true
        if (
            player.isInPvpCombat() ||
                (player.isInPvnCombat() && player.attacker != null && player.attacker != npc.uid)
        )
            return false
        return npc.vars[varns.lastcombat] + constants.combat_activecombat_delay <=
            npc.currentMapClock || npc.opponent == null || npc.opponent == player.uid
    }

    private fun hitChance(player: Player, npc: Npc): Int {
        val type = types.get(player)
        val style = styles.get(player)
        val attributes = EnumSet.noneOf(CombatNpcAttributes::class.java)
        fun meleeRoll() =
            melee.computeAttackRoll(
                player,
                MeleeAttackType.from(type) ?: MeleeAttackType.Crush,
                MeleeAttackStyle.from(style),
                EnumSet.noneOf(CombatMeleeAttributes::class.java),
                attributes,
            )
        fun rangedRoll(): Int {
            val rangedStyle = RangedAttackStyle.from(style)
            val equipment = rangedBonuses.collect(player, RangedAttackType.from(type), rangedStyle)
            equipment.retainAll(crystal)
            return ranged.computeAttackRoll(
                player,
                npc.magicLvl,
                player.coords.chebyshevDistance(npc.coords),
                rangedStyle,
                equipment,
                attributes,
            )
        }
        val roll =
            when {
                type?.isRanged == true -> rangedRoll()
                objs.getOrNull(player.righthand)?.weaponCategory ==
                    WeaponCategory.PoweredStaff.id -> maxOf(meleeRoll(), rangedRoll())
                else -> meleeRoll()
            }
        val defence =
            ranged.computeDefenceRoll(
                npc.visType,
                npc.defenceLvl,
                0,
                RangedAttackType.Heavy,
                attributes,
            )
        return AccuracyOperations.calculateHitChance(roll, defence)
    }

    companion object {
        private val crystal =
            setOf(
                CombatRangedAttributes.CrystalBow,
                CombatRangedAttributes.CrystalHelm,
                CombatRangedAttributes.CrystalBody,
                CombatRangedAttributes.CrystalLegs,
            )

        /** Revolves clockwise from north. Adjacent corner-edge tiles can be hit by two sectors. */
        fun covers(direction: Int, dx: Int, dz: Int): Boolean {
            val quarter = direction / 2
            val x =
                when (quarter) {
                    0 -> dx
                    1 -> -dz
                    2 -> -dx
                    else -> dz
                }
            val z =
                when (quarter) {
                    0 -> dz
                    1 -> dx
                    2 -> -dz
                    else -> -dx
                }
            if (abs(x) > 8 || abs(z) > 8 || (abs(x) == 1 && abs(z) == 1)) return false
            return if (direction % 2 == 0) z > 0 && abs(x) <= (z + 1) / 2
            else x > 0 && z > 0 && minOf(x, z) * 2 >= maxOf(x, z)
        }
    }
}
