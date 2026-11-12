package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.proj.ProjAnimTypeList

internal var Player.darkLureCooldown by intVarBit(varbits.arceuus_dark_lure_cooldown)

public class DarkLureSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val worldRepo: WorldRepository,
    private val projAnimTypes: ProjAnimTypeList,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    public fun cast(access: ProtectedAccess, target: Npc) {
        with(access) {
            if (!canAccess() || player.darkLureCooldown != 0) {
                return
            }
            if (target.collisionStrategy == null) {
                mes("This creature cannot be lured because it cannot move.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_dark_lure))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_spellcast_grasp)
            spotanim(spotanims.dark_lure_cast)
            val projectile =
                worldRepo.projAnim(
                    player,
                    target,
                    spotanims.dark_lure_travel,
                    projAnimTypes[projanims.magic_spell_low],
                )
            strongQueue(
                dark_lure_queues.impact,
                projectile.serverCycles,
                DarkLureImpact(target, target.uid),
            )
            player.darkLureCooldown = 1
            player.softTimer(timers.magic_arceuus_dark_lure_cooldown, CAST_RATE)
        }
    }

    internal fun impact(access: ProtectedAccess, request: DarkLureImpact) {
        with(access) {
            val target = request.npc
            if (target.uid != request.uid || !target.isValidTarget()) {
                return
            }
            target.spotanim(spotanims.dark_lure_hit)
            if (target.visType.op.getOrNull(ATTACK_OP_INDEX) == "Attack") {
                target.opPlayer2(player, aiPlayerInteractions)
            } else if (target.visType.name.endsWith(IMPLING_NAME_SUFFIX, ignoreCase = true)) {
                target.playerFollow(player)
                target.timer(timers.magic_arceuus_dark_lure_follow, FOLLOW_CYCLES)
            }
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        return true
    }

    public companion object {
        public const val CAST_RATE: Int = 5
        public const val FOLLOW_CYCLES: Int = 10
        private const val ATTACK_OP_INDEX: Int = 1
        private const val IMPLING_NAME_SUFFIX: String = " impling"
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
    }
}
