package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PitfallScript @Inject constructor(private val traps:PitfallTrap):PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc3(PitfallLocs.empty) { traps.setup(this,it.loc) }
        onOpLoc1(PitfallLocs.set) { traps.jump(this,it.loc) }
        onOpLoc2(PitfallLocs.set) { traps.dismantle(this,it.loc) }
        for(full in PitfallLocs.full + PitfallLocs.completed) onOpLoc2(full) { traps.dismantle(this,it.loc) }
        for(species in PitfallSpeciesList.all) onOpNpc1(species.npc) { traps.tease(this,it.npc,species) }
        for(log in PitfallSpeciesList.logs) {
            onOpLocU(PitfallLocs.empty,log) { traps.setup(this,it.loc) }
            for(full in PitfallLocs.full + PitfallLocs.completed) onOpLocU(full,log) { traps.setup(this,it.loc) }
        }
        onPlayerSoftTimer(PitfallTimers.lure) { traps.tick(player) }
        onAiConTimer(PitfallController.trap) { traps.expire(controller) }
        onPlayerLogout { traps.clear(player) }
    }
}
