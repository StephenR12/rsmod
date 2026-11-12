package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HunterExpertScript
@Inject
constructor(
    private val feldip: FeldipHunterExpertDialogue,
    private val rellekka: RellekkaHunterExpertDialogue,
    private val cape: HunterCapeScript,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(npcs.hunting_jungle_information) { feldip.talk(this, it.npc) }
        onOpNpc1(npcs.hunting_polar_information) { rellekka.talk(this, it.npc) }
        cape.startup(this)
    }
}
