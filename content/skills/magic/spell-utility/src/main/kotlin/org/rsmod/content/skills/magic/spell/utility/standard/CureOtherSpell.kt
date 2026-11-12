package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player

/** Cache-233 Lunar Cure Other behavior. */
public class CureOtherSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, target: Player) {
        with(access) {
            if (player === target || !cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }
            if (target.vars[varbits.option_acceptaid] == 0) {
                mes("${target.displayName} is not accepting aid.")
                return
            }

            val poisonState = target.vars[varps.poison]
            if (poisonState <= 0) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_cure_other))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            VarPlayerIntMapSetter.set(target, varps.poison, LunarPoisonCure.curedState(poisonState))
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.quest_lunar_pushing_magic_animation)
            target.spotanim(
                spotanims.quest_lunar_spellbook_energy_heal_spot_anim,
                height = GRAPHIC_HEIGHT,
            )
            soundSynth(synths.lunar_cure)
            cooldowns.markCast(player, mapClock + CAST_RATE)
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
        return true
    }

    private companion object {
        const val CAST_RATE = 5
        const val GRAPHIC_HEIGHT = 96
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
