package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry

/** Cache-233 Lunar Spellbook Swap casting behavior. */
public class SpellbookSwapSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: SpellbookSwapStateRepository,
) {
    public suspend fun cast(access: ProtectedAccess) {
        with(access) {
            val spell = requireNotNull(spells.getObjSpell(objs.spell_spellbook_swap))
            if (!canAccess() || !runes.canCastSpell(player, spell)) {
                return
            }

            val choices = accessibleSpellbooks()
            val selection =
                menu(
                    title = "Choose spellbook:",
                    hotkeys = false,
                    choices = choices.map(Choice::label) + "Cancel",
                )
            val selected = choices.getOrNull(selection)?.spellbook ?: return

            if (!canAccess() || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }

            val moving = player.routeDestination.isNotEmpty()
            anim(seqs.dream_player_spellbook_swap)
            spotanim(spotanims.dream_spellbook_spotanim)
            soundSynth(synths.lunar_change_spellbook)
            states.activate(player, selected)
            if (moving) {
                player.runEnergyDrainBlockedUntil = mapClock + CAST_TICKS
            }
            delay(CAST_TICKS)
        }
    }

    private fun ProtectedAccess.accessibleSpellbooks(): List<Choice> = buildList {
        add(Choice("Standard", Spellbook.Standard))
        if (vars[varbits.desert_treasure_main] >= DESERT_TREASURE_COMPLETE) {
            add(Choice("Ancient", Spellbook.Ancients))
        }
        if (vars[varbits.arceuus_spellbook_unlocked] != 0) {
            add(Choice("Arceuus", Spellbook.Arceuus))
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

    private data class Choice(val label: String, val spellbook: Spellbook)

    private companion object {
        const val CAST_TICKS = 4
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28
        const val DESERT_TREASURE_COMPLETE = 15
    }
}
