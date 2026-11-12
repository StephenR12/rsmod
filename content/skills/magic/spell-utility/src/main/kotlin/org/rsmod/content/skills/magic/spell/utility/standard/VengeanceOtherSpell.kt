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
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player

/** Cache-233 Lunar Vengeance Other cast behavior. */
public class VengeanceOtherSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: VengeanceStateRepository,
    private val houses: PlayerOwnedHousePresenceRepository,
) {
    public fun cast(access: ProtectedAccess, target: Player) {
        with(access) {
            if (player === target || !canAccess()) {
                return
            }
            if (!states.isCooldownReady(player)) {
                mes("You can only cast Vengeance spells every 30 seconds.")
                return
            }
            if (houses.isInside(player) || houses.isInside(target)) {
                return
            }
            if (target.vars[varbits.option_acceptaid] == 0) {
                mes("${target.displayName} is not accepting aid.")
                return
            }
            if (target.hitpoints == 0 || !canCastInWilderness(target) || states.isActive(target)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_vengeance_other))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.quest_lunar_pushing_magic_animation)
            target.spotanim(
                spotanims.quest_lunar_spellbook_vengeance_spot_anim,
                height = GRAPHIC_HEIGHT,
            )
            soundSynth(synths.lunar_vengence_other)
            target.mes("You have the power of vengeance!")
            states.activate(target)
            states.beginCooldown(player)
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

    private fun ProtectedAccess.canCastInWilderness(target: Player): Boolean {
        val casterSingle =
            vars[varbits.inside_wilderness] == 1 && vars[varbits.multiway_indicator] == 0
        val targetSingle =
            target.vars[varbits.inside_wilderness] == 1 &&
                target.vars[varbits.multiway_indicator] == 0
        return !casterSingle && !targetSingle
    }

    private companion object {
        const val GRAPHIC_HEIGHT = 96
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
