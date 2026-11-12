package org.rsmod.content.skills.magic.spell.attacks.ancient

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.feet
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.spell.attacks.ancient.config.ancient_queues
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStatus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

public class AncientSpells
@Inject
constructor(
    private val targets: AncientTargetSelector,
    private val random: GameRandom,
    private val bindings: BindingStateRepository,
) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (definition in AncientSpellDefinitions.all) {
            register(
                definition.spell,
                AncientSpellAttack(manager, targets, random, definition, bindings),
            )
        }
    }
}

internal class AncientSpellAttack(
    private val manager: SpellAttackManager,
    private val targets: AncientTargetSelector,
    private val random: GameRandom,
    private val definition: AncientSpellDefinition,
    private val bindings: BindingStateRepository,
) : SpellAttack {
    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
        cast(target, attack)
    }

    override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
        cast(target, attack)
    }

    private fun ProtectedAccess.cast(primary: PathingEntity, attack: CombatAttack.Spell) {
        if (!canAccessAncients()) {
            manager.stopCombat(this)
            return
        }

        val castResult = manager.attemptCast(this, attack)
        if (castResult.isFailure()) return

        anim(seqs.zaros_casting)
        val ancientSceptre = player.hasAncientSceptre()
        val familySceptre = player.hasFamilySceptre(definition.family)
        val bloodbarkPieces = player.bloodbarkPieces()
        val selected = targets.targets(primary, definition.multiTarget, mapMultiway())

        for ((index, target) in selected.withIndex()) {
            val projectile =
                manager.spawnProjectile(this, target, definition.travel, projanims.magic_spell)
            val (serverDelay, clientDelay) = projectile.durations
            val accuracyPercent =
                if (
                    definition.family == AncientSpellFamily.Ice &&
                        familySceptre &&
                        target.canReceiveIceSceptreAccuracy()
                ) {
                    110
                } else {
                    100
                }
            val splash = manager.rollSplash(this, target, attack, castResult, accuracyPercent)
            val castSound = if (index == 0) definition.castSound else null

            if (splash) {
                manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
                manager.queueSplashHit(this, target, definition.spell, clientDelay, serverDelay)
                continue
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, definition.baseMaxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = castSound,
                soundRadius = 8,
                hitSpot = definition.impact,
                hitSpotHeight = 124,
                hitSound = definition.impactSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, definition.spell, damage, clientDelay, serverDelay)
            target.queueAncientImpact(
                AncientImpact(
                    source = player,
                    family = definition.family,
                    effectStrength = definition.effectStrength,
                    damage = damage,
                    ancientSceptre = ancientSceptre,
                    familySceptre = familySceptre,
                    bloodbarkPieces = bloodbarkPieces,
                    poisonProc =
                        definition.family == AncientSpellFamily.Smoke && random.randomBoolean(8),
                ),
                serverDelay,
            )
        }
        manager.continueCombatIfAutocast(this, primary)
    }

    private fun ProtectedAccess.canAccessAncients(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.desert_treasure_main] < DESERT_TREASURE_COMPLETE) {
            mes("You need to complete Desert Treasure to cast this spell.")
            return false
        }
        return true
    }

    private fun PathingEntity.queueAncientImpact(impact: AncientImpact, delay: Int) {
        when (this) {
            is Npc -> queue(ancient_queues.impact, delay, impact)
            is Player -> strongQueue(ancient_queues.impact, delay, impact)
        }
    }

    private fun Player.hasAncientSceptre(): Boolean =
        righthand.isAnyType(
            objs.ancient_sceptre,
            objs.blood_ancient_sceptre,
            objs.ice_ancient_sceptre,
            objs.smoke_ancient_sceptre,
            objs.shadow_ancient_sceptre,
        )

    private fun Player.hasFamilySceptre(family: AncientSpellFamily): Boolean =
        when (family) {
            AncientSpellFamily.Smoke -> righthand.isType(objs.smoke_ancient_sceptre)
            AncientSpellFamily.Shadow -> righthand.isType(objs.shadow_ancient_sceptre)
            AncientSpellFamily.Blood -> righthand.isType(objs.blood_ancient_sceptre)
            AncientSpellFamily.Ice -> righthand.isType(objs.ice_ancient_sceptre)
        }

    private fun Player.bloodbarkPieces(): Int =
        listOf(hat, torso, legs, hands, feet).count {
            it.isAnyType(
                objs.bloodbark_helm,
                objs.bloodbark_body,
                objs.bloodbark_legs,
                objs.bloodbark_gauntlets,
                objs.bloodbark_greaves,
            )
        }

    private fun PathingEntity.canReceiveIceSceptreAccuracy(): Boolean {
        if (bindings.status(this) != BindingStatus.Available) return false
        return this !is Npc || visType.param(params.freeze_resistance) < 100
    }

    private companion object {
        const val DESERT_TREASURE_COMPLETE = 15
    }
}
