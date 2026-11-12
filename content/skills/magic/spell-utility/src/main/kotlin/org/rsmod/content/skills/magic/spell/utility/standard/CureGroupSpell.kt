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
import org.rsmod.game.entity.PlayerList

/** Cache-233 Lunar Cure Group behavior. */
public class CureGroupSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
    private val players: PlayerList,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_cure_group))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.quest_lunar_spell_cast_spell_on_group)
            soundSynth(synths.lunar_cure)

            for (recipient in recipients(player)) {
                val poisonState = recipient.vars[varps.poison]
                if (poisonState > 0) {
                    VarPlayerIntMapSetter.set(
                        recipient,
                        varps.poison,
                        LunarPoisonCure.curedState(poisonState),
                    )
                }
                recipient.spotanim(
                    spotanims.quest_lunar_spellbook_cure_group_spot_anim,
                    height = GRAPHIC_HEIGHT,
                )
            }
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    private fun recipients(caster: Player): Sequence<Player> =
        players
            .asSequence()
            .filter { it.coords.level == caster.coords.level }
            .filter { it.coords.chebyshevDistance(caster.coords) <= AREA_RADIUS }
            .filter { it === caster || it.vars[varbits.option_acceptaid] != 0 }

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
        const val AREA_RADIUS = 1
        const val CAST_RATE = 5
        const val GRAPHIC_HEIGHT = 96
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
