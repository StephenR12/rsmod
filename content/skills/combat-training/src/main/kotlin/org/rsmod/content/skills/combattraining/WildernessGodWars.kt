package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WildernessDungeonLocs : LocReferences() {
    val entrance = find("wilderness_gwd_entrance")
    val crevice = find("wilderness_gwd_crevice")
    val juttingWall = find("wilderness_gwd_agilityobstacle")
    val returnCrevice = find("wilderness_gwd_crevice2")
}

internal object WildernessDungeonNpcs : NpcReferences() {
    val boulder = find("wilderness_gwd_boulder")
}

class WildernessGodWars : PluginScript() {
    override fun ScriptContext.startup() {
        with(WildernessDungeonLocs) {
            onOpLoc1(entrance) { if (player.members) telejump(CoordGrid(3065, 10159, 3)) }
            onOpLoc1(crevice) {
                when (it.loc.coords) {
                    CoordGrid(3065, 10160, 3) -> telejump(CoordGrid(3017, 3740))
                    CoordGrid(3049, 10165, 3) -> enterDungeon(CoordGrid(3034, 10158))
                    CoordGrid(3066, 10142, 3) -> enterDungeon(CoordGrid(3062, 10130))
                }
            }
            onOpLoc1(returnCrevice) {
                when (it.loc.coords) {
                    CoordGrid(3035, 10158) -> telejump(CoordGrid(3050, 10165, 3))
                    CoordGrid(3062, 10131) -> telejump(CoordGrid(3066, 10143, 3))
                }
            }
            onOpLoc1(juttingWall) {
                if (player.stat(stats.agility) < 60) {
                    mes("You need an Agility level of 60 to pass this wall.")
                    return@onOpLoc1
                }
                val entering = player.coords.z > 10148
                telejump(CoordGrid(3066, if (entering) 10147 else 10149, 3))
                if (entering) player.statAdvance(stats.agility, 5.5)
            }
        }
        onOpNpc1(WildernessDungeonNpcs.boulder) {
            if (player.stat(stats.strength) < 60) {
                mes("You need a Strength level of 60 to move this boulder.")
                return@onOpNpc1
            }
            val entering = player.coords.x > 3053
            telejump(CoordGrid(if (entering) 3052 else 3055, 10165, 3))
            if (entering) player.statAdvance(stats.strength, 5.5)
        }
    }

    private fun ProtectedAccess.enterDungeon(destination: CoordGrid) {
        if (!player.members) return
        telejump(destination)
        player.completeDiaryTask(DiaryTask.EnterWildernessGodWars)
    }
}
