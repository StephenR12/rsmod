package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LunarTeleportSpellScript
@Inject
constructor(
    private val teleports: LunarTeleportSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(components.magic_spellbook_tele_moonclan, objs.spell_moonclan_teleport)
        register(components.magic_spellbook_ourania_teleport, objs.spell_zmi_teleport)
        register(components.magic_spellbook_tele_waterbirth, objs.spell_waterbirth_teleport)
        register(components.magic_spellbook_tele_barb_out, objs.spell_barboutpost_teleport)
        register(components.magic_spellbook_tele_khazard, objs.spell_portkhazard_teleport)
        register(components.magic_spellbook_tele_fish, objs.spell_fishguild_teleport)
        register(components.magic_spellbook_tele_cather, objs.spell_catherby_teleport)
        register(components.magic_spellbook_tele_ghorrock, objs.spell_lunarghorrock_teleport)
    }

    private fun ScriptContext.register(component: ComponentType, spell: ObjType) {
        onIfOverlayButton(component) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { teleports.cast(this, spell) }
            }
        }
    }
}
