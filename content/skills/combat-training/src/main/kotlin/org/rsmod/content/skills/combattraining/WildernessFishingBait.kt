package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DarkBait : ObjReferences() {
    val bait = find("wilderness_fishing_bait")
}

internal object BaitGroundSpawns : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<WildernessFishingBait>("wilderness-bait.toml")
    }
}

internal object BaitRespawn : ObjEditor() {
    init {
        edit(DarkBait.bait) { respawnRate = 55 }
    }
}

public class WildernessFishingBait
@Inject
constructor(
    private val random: GameRandom,
    private val objects: ObjRepository,
    private val players: PlayerList,
) : PluginScript() {
    private data class BaitRoll(val slots: Int, val weight: Int, val count: IntRange)

    private val drops =
        mapOf(
            6608 to BaitRoll(100, 33, 60..60),
            7864 to BaitRoll(92, 57, 60..60),
            6604 to BaitRoll(128, 2, 12..12),
            6606 to BaitRoll(128, 2, 10..24),
            6607 to BaitRoll(129, 2, 10..24),
        )

    override fun ScriptContext.startup() {
        onEvent<NpcKillEvents.Credited> {
            val table = drops[npc.id] ?: return@onEvent
            val owner = npc.findHero(players) ?: player
            if (!owner.members || random.of(table.slots) >= table.weight) return@onEvent
            val count =
                if (table.count.first == table.count.last) table.count.first
                else random.of(table.count)
            objects.add(
                DarkBait.bait,
                npc.coords,
                constants.lootdrop_duration,
                owner,
                count = count,
            )
        }
    }
}
