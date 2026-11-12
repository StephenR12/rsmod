package org.rsmod.content.skills.magic.spell.attacks.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.chargeRemaining by intVarp(varps.magearena_charge)

private var Player.chargeCooldown by intVarBit(varbits.buff_charge_spell_disabled)

public class ChargeSpell
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (vars[varbits.buff_charge_spell_disabled] != 0) {
                mes("You can only cast this spell once every 60 seconds.")
                return
            }
            if (vars[varps.magearena] == 0) {
                mes("You need to have claimed a god staff before you can cast this spell.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_charge))
            val result = runes.attemptCast(player, spell)
            if (result.isFailure()) {
                return
            }

            statAdvance(stats.magic, spell.castXp)
            anim(seqs.human_casting)
            vars[varps.magearena_charge] = CHARGE_VARP_START
            player.chargeCooldown = 1
            player.softTimer(timers.magic_charge_active, CHARGE_VARP_TICK_CYCLES)
            player.softTimer(timers.magic_charge_cooldown, COOLDOWN_CYCLES)
            mes("<col=ef1020>You feel charged with magic power.</col>")
        }
    }

    public companion object {
        // RuneLite and the official client interpret this varp as two remaining game ticks per
        // unit.
        public const val CHARGE_VARP_START: Int = 350
        public const val CHARGE_VARP_TICK_CYCLES: Int = 2
        public const val COOLDOWN_CYCLES: Int = 100
    }
}

public class ChargeSpellScript
@Inject
constructor(private val charge: ChargeSpell, private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_charge) {
            protectedAccess.launch(player) { charge.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_charge_active) { player.tickCharge() }
        onPlayerSoftTimer(timers.magic_charge_cooldown) { player.chargeCooldown = 0 }
    }

    private fun Player.tickCharge() {
        val remaining = vars[varps.magearena_charge]
        if (remaining <= 1) {
            chargeRemaining = 0
            mes("<col=ef1020>Your magical charge fades away.</col>")
            return
        }
        chargeRemaining = remaining - 1
        softTimer(timers.magic_charge_active, ChargeSpell.CHARGE_VARP_TICK_CYCLES)
    }
}
