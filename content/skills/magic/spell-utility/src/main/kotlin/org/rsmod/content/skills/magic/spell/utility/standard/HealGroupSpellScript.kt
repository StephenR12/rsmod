package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HealGroupSpellScript
@Inject
constructor(
    private val healGroup: HealGroupSpell,
    private val states: HealGroupStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_heal_group) {
            protectedAccess.launch(player) { healGroup.cast(this) }
        }
        onPlayerSoftTimer(timers.magic_heal_group_cooldown) { states.tick(player) }
        onPlayerLogin { states.syncOnLogin(player) }
    }
}
