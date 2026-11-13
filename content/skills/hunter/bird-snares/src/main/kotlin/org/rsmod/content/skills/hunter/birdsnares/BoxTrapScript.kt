package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onNpcAccessEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BoxTrapScript @Inject constructor(private val traps: BoxTrap) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(BoxTrapObjs.snare) { traps.lay(this, it.slot) }
        onOpLoc1(BoxTrapLocs.open) { traps.dismantle(this, it.loc) }
        onOpLoc2(BoxTrapLocs.open) { traps.investigate(this, it.loc) }
        onOpLoc1(BoxTrapLocs.broken) { traps.dismantle(this, it.loc) }
        onOpLoc2(BoxTrapLocs.broken) { traps.reset(this, it.loc) }
        for (species in BoxTrapSpeciesList.all) {
            onOpLoc1(species.full) { traps.check(this, it.loc) }
            onOpLoc2(species.full) { traps.reset(this, it.loc) }
        }
        for (torch in listOf(BoxTrapObjs.litTorch, BoxTrapObjs.brumaTorch)) {
            onOpLocU(BoxTrapLocs.open, torch) { traps.smoke(this, it.loc) }
        }
        onNpcAccessEvent<AiLocEvents.Op1>(BoxTrapLocs.open.id) { traps.approach(this, it.loc) }
        onAiConTimer(BoxTrapControllerBuilder.trap) { traps.tick(controller) }
        onPlayerLogout { traps.clear(player) }
    }
}
