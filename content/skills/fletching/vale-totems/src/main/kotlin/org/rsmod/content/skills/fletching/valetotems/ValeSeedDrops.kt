package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ValeSeedDrops
@Inject
constructor(
    private val ground: ObjRepository,
    private val items: ObjTypeList,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<Woodcutting.CutLogs> {
            val at = tree.coords
            if (
                !player.members ||
                    at.level != 0 ||
                    at.x !in 1152..1791 ||
                    at.z !in 2752..3391 && !(at.x in 1216..1343 && at.z in 3392..3455)
            )
                return@onEvent
            if (product.id in logs && random.of(400) == 0) {
                ground.add(
                    items.getValue(31018),
                    player.coords,
                    constants.lootdrop_duration,
                    player,
                )
                player.mes("An ent seed falls out of the tree.")
            }
        }
    }

    private val logs = setOf(1521, 1519, 1517, 6333, 6332, 1515, 1513)
}
