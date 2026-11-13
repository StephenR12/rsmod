package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.hat
import org.rsmod.api.player.hunter.isImplingCreature
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.torso
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.config.binding_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

public class BindingSpells
@Inject
constructor(private val objTypes: ObjTypeList, private val states: BindingStateRepository) :
    SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(
            objs.spell_bind,
            BindingSpellAttack(
                manager,
                objTypes,
                states,
                duration = 8,
                damageMaxHit = 0,
                successXpBonus = 1.0,
                impact = spotanims.bind_impact,
                castSound = synths.bind_cast,
                hitSound = synths.bind_impact,
            ),
        )
        register(
            objs.spell_snare,
            BindingSpellAttack(
                manager,
                objTypes,
                states,
                duration = 16,
                damageMaxHit = 2,
                successXpBonus = 1.0,
                impact = spotanims.snare_impact,
                castSound = synths.snare_all,
                hitSound = null,
            ),
        )
        register(
            objs.spell_entangle,
            BindingSpellAttack(
                manager,
                objTypes,
                states,
                duration = 24,
                damageMaxHit = 5,
                successXpBonus = 19.0,
                impact = spotanims.entangle_impact,
                castSound = synths.entangle_cast_and_fire,
                hitSound = synths.entangle_hit,
            ),
        )
    }

    private class BindingSpellAttack(
        private val manager: SpellAttackManager,
        private val objTypes: ObjTypeList,
        private val states: BindingStateRepository,
        private val duration: Int,
        private val damageMaxHit: Int,
        private val successXpBonus: Double,
        private val impact: SpotanimType,
        private val castSound: SynthType,
        private val hitSound: SynthType?,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            when (states.status(target)) {
                BindingStatus.Held -> {
                    manager.stopCombat(this)
                    mes("Your target is already held by a magical force.")
                    return
                }
                BindingStatus.Immune -> {
                    manager.stopCombat(this)
                    mes("The target is currently immune to that spell.")
                    return
                }
                BindingStatus.Available -> Unit
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            val weaponType = objTypes.getOrNull(attack.weapon)
            anim(weaponType.castAnim())
            spotanim(spotanims.entangle_casting, height = 92)

            val projectile =
                manager.spawnProjectile(this, target, spotanims.entangle_travel, projanims.bind)
            val (serverDelay, clientDelay) = projectile.durations
            val spell = attack.spell.obj

            val impling = target is Npc && target.isImplingCreature()
            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
                if (!impling) manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.stopCombat(this)
                return
            }

            statAdvance(stats.magic, successXpBonus)
            val damage =
                if (damageMaxHit > 0 && !impling) {
                    manager.rollMaxHit(this, target, attack, castResult, damageMaxHit)
                } else {
                    0
                }
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = castSound,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 124,
                hitSound = hitSound,
            )
            if (damageMaxHit > 0 && !impling) {
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            } else if (!impling) {
                manager.queueEffectRetaliation(this, target, serverDelay)
            }
            val effectiveDuration = duration + player.swampbarkDurationBonus()
            target.queueBindingImpact(BindingImpact(player, effectiveDuration), serverDelay)
            manager.stopCombat(this)
        }

        private fun UnpackedObjType?.castAnim(): SeqType =
            if (this != null && isCategoryType(categories.staff)) {
                seqs.human_castentangle_staff
            } else {
                seqs.human_castentangle
            }
    }

    private companion object {
        fun Player.swampbarkDurationBonus(): Int =
            listOf(hat, torso, legs).count { worn ->
                worn.isType(objs.swampbark_helm) ||
                    worn.isType(objs.swampbark_body) ||
                    worn.isType(objs.swampbark_legs)
            } * 2

        fun PathingEntity.queueBindingImpact(impact: BindingImpact, delay: Int) {
            when (this) {
                is Npc -> queue(binding_queues.impact, delay, impact)
                is Player -> strongQueue(binding_queues.impact, delay, impact)
            }
        }
    }
}
