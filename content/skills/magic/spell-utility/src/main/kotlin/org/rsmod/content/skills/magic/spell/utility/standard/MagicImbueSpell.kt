package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicImbueStateRepository
import org.rsmod.api.spells.MagicSpellRegistry

public class MagicImbueSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: MagicImbueStateRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || states.isActive(player)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_magic_imbue))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_cast_selfimbue)
            soundSynth(synths.lunar_embue_runes)
            states.activate(player)
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
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
