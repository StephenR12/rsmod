package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player

private var Player.vileVigourCooldown by intVarBit(varbits.arceuus_vile_vigour_cooldown)

public class VileVigourSpell
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.vileVigourCooldown != 0) {
                return
            }
            val converted = convertiblePrayer()
            if (converted == 0) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_vile_vigour))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            player.statSub(stats.prayer, constant = converted, percent = 0)
            player.runEnergy =
                min(constants.run_max_energy, player.runEnergy + converted * RUN_PER_PRAYER)
            UpdateRun.energy(player, player.runEnergy)
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_cast_vile_vigour)
            spotanim(spotanims.vile_vigour_cast)
            player.vileVigourCooldown = 1
            player.softTimer(timers.magic_arceuus_vile_vigour_cooldown, COOLDOWN_CYCLES)
        }
    }

    private fun ProtectedAccess.convertiblePrayer(): Int {
        val missingEnergy = constants.run_max_energy - player.runEnergy
        if (missingEnergy <= 0) {
            mes("Your run energy is already full.")
            return 0
        }
        val prayer = player.stat(stats.prayer)
        if (prayer <= 0) {
            mes("You do not have any Prayer points to convert.")
            return 0
        }
        val pointsNeeded = (missingEnergy + RUN_PER_PRAYER - 1) / RUN_PER_PRAYER
        return min(prayer, pointsNeeded)
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
        public const val COOLDOWN_CYCLES: Int = 17
        public const val RUN_PER_PRAYER: Int = 100
        private const val A_KINGDOM_DIVIDED_COMPLETE = 150
    }
}
