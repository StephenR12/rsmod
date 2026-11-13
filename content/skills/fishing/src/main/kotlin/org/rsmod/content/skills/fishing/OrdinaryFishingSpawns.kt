package org.rsmod.content.skills.fishing

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Published fixed positions; exact concurrent population and ordinary spot relocation remain open.
 */
internal object OrdinaryFishingSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<HighLevelFishingScript>("ordinary-fishing-spots.toml")
    }
}
