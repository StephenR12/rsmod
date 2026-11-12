package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.deathChargeCooldown by intVarBit(varbits.arceuus_death_charge_cooldown)

public class DeathChargeSpellScript
@Inject
constructor(
    private val spell: DeathChargeSpell,
    private val states: DeathChargeStateRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(components.magic_spellbook_death_charge) {
            protectedAccess.launch(player) { spell.cast(this) }
        }
        onOpHeld1(objs.death_charge_scroll) { readDeathChargeScroll(it.inventory, it.slot) }
        onEvent<NpcKillEvents.Credited> { spell.onNpcKill(player, npc) }
        onPlayerSoftTimer(timers.magic_arceuus_death_charge_cooldown) {
            player.deathChargeCooldown = 0
        }
        onPlayerSoftTimer(timers.magic_arceuus_death_charge_expiry) {
            if (states.remainingCycles(player) == 0) {
                states.clear(player)
            }
        }
    }

    public suspend fun ProtectedAccess.readDeathChargeScroll(inventory: Inventory, slot: Int) {
        if (vars[varbits.death_charge_scroll_used] != 0) {
            mes("You have already upgraded Death Charge.")
            return
        }

        mesbox(
            "You can make out some faded words on the ancient parchment. It appears to be a " +
                "demonic learning on spell efficiency. Would you like to absorb its knowledge?"
        )
        val learn =
            choice2(
                "Upgrade Death Charge",
                true,
                "Cancel",
                false,
                title = "This will consume the scroll.",
            )
        if (!learn) {
            return
        }

        val removed = invDel(inventory, objs.death_charge_scroll, count = 1, slot = slot)
        if (removed.failure) {
            return
        }
        vars[varbits.death_charge_scroll_used] = 1
        anim(seqs.death_charge)
        spotanim(spotanims.death_charge_upgrade_cast)
        mes("You study the scroll and upgrade an Arceuus Spell: Death Charge")
    }
}
