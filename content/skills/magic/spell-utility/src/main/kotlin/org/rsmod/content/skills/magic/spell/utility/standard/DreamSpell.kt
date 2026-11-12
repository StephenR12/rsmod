package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.type.seq.SeqTypeList

/** Cache-233 Lunar Dream, including its independent three-per-minute healing channel. */
public class DreamSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: DreamStateRepository,
    private val seqTypes: SeqTypeList,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || states.isActive(player)) {
                return
            }
            if (player.stat(stats.hitpoints) >= player.statBase(stats.hitpoints)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_dream))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            stopAction()
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.dream_spell_start)
            val token = states.begin(player)
            queue(
                dream_queues.channel,
                1,
                DreamChannel(token, elapsedTicks = 1, observedHitpoints = stat(stats.hitpoints)),
            )
        }
    }

    internal fun advance(access: ProtectedAccess, channel: DreamChannel) {
        with(access) {
            if (!states.isActive(player, channel.token)) {
                return
            }

            val hitpoints = stat(stats.hitpoints)
            if (hitpoints < channel.observedHitpoints) {
                finish(this, channel.token)
                return
            }
            if (hitpoints >= statBase(stats.hitpoints)) {
                finish(this, channel.token)
                return
            }

            if (channel.elapsedTicks == seqTypes[seqs.dream_spell_start].tickDuration) {
                anim(seqs.dream_player_dream_spell_ready)
                spotanim(spotanims.dream_zeds_spotanim)
            }

            if (isHealTick(channel.elapsedTicks)) {
                statHeal(stats.hitpoints, constant = 1, percent = 0)
                if (stat(stats.hitpoints) >= statBase(stats.hitpoints)) {
                    finish(this, channel.token)
                    return
                }
            }

            queue(
                dream_queues.channel,
                1,
                channel.copy(
                    elapsedTicks = channel.elapsedTicks + 1,
                    observedHitpoints = stat(stats.hitpoints),
                ),
            )
        }
    }

    public fun interrupt(player: Player) {
        if (!states.cancel(player)) {
            return
        }
        player.clearQueue(dream_queues.channel)
        player.resetSpotanim()
        player.anim(seqs.dream_spell_end, delay = 0, priority = seqs.dream_spell_end.priority)
    }

    public fun clear(player: Player) {
        states.cancel(player)
        player.clearQueue(dream_queues.channel)
    }

    private fun finish(access: ProtectedAccess, token: Int) {
        with(access) {
            if (!states.finish(player, token)) {
                return
            }
            clearQueue(dream_queues.channel)
            resetSpotanim()
            anim(seqs.dream_spell_end)
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        if (vars[varbits.dream_mentor] < DREAM_MENTOR_COMPLETE) {
            mes("You need to complete Dream Mentor to cast this spell.")
            return false
        }
        return true
    }

    private fun isHealTick(elapsedTicks: Int): Boolean =
        when (elapsedTicks % REGEN_PERIOD_TICKS) {
            FIRST_HEAL_TICK,
            SECOND_HEAL_TICK,
            0 -> true
            else -> false
        }

    private companion object {
        private const val LUNAR_DIPLOMACY_COMPLETE = 190
        private const val DREAM_MENTOR_COMPLETE = 28
        private const val REGEN_PERIOD_TICKS = 100
        private const val FIRST_HEAL_TICK = 34
        private const val SECOND_HEAL_TICK = 67
    }
}
