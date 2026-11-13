package org.rsmod.content.areas.city.lumbridge.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.content.areas.city.lumbridge.LumbridgeScript

internal object DiarySkillNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<LumbridgeScript>("diary-skills-npcs.toml")
    }
}
