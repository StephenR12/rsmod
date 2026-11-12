package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.magic.WardOfArceuusStateRepository
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.wardOfArceuusCooldown by intVarBit(varbits.arceuus_ward_cooldown)

public class WardOfArceuusSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: WardOfArceuusStateRepository,
    private val corruption: CorruptionStateRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.wardOfArceuusCooldown != 0) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_ward_of_arceuus))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            val duration = player.baseMagicLvl
            anim(seqs.self_imbue)
            spotanim(spotanims.ward_of_arceuus_cast)
            corruption.clearStatus(player)
            states.activate(player, duration)
            player.wardOfArceuusCooldown = 1
            player.softTimer(timers.magic_ward_of_arceuus_cooldown, COOLDOWN_CYCLES)
            player.softTimer(timers.magic_ward_of_arceuus_expiry, duration)
            mes("<col=0000b2>Your defence against Arceuus magic has been strengthened.</col>")
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
        public const val COOLDOWN_CYCLES: Int = 50
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
    }
}

public class WardOfArceuusSpellScript
@Inject
constructor(
    private val spell: WardOfArceuusSpell,
    private val states: WardOfArceuusStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_ward_of_arceuus) {
            protectedAccess.launch(player) { spell.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_ward_of_arceuus_cooldown) {
            player.wardOfArceuusCooldown = 0
        }
        onPlayerSoftTimer(timers.magic_ward_of_arceuus_expiry) {
            if (states.remainingCycles(player) == 0) {
                states.clear(player)
            }
        }
    }
}
