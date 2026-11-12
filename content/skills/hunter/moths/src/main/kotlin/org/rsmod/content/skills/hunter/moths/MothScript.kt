package org.rsmod.content.skills.hunter.moths

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MothScript @Inject constructor(private val hunting: MothHunting) : PluginScript() {
    override fun ScriptContext.startup() {
        for (species in MothSpeciesList.all) {
            for (npc in species.npcs) {
                onOpNpc1(npc) { hunting.catch(this, it.npc) }
            }
            onOpHeld4(species.jar) { hunting.release(this, it.slot) }
        }
    }
}
