package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onNpcAccessEvent
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NetTrapScript @Inject constructor(private val traps: NetTrap) : PluginScript() {
    override fun ScriptContext.startup() {
        for (species in NetTrapSpeciesList.all) {
            onOpLoc1(species.tree) { traps.lay(this, it.loc, species) }
            onOpLoc1(species.open) { traps.dismantle(this, it.loc) }
            onOpLoc2(species.open) { traps.investigate(this, it.loc) }
            onOpLoc1(species.full) { traps.check(this, it.loc) }
            onOpLoc2(species.full) { traps.check(this, it.loc, reset = true) }
            onOpLoc1(species.broken) { traps.dismantle(this, it.loc) }
            onOpLoc2(species.broken) { traps.reset(this, it.loc) }
            onNpcAccessEvent<AiLocEvents.Op1>(species.open.id) { traps.approach(this, it.loc) }
            for (torch in listOf(BoxTrapObjs.litTorch, BoxTrapObjs.brumaTorch)) onOpLocU(species.open, torch) { traps.smoke(this, it.loc) }
            onOpLocU(species.open, NetTrapObjs.bait[species.index]) { traps.bait(this, it.loc, NetTrapObjs.bait[species.index]) }
            onOpHeld5(species.product) { invDel(inv, species.product, 1, it.slot) }
        }
        onOpHeld5(NetTrapObjs.grownTecu) { invDel(inv, NetTrapObjs.grownTecu, 1, it.slot) }
        onAiConTimer(NetTrapController.trap) { traps.tick(controller) }
        onPlayerLogout { traps.clear(player) }
    }
}
