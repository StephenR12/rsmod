package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TeleportToTargetSpellScript
@Inject
constructor(
    private val spell: TeleportToTargetSpell,
    private val targets: BountyTargetRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_bounty_target) {
            if (op == IfButtonOp.Op1) {
                protectedAccess.launch(player) { spell.cast(this) }
            }
        }
        onOpHeld1(objs.bounty_teleport_scroll) { readTargetTeleportScroll(it.inventory, it.slot) }
        onPlayerLogout { targets.clear(player) }
    }

    public suspend fun ProtectedAccess.readTargetTeleportScroll(inventory: Inventory, slot: Int) {
        if (vars[varbits.bounty_target_teleport] != 0) {
            mesbox(
                "You have already unlocked the spell.<br>" +
                    "The scroll can be sold if you no longer need it."
            )
            return
        }

        mesbox(
            "The 'Teleport to Target' spell requires level 85 Magic, and can be cast only " +
                "within the Wilderness, while you have a Target. This scroll will be " +
                "destroyed when it has been read."
        )
        val learn =
            choice2(
                "Yes, I want to learn the spell.",
                true,
                "No, I'll keep the scroll.",
                false,
                title = "Are you sure you want to learn the spell?",
            )
        if (!learn) {
            return
        }

        val removed = invDel(inventory, objs.bounty_teleport_scroll, count = 1, slot = slot)
        if (removed.failure) {
            return
        }
        vars[varbits.bounty_target_teleport] = 1
        mes("You have learnt the 'Teleport to Target' spell.")
    }
}
