package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class OfferingSpellsScript
@Inject
constructor(
    private val spells: OfferingSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_demonic_offering) {
            protectedAccess.launch(player) { spells.castDemonic(this) }
        }
        onIfOverlayButton(components.magic_spellbook_sinister_offering) {
            protectedAccess.launch(player) { spells.castSinister(this) }
        }
        onPlayerSoftTimer(timers.magic_arceuus_offering_cooldown) { player.offeringCooldown = 0 }
    }
}
