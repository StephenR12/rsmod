package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/** Cache-233 Lunar Energy Transfer behavior. */
public class EnergyTransferSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val specialEnergy: SpecialAttackEnergy,
    private val realm: Realm,
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
            if (target.hitpoints == 0 || !canTransferInPvpArea(target)) {
                return
            }
            if (player.hitpoints <= HITPOINT_COST) {
                mes("You need more hitpoints to cast this spell.")
                return
            }
            if (!specialEnergy.hasSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_energy_transfer))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            specialEnergy.takeSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)
            specialEnergy.restoreSpecialEnergy(target, SpecialAttackEnergy.MAX_ENERGY)
            target.runEnergy = constants.run_max_energy
            UpdateRun.energy(target, target.runEnergy)

            stopAction()
            anim(seqs.quest_lunar_spell_energy_exchange)
            target.spotanim(
                spotanims.quest_lunar_spellbook_energy_exchange_spot_anim,
                height = GRAPHIC_HEIGHT,
            )
            soundSynth(synths.lunar_energy_transfer)
            target.soundSynth(synths.lunar_transfer_other)
            takeInstantHit(HitType.Typeless, HITPOINT_COST)
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

    private fun ProtectedAccess.canTransferInPvpArea(target: Player): Boolean {
        val casterInWilderness = vars[varbits.inside_wilderness] == 1
        val targetInWilderness = target.vars[varbits.inside_wilderness] == 1
        val casterSingle = vars[varbits.multiway_indicator] == 0
        val targetSingle = target.vars[varbits.multiway_indicator] == 0
        if ((casterInWilderness || targetInWilderness) && (casterSingle || targetSingle)) {
            return false
        }

        val pvpWorld = realm.config.hasWorldFlag(WorldFlag.Pvp)
        val hasRecordedPvpCombat = target.vars[varps.lastcombat_pvp] > 0
        return !pvpWorld || !hasRecordedPvpCombat || !target.isInPvpCombat()
    }

    private companion object {
        const val CAST_RATE = 5
        const val GRAPHIC_HEIGHT = 96
        const val HITPOINT_COST = 10
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}
