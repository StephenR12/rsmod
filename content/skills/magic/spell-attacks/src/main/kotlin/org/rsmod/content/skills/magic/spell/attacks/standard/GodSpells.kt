package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MageArenaTraining
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.config.stat_drain_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.stat.StatType

public class GodSpells @Inject constructor() : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(
            objs.spell_saradomin_strike,
            GodSpellAttack(manager, spotanims.saradomin_lightning, GodEffect.Saradomin),
        )
        register(
            objs.spell_claws_of_guthix,
            GodSpellAttack(manager, spotanims.guthix_claw, GodEffect.Guthix),
        )
        register(
            objs.spell_flames_of_zamorak,
            GodSpellAttack(manager, spotanims.zamorak_flame, GodEffect.Zamorak),
        )
    }

    private class GodSpellAttack(
        private val manager: SpellAttackManager,
        private val impact: SpotanimType,
        private val effect: GodEffect,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            val spell = attack.spell.obj
            if (vars[varps.magearena] < MageArenaTraining.CLAIMED_STAFF) {
                mes("You must defeat Kolodion and claim a god staff first.")
                return
            }
            val counter = MageArenaTraining.counter(spell)
            val casts = vars[counter]
            val training = casts < MageArenaTraining.REQUIRED_CASTS
            if (training && !MageArenaTraining.isPractice(coords, target)) {
                mes("You must practise this spell on the battle mages in the Mage Arena first.")
                return
            }
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            if (training) {
                vars[counter] = casts + 1
                if (casts + 1 == MageArenaTraining.REQUIRED_CASTS)
                    mes("You can now cast this spell outside the Mage Arena.")
            }
            anim(seqs.human_casting)
            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, CLIENT_DELAY, castSound = null, soundRadius = 8)
                manager.queueSplashHit(this, target, spell, CLIENT_DELAY, SERVER_DELAY)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, BASE_MAX_HIT)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = CLIENT_DELAY,
                castSound = null,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 0,
                hitSound = null,
            )
            manager.giveCombatXp(this, target, attack, damage)
            if (
                target is Player &&
                    vars[varbits.inside_wilderness] != 0 &&
                    target.vars[varbits.inside_wilderness] != 0
            ) {
                player.completeDiaryTask(DiaryTask.CastWildernessGodSpell)
            }
            manager.queueMagicHit(this, target, spell, damage, CLIENT_DELAY, SERVER_DELAY)
            target.queueGodEffect(effect, SERVER_DELAY)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    private sealed interface GodEffect {
        data object Saradomin : GodEffect

        data object Guthix : GodEffect

        data object Zamorak : GodEffect
    }

    private companion object {
        const val BASE_MAX_HIT: Int = 20
        const val SERVER_DELAY: Int = 1
        const val CLIENT_DELAY: Int = 0

        fun PathingEntity.queueGodEffect(effect: GodEffect, delay: Int) {
            when (effect) {
                GodEffect.Saradomin -> {
                    if (this is Player) {
                        strongQueue(
                            stat_drain_queues.impact,
                            delay,
                            StatDrainImpact(stats.prayer, percent = 0, constant = 1),
                        )
                    }
                }
                GodEffect.Guthix ->
                    queueStatDrain(stats.defence, constant = 1, percent = 5, delay = delay)
                GodEffect.Zamorak ->
                    queueStatDrain(stats.magic, constant = 1, percent = 5, delay = delay)
            }
        }

        fun PathingEntity.queueStatDrain(stat: StatType, constant: Int, percent: Int, delay: Int) {
            val impact =
                StatDrainImpact(
                    stat = stat,
                    constant = constant,
                    percent = percent,
                    onlyWhenRestored = true,
                )
            when (this) {
                is Npc -> queue(stat_drain_queues.impact, delay, impact)
                is Player -> strongQueue(stat_drain_queues.impact, delay, impact)
            }
        }
    }
}
