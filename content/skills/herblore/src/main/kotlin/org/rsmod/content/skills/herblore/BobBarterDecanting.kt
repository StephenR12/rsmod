package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object DecantNpcs : NpcReferences() {
    val bob = find("ge_expert_herbs")
}

public class BobBarterDecanting @Inject constructor(private val decanting: PotionDecanting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DecantNpcs.bob) { decanting.select(this) }
        onOpNpc4(DecantNpcs.bob) { decanting.select(this) }
    }
}

internal object BobBarterSpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BobBarterDecanting>("decanting.toml")
    }
}
