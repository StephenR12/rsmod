package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.timers
import org.rsmod.api.script.onPlayerHit
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Shared lifecycle for effects armed by either Vengeance spell. */
public class VengeanceEffectScript
@Inject
constructor(
    private val states: VengeanceStateRepository,
    private val reflection: VengeanceReflection,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerHit { reflection.impact(this) }
        onPlayerSoftTimer(timers.magic_vengeance_cooldown) { states.tickCooldown(player) }
        onPlayerLogin { states.syncOnLogin(player) }
        onPlayerLogout { states.clearActive(player) }
    }
}
