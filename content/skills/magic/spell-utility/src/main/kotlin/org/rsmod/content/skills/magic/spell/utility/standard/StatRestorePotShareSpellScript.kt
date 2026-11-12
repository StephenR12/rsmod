package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class StatRestorePotShareSpellScript
@Inject
constructor(
    private val spell: StatRestorePotShareSpell,
    private val stamina: StaminaEffectRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButtonT(
            components.magic_spellbook_stat_restore_pot_share,
            components.inv_items,
        ) {
            castPotionShare()
        }
        onPlayerSoftTimer(timers.stamina_effect) { stamina.tick(player) }
        onPlayerLogin { stamina.clear(player) }
        onPlayerLogout { stamina.clear(player) }
    }

    private fun IfOverlayButtonT.castPotionShare() {
        val target = targetObj ?: return
        protectedAccess.launch(player) {
            val current = player.inv[targetSlot]
            if (!current.isType(target)) {
                return@launch
            }
            spell.cast(this, targetSlot)
        }
    }
}
