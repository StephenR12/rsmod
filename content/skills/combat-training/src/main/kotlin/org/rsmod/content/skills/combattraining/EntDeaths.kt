package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.EntTrunkOwnership
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EntNpcs : NpcReferences() {
    val guild = find("wcguild_ent")
    val wilderness = find("wilderness_ent")
    val trunk = find("ent_trunk")
}

internal object EntPopulation : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<EntDeaths>("ents.toml")
    }
}

internal object EntStats : NpcEditor() {
    init {
        for (type in listOf(EntNpcs.guild, EntNpcs.wilderness)) {
            edit(type) {
                val wild = type == EntNpcs.wilderness
                hitpoints = if (wild) 105 else 75
                attack = if (wild) 80 else 75
                strength = if (wild) 95 else 75
                defence = 75
                magic = 1
                ranged = 1
                respawnRate = 51
                param[params.attackrate] = 4
                param[params.elemental_weakness_type] = constants.elemental_weakness_fire
                param[params.elemental_weakness_percent] = 40
            }
        }
    }
}

public class EntDeaths
@Inject
constructor(
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val ownership: EntTrunkOwnership,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            if (npc.id != EntNpcs.guild.id && npc.id != EntNpcs.wilderness.id) return@onEvent
            val uuid = player.uuid ?: return@onEvent
            val trunk = Npc(types[EntNpcs.trunk], npc.coords)
            npcs.add(trunk, 100)
            ownership.assign(trunk, uuid)
        }
        onEvent<NpcStateEvents.Delete> { ownership.forget(npc) }
    }
}
