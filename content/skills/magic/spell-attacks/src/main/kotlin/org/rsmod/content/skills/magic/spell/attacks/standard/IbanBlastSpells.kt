package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

@Singleton
public class IbanStaffChargeRepository @Inject constructor() {
    private var Player.charges by intVarp(varps.iban_charges)

    public fun get(player: Player): Int = player.charges

    public fun set(player: Player, charges: Int) {
        require(charges in 0..UPGRADED_MAX_CHARGES)
        player.charges = charges
    }

    public fun has(player: Player, count: Int): Boolean = get(player) >= count

    public fun consume(player: Player, count: Int) {
        require(has(player, count))
        player.charges -= count
    }

    public companion object {
        public const val REGULAR_MAX_CHARGES: Int = 120
        public const val UPGRADED_MAX_CHARGES: Int = 2_500
    }
}

public class IbanBlastSpells
@Inject
constructor(private val runes: MagicRuneManager, private val charges: IbanStaffChargeRepository) :
    SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(objs.spell_iban_blast, IbanBlastSpellAttack(manager))
    }

    private inner class IbanBlastSpellAttack(private val manager: SpellAttackManager) :
        SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack, chargeCost = NPC_CHARGE_COST)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack, chargeCost = PLAYER_CHARGE_COST)
        }

        private suspend fun ProtectedAccess.cast(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            chargeCost: Int,
        ) {
            // Validate the staff and runes without consuming them so the shared staff charge can be
            // rejected atomically before the normal cast transaction awards XP or removes runes.
            if (vars[IbanQuest.progress] < 11) {
                manager.stopCombat(this)
                mes("You must complete Underground Pass before casting Iban Blast.")
                return
            }
            if (!runes.canCastSpell(player, attack.spell)) {
                manager.stopCombat(this)
                return
            }
            if (!charges.has(player, chargeCost)) {
                manager.stopCombat(this)
                val message =
                    if (charges.get(player) == 0) {
                        "There are no charges left on this staff."
                    } else {
                        "There are not enough charges left on this staff."
                    }
                mes(message)
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }
            charges.consume(player, chargeCost)
            if (charges.get(player) in 1..10)
                mes("Your staff only has ${charges.get(player)} charges left.")

            anim(seqs.human_castibanblast)
            spotanim(spotanims.ibanblast_casting, height = 92)
            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    spotanims.ibanblast_travel,
                    projanims.iban_blast,
                )
            val (serverDelay, clientDelay) = projectile.durations
            val spell = attack.spell.obj
            val splash = manager.rollSplash(this, target, attack, castResult)
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
                hitSpot = spotanims.ibanblast_impact,
                hitSpotHeight = 124,
                hitSound = null,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }
    }

    private companion object {
        const val BASE_MAX_HIT: Int = 25
        const val NPC_CHARGE_COST: Int = 1
        const val PLAYER_CHARGE_COST: Int = 2
    }
}
