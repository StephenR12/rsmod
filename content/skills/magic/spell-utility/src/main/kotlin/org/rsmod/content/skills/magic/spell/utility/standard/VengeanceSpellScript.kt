package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class VengeanceSpellScript
@Inject
constructor(
    private val spell: VengeanceSpell,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_vengeance) {
            when (op) {
                IfButtonOp.Op1 -> protectedAccess.launch(player) { spell.cast(this) }
                IfButtonOp.Op10 ->
                    protectedAccess.launch(player) { spell.openAnimationPicker(this) }
                else -> Unit
            }
        }
        onOpHeld1(objs.league_4_vengeance_scroll) { readAnimationScroll(it.inventory, it.slot) }
        onIfModalButton(components.animation_picker_clickable) {
            if (it.op == IfButtonOp.Op1) {
                spell.toggleAnimation(this, it.comsub)
            }
        }
    }

    /** Consumes cache 233's reward scroll and permanently exposes the League-IV picker option. */
    public fun ProtectedAccess.readAnimationScroll(inventory: Inventory, slot: Int) {
        if (!player.members || vars[varbits.league_4_vengeance_animation_unlocked] != 0) {
            return
        }
        val removed = invDel(inventory, objs.league_4_vengeance_scroll, count = 1, slot = slot)
        if (removed.failure) {
            return
        }
        vars[varbits.league_4_vengeance_animation_unlocked] = 1
        anim(seqs.league_4_trailblazer_reloaded_scroll_read)
    }
}
