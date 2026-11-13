package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

internal object PortableHunterSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<BirdSnareScript>("portable-hunter-creatures.toml") }
}
