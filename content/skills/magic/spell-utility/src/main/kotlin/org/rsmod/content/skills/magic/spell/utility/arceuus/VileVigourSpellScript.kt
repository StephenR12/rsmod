package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.vileVigourCooldown by intVarBit(varbits.arceuus_vile_vigour_cooldown)

public class VileVigourSpellScript
@Inject
constructor(
    private val spell: VileVigourSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_vile_vigour) {
            protectedAccess.launch(player) { spell.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_arceuus_vile_vigour_cooldown) {
            player.vileVigourCooldown = 0
        }
    }
}
