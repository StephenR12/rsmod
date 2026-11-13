package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class StandardTeleportSpellScript
@Inject
constructor(
    private val teleports: StandardTeleportSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(components.magic_spellbook_varrock_teleport, objs.spell_varrock_teleport)
        register(components.magic_spellbook_lumbridge_teleport, objs.spell_lumbridge_teleport)
        register(components.magic_spellbook_falador_teleport, objs.spell_falador_teleport)
        register(components.magic_spellbook_camelot_teleport, objs.spell_camelot_teleport)
        register(components.magic_spellbook_kourend_teleport, objs.spell_kourendcastle_teleport)
        register(components.magic_spellbook_ardougne_teleport, objs.spell_ardougne_teleport)
        register(
            components.magic_spellbook_civitas_fortis_teleport,
            objs.spell_civitas_fortis_teleport,
        )
        register(components.magic_spellbook_watchtower_teleport, objs.spell_watchtower_teleport)
        register(components.magic_spellbook_trollheim_teleport, objs.spell_trollheim_teleport)
        register(components.magic_spellbook_ape_atoll_teleport, objs.spell_apeatoll_teleport)
    }

    private fun ScriptContext.register(component: ComponentType, spell: ObjType) {
        onIfOverlayButton(component) {
            protectedAccess.launch(player) { teleports.handleButton(this, spell, op) }
        }
    }
}
