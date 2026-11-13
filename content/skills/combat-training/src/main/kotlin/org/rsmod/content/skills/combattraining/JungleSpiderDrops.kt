package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object JungleSpiderItems : ObjReferences() {
    val carcass = find("village_spider_carcass")
}

internal object JungleSpiderPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<JungleSpiderDrops>("jungle-spiders.toml")
    }
}

public class JungleSpiderDrops @Inject constructor(private val ground: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            if (npc.id in listOf(3020, 6267, 6271)) {
                ground.add(
                    JungleSpiderItems.carcass,
                    npc.coords,
                    constants.lootdrop_duration,
                    player,
                )
            }
        }
    }
}
