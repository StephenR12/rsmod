package org.rsmod.content.skills.crafting.tanning

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

internal object TannerSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<TanningScript>("sbott.toml") }
}
