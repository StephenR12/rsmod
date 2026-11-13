package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FarmingGuild @Inject constructor(private val types: LocTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        val names = types.values.associateBy { it.internalName }
        for (side in listOf("left", "right")) {
            val door = names.getValue("kebos_farming_guild_door_${side}_closed")
            onOpLoc1(door) {
                if (it.loc.x !in 1248..1249 || it.loc.z != 3723) return@onOpLoc1
                val entering = coords.z < 3723
                if (entering && (!player.members || stat(stats.farming) < 45)) {
                    mes("You need a Farming level of 45 to enter the guild.")
                    return@onOpLoc1
                }
                faceLoc(it.loc)
                val dest = CoordGrid(it.loc.x, if (entering) 3724 else 3722)
                exactMove(
                    coords,
                    dest,
                    0,
                    60,
                    if (entering) Constants.em_face_north else Constants.em_face_south,
                )
                delay(2)
                if (entering && coords == dest)
                    player.completeDiaryTask(DiaryTask.EnterFarmingGuild)
            }
        }
    }
}
