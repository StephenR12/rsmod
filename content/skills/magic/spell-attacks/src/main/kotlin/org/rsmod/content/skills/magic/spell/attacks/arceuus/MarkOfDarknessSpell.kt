package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MarkOfDarknessStateRepository
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.markOfDarknessCooldown by intVarBit(varbits.buff_mark_of_darkness_disabled)

public class MarkOfDarknessSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: MarkOfDarknessStateRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.markOfDarknessCooldown != 0) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_mark_of_darkness))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            val duration =
                if (player.righthand.isType(objs.purging_staff)) {
                    PURGING_STAFF_DURATION_CYCLES
                } else {
                    DURATION_CYCLES
                }
            anim(seqs.mark_of_darkness)
            spotanim(spotanims.mark_of_darkness_cast)
            states.activate(player, duration)
            player.markOfDarknessCooldown = 1
            player.softTimer(timers.magic_mark_of_darkness_cooldown, COOLDOWN_CYCLES)
            player.softTimer(timers.magic_mark_of_darkness_warning, duration - WARNING_LEAD_CYCLES)
            player.softTimer(timers.magic_mark_of_darkness_expiry, duration)
            mes("<col=ef1020>You have placed a Mark of Darkness upon yourself.</col>")
        }
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
        public const val DURATION_CYCLES = 300
        public const val PURGING_STAFF_DURATION_CYCLES = DURATION_CYCLES * 5
        public const val COOLDOWN_CYCLES = 10
        public const val WARNING_LEAD_CYCLES = 10
        private const val A_KINGDOM_DIVIDED_COMPLETE = 150
    }
}

public class MarkOfDarknessSpellScript
@Inject
constructor(
    private val spell: MarkOfDarknessSpell,
    private val states: MarkOfDarknessStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_mark_of_darkness) {
            protectedAccess.launch(player) { spell.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_mark_of_darkness_cooldown) {
            player.markOfDarknessCooldown = 0
        }
        onPlayerSoftTimer(timers.magic_mark_of_darkness_warning) {
            if (states.remainingCycles(player) in 1..MarkOfDarknessSpell.WARNING_LEAD_CYCLES) {
                player.mes("Your Mark of Darkness is about to run out.")
            }
        }
        onPlayerSoftTimer(timers.magic_mark_of_darkness_expiry) {
            if (states.remainingCycles(player) == 0) {
                states.clear(player)
                player.spotanim(spotanims.mark_of_darkness_end)
            }
        }
    }
}
