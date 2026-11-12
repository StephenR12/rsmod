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
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/** Cache-233 Lunar Heal Other behavior. */
public class HealOtherSpell
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
            if (target.hitpoints == 0) {
                return
            }

            val current = player.hitpoints
            if (!LunarHealOther.hasRequiredHealth(current, player.baseHitpointsLvl)) {
                return
            }
            val transfer =
                LunarHealOther.transfer(current, target.hitpoints, target.baseHitpointsLvl)
            if (transfer == 0) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_heal_other))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.quest_lunar_pushing_magic_animation)
            target.spotanim(
                spotanims.quest_lunar_spellbook_energy_heal_spot_anim,
                height = GRAPHIC_HEIGHT,
            )
            soundSynth(synths.lunar_heal_other)
            target.statHeal(stats.hitpoints, constant = transfer, percent = 0)
            takeInstantHit(HitType.Typeless, transfer)
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
