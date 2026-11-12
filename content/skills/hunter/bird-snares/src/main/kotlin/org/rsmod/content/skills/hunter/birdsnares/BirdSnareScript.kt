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

public class BirdSnareScript @Inject constructor(private val traps: BirdSnareTrap) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(BirdSnareObjs.snare) { traps.lay(this, it.slot) }

        onOpLoc1(BirdSnareLocs.open) { traps.dismantle(this, it.loc) }
        onOpLoc2(BirdSnareLocs.open) { traps.investigate(this, it.loc) }
        onOpLoc1(BirdSnareLocs.broken) { traps.dismantle(this, it.loc) }
        for (bird in BirdSnareBirds.all) {
            onOpLoc1(bird.full) { traps.check(this, it.loc) }
        }
        for (torch in listOf(BirdSnareObjs.litTorch, BirdSnareObjs.brumaTorch)) {
            onOpLocU(BirdSnareLocs.open, torch) { traps.smoke(this, it.loc) }
        }

        onNpcAccessEvent<AiLocEvents.Op1>(BirdSnareLocs.open.id) {
            traps.birdApproach(this, it.loc)
        }
        onAiConTimer(BirdSnareControllerBuilder.trap) { traps.tick(controller) }
        onPlayerLogout { traps.clear(player) }
    }
}
