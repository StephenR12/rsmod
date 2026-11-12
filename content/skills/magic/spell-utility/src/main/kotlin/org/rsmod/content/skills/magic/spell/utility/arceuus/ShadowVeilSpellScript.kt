package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.magic.ShadowVeilStateRepository
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ShadowVeilSpellScript
@Inject
constructor(
    private val spell: ShadowVeilSpell,
    private val states: ShadowVeilStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_shadow_veil) {
            protectedAccess.launch(player) { spell.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_arceuus_shadow_veil_cooldown) {
            player.shadowVeilCooldown = 0
        }
        onPlayerSoftTimer(timers.magic_arceuus_shadow_veil_expiry) {
            if (states.remainingCycles(player) == 0) {
                states.clear(player)
                player.shadowVeilActive = 0
                player.mes("<col=6800bf>Your Shadow Veil has faded away.</col>")
            }
        }
    }
}
