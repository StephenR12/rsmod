package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MasterFarmerScript @Inject constructor(private val pickpocketing: Pickpocketing) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in MasterFarmerSeeds.definition.npcs) onOpNpc3(npc) { pickpocketing.attempt(this, it.npc, MasterFarmerSeeds.definition) }
    }
}
internal object MasterFarmerSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<MasterFarmerScript>("master-farmers.toml") }
}
