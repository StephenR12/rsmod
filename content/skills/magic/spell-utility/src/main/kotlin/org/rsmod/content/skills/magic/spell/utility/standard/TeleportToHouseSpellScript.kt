package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TeleportToHouseSpellScript
@Inject
constructor(
    private val spell: TeleportToHouseSpell,
    private val houses: PlayerOwnedHouseRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_teleport_to_house) {
            val destination =
                when (op) {
                    IfButtonOp.Op1 -> HouseTeleportDestination.Inside
                    IfButtonOp.Op2,
                    IfButtonOp.Op3 -> HouseTeleportDestination.Outside
                    IfButtonOp.Op4 -> HouseTeleportDestination.GroupChoose
                    IfButtonOp.Op5 -> HouseTeleportDestination.GroupPrevious
                    else -> return@onIfOverlayButton
                }
            protectedAccess.launch(player) { spell.cast(this, destination) }
        }
        onPlayerLogout { houses.clear(player) }
    }
}
