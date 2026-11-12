package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfOverlayButtonT
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class JewelleryEnchantSpellScript
@Inject
constructor(
    private val enchant: JewelleryEnchantSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButtonT(components.magic_spellbook_enchant_1, components.inv_items) {
            cast(objs.spell_enchant_lvl_1)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_2, components.inv_items) {
            cast(objs.spell_enchant_lvl_2)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_3, components.inv_items) {
            cast(objs.spell_enchant_lvl_3)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_4, components.inv_items) {
            cast(objs.spell_enchant_lvl_4)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_5, components.inv_items) {
            cast(objs.spell_enchant_lvl_5)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_6, components.inv_items) {
            cast(objs.spell_enchant_lvl_6)
        }
        onIfOverlayButtonT(components.magic_spellbook_enchant_7, components.inv_items) {
            cast(objs.spell_enchant_lvl_7)
        }
        onPlayerQueueWithArgs<JewelleryEnchantRequest>(jewellery_enchant_queues.manual) {
            enchant.castQueued(this, it.args)
        }
        onPlayerQueueWithArgs<JewelleryEnchantRequest>(jewellery_enchant_queues.auto) {
            enchant.castQueued(this, it.args)
        }
    }

    private fun IfOverlayButtonT.cast(spell: ObjType) {
        val target = targetObj ?: return
        protectedAccess.launch(player) {
            val current = player.inv[targetSlot]
            if (!current.isType(target)) {
                return@launch
            }
            enchant.castManual(this, spell, target.id)
        }
    }
}
