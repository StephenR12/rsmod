package org.rsmod.content.skills.magic.spell.utility.ancient

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AncientTeleportSpellScript
@Inject
constructor(
    private val teleports: AncientTeleportSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(AncientTeleportComponents.paddewwa, objs.spell_paddewwa_teleport)
        register(AncientTeleportComponents.senntisten, objs.spell_senntisten_teleport)
        register(AncientTeleportComponents.kharyrll, objs.spell_kharyllyl_teleport)
        register(AncientTeleportComponents.lassar, objs.spell_lassar_teleport)
        register(AncientTeleportComponents.dareeyak, objs.spell_dareeyak_teleport)
        register(AncientTeleportComponents.carrallangar, objs.spell_carrallagar_teleport)
        register(AncientTeleportComponents.annakarl, objs.spell_annakarl_teleport)
        register(AncientTeleportComponents.ghorrock, objs.spell_ghorrock_teleport)
    }

    private fun ScriptContext.register(component: ComponentType, spell: ObjType) {
        onIfOverlayButton(component) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { teleports.cast(this, spell) }
            }
        }
    }
}

internal object AncientTeleportComponents : ComponentReferences() {
    val paddewwa = find("magic_spellbook:zarosteleport1")
    val senntisten = find("magic_spellbook:zarosteleport2")
    val kharyrll = find("magic_spellbook:zarosteleport3")
    val lassar = find("magic_spellbook:zarosteleport4")
    val dareeyak = find("magic_spellbook:zarosteleport5")
    val carrallangar = find("magic_spellbook:zarosteleport6")
    val annakarl = find("magic_spellbook:zarosteleport7")
    val ghorrock = find("magic_spellbook:zarosteleport8")
}
