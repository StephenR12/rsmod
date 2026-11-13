package org.rsmod.content.skills.construction

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

internal object ConstructionNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ConstructionNpcSpawns>("construction-suppliers.toml")
        resourceFile<ConstructionNpcSpawns>("mahogany-contractors.toml")
    }
}
