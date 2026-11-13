package org.rsmod.content.skills.farming

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

internal object FarmingNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<FarmingNpcSpawns>("farming-suppliers.toml")
        resourceFile<FarmingNpcSpawns>("farming-gardeners.toml")
    }
}
