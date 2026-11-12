package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType

public class CrumbleUndeadSpells @Inject constructor(private val objTypes: ObjTypeList) :
    SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(objs.spell_crumble_undead, CrumbleUndeadSpellAttack(manager))
    }

    private inner class CrumbleUndeadSpellAttack(private val manager: SpellAttackManager) :
        SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            if (target.type.param(params.undead) == 0) {
                rejectTarget()
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            val weaponType = objTypes.getOrNull(attack.weapon)
            anim(weaponType.castAnim())
            spotanim(spotanims.crumbleundead_casting, height = 92)

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    spotanims.crumbleundead_travel,
                    projanims.crumble_undead,
                )
            val (serverDelay, clientDelay) = projectile.durations
            val spell = attack.spell.obj
            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = synths.crumble_cast_and_fire,
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
                castSound = synths.crumble_cast_and_fire,
                soundRadius = 8,
                hitSpot = spotanims.crumbleundead_impact,
                hitSpotHeight = 124,
                hitSound = synths.crumble_hit,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            rejectTarget()
        }

        private fun ProtectedAccess.rejectTarget() {
            manager.stopCombat(this)
            mes("This spell only affects skeletons, zombies, ghosts and shades")
        }

        private fun UnpackedObjType?.castAnim(): SeqType =
            if (this != null && isCategoryType(categories.staff)) {
                seqs.human_castcrumbleundead_staff
            } else {
                seqs.human_castcrumbleundead
            }
    }

    private companion object {
        const val BASE_MAX_HIT: Int = 15
    }
}
