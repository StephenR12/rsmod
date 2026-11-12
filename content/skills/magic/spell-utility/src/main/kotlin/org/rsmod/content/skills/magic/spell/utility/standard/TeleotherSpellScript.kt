package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onApPlayerT
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TeleotherSpellScript
@Inject
constructor(
    private val teleother: TeleotherSpells,
    private val lunarGroupOffers: LunarGroupTeleportOfferRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (definition in TeleotherDefinition.entries) {
            onApPlayerT(definition.component) { teleother.cast(this, it.target, definition.spell) }
        }
        onIfClose(interfaces.teleport_other) {
            teleother.clear(player)
            lunarGroupOffers.remove(player)
        }
        onPlayerLogout {
            teleother.clear(player)
            lunarGroupOffers.remove(player)
        }
    }
}
