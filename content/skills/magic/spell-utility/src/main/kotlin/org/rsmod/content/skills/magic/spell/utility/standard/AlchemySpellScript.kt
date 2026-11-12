package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AlchemySpellScript
@Inject
constructor(
    private val alchemy: AlchemySpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButtonT(components.magic_spellbook_low_alchemy, components.inv_items) {
            cast(objs.spell_low_alchemy)
        }
        onIfOverlayButtonT(components.magic_spellbook_high_alchemy, components.inv_items) {
            cast(objs.spell_high_alchemy)
        }
    }

    private fun IfOverlayButtonT.cast(spell: ObjType) {
        val target = targetObj ?: return
        protectedAccess.launch(player) {
            val current = player.inv[targetSlot]
            if (!current.isType(target)) {
                return@launch
            }
            alchemy.cast(this, spell, targetSlot)
        }
    }
}
