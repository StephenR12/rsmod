package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.interf.IfEvent

/** Cache-233 Lunar Vengeance self-cast and animation-picker behavior. */
public class VengeanceSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: VengeanceStateRepository,
    private val houses: PlayerOwnedHousePresenceRepository,
    private val animations: VengeanceAnimationRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess()) {
                return
            }
            if (!states.isCooldownReady(player)) {
                mes("You can only cast Vengeance spells every 30 seconds.")
                return
            }
            if (houses.isInside(player) || states.isActive(player)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_vengeance))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            when (animations.choose(player, random)) {
                VengeanceAnimationRepository.LEAGUE_IV_INDEX -> playLeagueIvPresentation()
                else -> playDefaultPresentation()
            }
            states.activate(player)
            states.beginCooldown(player)
        }
    }

    public fun openAnimationPicker(access: ProtectedAccess) {
        with(access) {
            val enabled = animations.normalizedEnabledMask(player)
            val allowed = animations.allowedMask(player)
            vars[varps.toggle_list] = VENGEANCE_TOGGLE_DBROW
            ifOpenMainModal(interfaces.animation_picker)
            runClientScript(ANIMATION_PICKER_LOAD_SCRIPT, enabled, allowed, VENGEANCE_TOGGLE_DBROW)
            ifSetEvents(
                components.animation_picker_clickable,
                VengeanceAnimationRepository.DEFAULT_INDEX..VengeanceAnimationRepository
                        .LEAGUE_IV_INDEX,
                IfEvent.Op1,
            )
        }
    }

    public fun toggleAnimation(access: ProtectedAccess, index: Int) {
        animations.toggle(access.player, index)
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

    private fun ProtectedAccess.playDefaultPresentation() {
        val inPvpArea = vars[varbits.pvp_area_client] != 0 || vars[varbits.inside_wilderness] != 0
        anim(
            if (inPvpArea) {
                seqs.vengeance_spell_anim_stalling
            } else {
                seqs.vengeance_spell_anim_nostalling
            }
        )
        // Cache symbols retain the historical reversed names: self Vengeance is graphic 726.
        spotanim(spotanims.quest_lunar_spellbook_vengeance_other_spot_anim, height = GRAPHIC_HEIGHT)
        soundSynth(synths.lunar_vengence)
    }

    private fun ProtectedAccess.playLeagueIvPresentation() {
        anim(seqs.trailblazer_vengeance_spotanim_01)
        spotanim(spotanims.trailblazer_vengeance_spotanim_01, height = GRAPHIC_HEIGHT)
        soundSynth(synths.leagues4_vengence_spell_01)
    }

    public companion object {
        public const val ANIMATION_PICKER_LOAD_SCRIPT: Int = 7707
        public const val VENGEANCE_TOGGLE_DBROW: Int = 4094
        public const val GRAPHIC_HEIGHT: Int = 96
        private const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
