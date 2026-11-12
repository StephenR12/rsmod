package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.slayerLvl
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

public class MagicDartSpells @Inject constructor() : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(objs.spell_magic_dart, MagicDartSpellAttack(manager))
    }

    private class MagicDartSpellAttack(private val manager: SpellAttackManager) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private suspend fun ProtectedAccess.cast(
            target: PathingEntity,
            attack: CombatAttack.Spell,
        ) {
            if (player.slayerLvl < REQUIRED_SLAYER_LEVEL) {
                manager.stopCombat(this)
                mes("You need a Slayer level of 55 to cast this spell.")
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            anim(seqs.slayer_magicdart_cast)
            val splash = manager.rollSplash(this, target, attack, castResult)
            val enchantedHit = attack.weapon.isType(objs.slayers_staff_e) && !splash
            val travel =
                if (enchantedHit) {
                    spotanims.slayer_magicdart_enchanted_travel
                } else {
                    spotanims.slayer_magicdart_travel
                }
            val impact =
                if (enchantedHit) {
                    spotanims.slayer_magicdart_enchanted_impact
                } else {
                    spotanims.slayer_magicdart_impact
                }
            val projectile = manager.spawnProjectile(this, target, travel, projanims.magic_spell)
            val (serverDelay, clientDelay) = projectile.durations
            val spell = attack.spell.obj

            if (splash) {
                manager.playSplashFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = null,
                    soundRadius = 8,
                )
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, BASE_MAX_HIT)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = null,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 124,
                hitSound = synths.magic_dart_hit,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    private companion object {
        const val REQUIRED_SLAYER_LEVEL: Int = 55
        // Magic Dart's formula replaces this value with 10 + floor(current Magic / 10).
        const val BASE_MAX_HIT: Int = 0
    }
}
