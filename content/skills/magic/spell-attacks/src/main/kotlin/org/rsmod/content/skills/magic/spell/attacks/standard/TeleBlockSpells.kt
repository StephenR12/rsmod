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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.config.teleblock_queues
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.proj.ProjAnimTypeList
import org.rsmod.game.type.seq.SeqType

public class TeleBlockSpells
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val projAnimTypes: ProjAnimTypeList,
    private val states: TeleBlockStateRepository,
) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(objs.spell_tele_block, TeleBlockSpellAttack(manager))
    }

    private inner class TeleBlockSpellAttack(private val manager: SpellAttackManager) :
        SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            manager.stopCombat(this)
            mes("You can only cast this spell on another player in the Wilderness.")
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            if (target.vars[varbits.inside_wilderness] != 1) {
                manager.stopCombat(this)
                mes("You can't attack players who aren't in the wilderness.")
                return
            }
            if (states.status(target) != TeleBlockStatus.Available) {
                manager.stopCombat(this)
                mes("That player is already being affected by this spell.")
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            val weaponType = objTypes.getOrNull(attack.weapon)
            anim(weaponType.castAnim())
            val splash = manager.rollSplash(this, target, attack, castResult)
            val timing =
                if (splash) {
                    manager
                        .spawnProjectile(
                            this,
                            target,
                            spotanims.tele_block_travel_forfail,
                            projanims.bind,
                        )
                        .durations
                } else {
                    val clientDelay =
                        org.rsmod.game.proj.ProjAnim.calculateEndTime(
                            projAnimTypes[projanims.bind],
                            distanceTo(target),
                        )
                    org.rsmod.game.proj.ProjAnim.Duration(
                        serverDelay = 1 + (clientDelay / 30),
                        clientDelay = clientDelay,
                    )
                }

            val spell = attack.spell.obj
            if (splash) {
                manager.playSplashFx(
                    source = this,
                    target = target,
                    clientDelay = timing.clientDelay,
                    castSound = synths.teleportblock_cast,
                    soundRadius = 8,
                )
                manager.queueSplashHit(
                    source = this,
                    target = target,
                    spell = spell,
                    clientDelay = timing.clientDelay,
                    hitDelay = timing.serverDelay,
                )
                manager.stopCombat(this)
                return
            }

            statAdvance(stats.magic, SUCCESS_XP_BONUS)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = timing.clientDelay,
                castSound = synths.teleportblock_cast,
                soundRadius = 8,
                hitSpot = spotanims.tele_block_impact,
                hitSpotHeight = 0,
                hitSound = synths.teleportblock_impact,
            )
            manager.queueEffectRetaliation(this, target, timing.serverDelay)
            target.strongQueue(
                teleblock_queues.impact,
                timing.serverDelay,
                TeleBlockImpact(
                    source = player,
                    protectedFromMagicAtCast = target.vars[varbits.protect_from_magic] == 1,
                ),
            )
            manager.stopCombat(this)
        }

        private fun UnpackedObjType?.castAnim(): SeqType =
            if (this != null && isCategoryType(categories.staff)) {
                seqs.human_casting_tele_block_staff
            } else {
                seqs.human_casting_tele_block
            }
    }

    private companion object {
        const val SUCCESS_XP_BONUS: Double = 11.4
    }
}
