package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.usedTomeOfWater
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.config.stat_drain_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.synth.SynthType

class StatDrainSpells @Inject constructor(private val objTypes: ObjTypeList) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(
            objs.spell_confuse,
            StatDrainSpellAttack(
                manager,
                objTypes,
                seqs.human_castconfuse_staff,
                seqs.human_castconfuse,
                spotanims.confuse_casting,
                spotanims.confuse_travel,
                spotanims.confuse_impact,
                synths.confuse_cast_and_fire,
                synths.confuse_hit,
            ),
        )
        register(
            objs.spell_weaken,
            StatDrainSpellAttack(
                manager,
                objTypes,
                seqs.human_castweaken_staff,
                seqs.human_castweaken,
                spotanims.weaken_casting,
                spotanims.weaken_travel,
                spotanims.weaken_impact,
                synths.weaken_all,
                hitSound = null,
            ),
        )
        register(
            objs.spell_curse,
            StatDrainSpellAttack(
                manager,
                objTypes,
                seqs.human_castcurse_staff,
                seqs.human_castcurse,
                spotanims.curse_casting,
                spotanims.curse_travel,
                spotanims.curse_impact,
                synths.curse_cast_and_fire,
                synths.curse_hit,
            ),
        )
        register(
            objs.spell_vulnerability,
            StatDrainSpellAttack(
                manager,
                objTypes,
                // Cache 233 has no separate human vulnerability sequence; the official spell
                // animation uses the Curse casting motion with Vulnerability's own spotanims.
                seqs.human_castcurse_staff,
                seqs.human_castcurse,
                spotanims.vulnerability_casting,
                spotanims.vulnerability_travel,
                spotanims.vulnerability_impact,
                synths.vulnerability_all,
                hitSound = null,
            ),
        )
        register(
            objs.spell_enfeeble,
            StatDrainSpellAttack(
                manager,
                objTypes,
                seqs.human_castenfeeble_staff,
                seqs.human_castenfeeble,
                spotanims.enfeeble_casting,
                spotanims.enfeeble_travel,
                spotanims.enfeeble_impact,
                synths.enfeeble_cast_and_fire,
                synths.enfeeble_hit,
            ),
        )
        register(
            objs.spell_stun,
            StatDrainSpellAttack(
                manager,
                objTypes,
                seqs.human_caststun_staff,
                seqs.human_caststun,
                spotanims.stun_casting,
                spotanims.stun_travel,
                spotanims.stun_impact,
                synths.stun_all,
                hitSound = null,
            ),
        )
    }

    private class StatDrainSpellAttack(
        private val manager: SpellAttackManager,
        private val objTypes: ObjTypeList,
        private val staffAnim: SeqType,
        private val unarmedAnim: SeqType,
        private val launch: SpotanimType,
        private val travel: SpotanimType,
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
            val spellType = objTypes[attack.spell.obj]
            val drainStat = spellType.param(params.spell_drain_stat)
            val drainPercent = spellType.param(params.spell_drain_stat_amount)

            if (!target.canApplyStatDrain(drainStat)) {
                manager.stopCombat(this)
                mes(drainStat.alreadyDrainedMessage())
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            val weaponType = objTypes.getOrNull(attack.weapon)
            anim(weaponType.castAnim())
            spotanim(launch, height = 92)

            val projectile = manager.spawnProjectile(this, target, travel, projanims.magic_spell)
            val (serverDelay, clientDelay) = projectile.durations
            val spell = attack.spell.obj

            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.stopCombat(this)
                return
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
            manager.queueEffectRetaliation(this, target, serverDelay)
            val effectivePercent =
                if (castResult.usedTomeOfWater()) drainPercent * 3 / 2 else drainPercent
            target.queueImpact(StatDrainImpact(drainStat, effectivePercent), serverDelay)
            manager.stopCombat(this)
        }

        private fun UnpackedObjType?.castAnim(): SeqType =
            if (this != null && isCategoryType(categories.staff)) staffAnim else unarmedAnim
    }

    private companion object {
        fun PathingEntity.canApplyStatDrain(stat: StatType): Boolean =
            when (this) {
                is Npc -> canApplyNpcStatDrain(stat)
                is Player -> canApplyPlayerStatDrain(stat)
            }

        fun PathingEntity.queueImpact(impact: StatDrainImpact, delay: Int) {
            when (this) {
                is Npc -> queue(stat_drain_queues.impact, delay, impact)
                is Player -> strongQueue(stat_drain_queues.impact, delay, impact)
            }
        }

        fun StatType.alreadyDrainedMessage(): String =
            when {
                isType(stats.attack) -> "Your foe's attack has already been weakened."
                isType(stats.strength) -> "Your foe's strength has already been weakened."
                isType(stats.defence) -> "Your foe's defence has already been weakened."
                else -> error("Unsupported stat-drain type: $this")
            }
    }
}
