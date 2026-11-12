package org.rsmod.content.skills.magic.spell.utility.arceuus

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

public class ArceuusTeleportSpellScript
@Inject
constructor(
    private val teleports: ArceuusTeleportSpells,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        register(
            components.magic_spellbook_arceuus_library_teleport,
            objs.spell_arceuuslibrary_teleport,
        )
        register(
            components.magic_spellbook_draynor_manor_teleport,
            objs.spell_draynormanor_teleport,
        )
        register(components.magic_spellbook_battlefront_teleport, objs.spell_battlefront_teleport)
        register(components.magic_spellbook_mind_altar_teleport, objs.spell_mindaltar_teleport)
        register(components.magic_spellbook_respawn_teleport, objs.spell_respawn_teleport)
        register(
            components.magic_spellbook_salve_graveyard_teleport,
            objs.spell_salvegrave_teleport,
        )
        register(
            components.magic_spellbook_fenkenstrain_castle_teleport,
            objs.spell_fenkenstrainscastle_teleport,
        )
        register(
            components.magic_spellbook_west_ardougne_teleport,
            objs.spell_westardougne_teleport,
        )
        register(
            components.magic_spellbook_harmony_island_teleport,
            objs.spell_harmonyisland_teleport,
        )
        register(components.magic_spellbook_cemetery_teleport, objs.spell_cemetery_teleport)
        register(components.magic_spellbook_barrows_teleport, objs.spell_barrows_teleport)
        register(
            components.magic_spellbook_arceuus_ape_atoll_teleport,
            objs.spell_arceuusapeatoll_teleport,
        )
    }

    private fun ScriptContext.register(component: ComponentType, spell: ObjType) {
        onIfOverlayButton(component) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { teleports.cast(this, spell) }
            }
        }
    }
}
