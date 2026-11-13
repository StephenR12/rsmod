package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RailLocs : LocReferences() {
    val goblinEntrance = find("slice_goblin_station_entrance")
    val dwarfEntrance = find("slice_dwarf_station_entrance")
    val goblinExit = find("slice_underground_wall_exit_goblin")
    val dwarfExit = find("slice_underground_wall_exit_dwarf")
    val cityEntrance = listOf(find("cave_goblin_city_doorl"), find("cave_goblin_city_doorr"))
    val cityExit = find("dorgesh_bonedoor_entrance")
}

internal object RailNpcs : NpcReferences() {
    val inspectors = listOf(find("slice_cave_goblin_inspector"), find("slice_dwarf_inspector"))
}

internal object RailBits : VarBitReferences() {
    val railway = find("slice_quest")
    val city = find("dttd_main")
}

internal object RailTimers : TimerReferences() {
    val departure = find("dorgesh_train_departure")
}

internal object RailAreas : AreaReferences() {
    val carriages = find("dorgesh_train_carriages")
}

internal object RailAreaBuilder : AreaBuilder() {
    init {
        build("dorgesh_train_carriages")
    }
}

private enum class Station(val x: Int, val starts: List<Int>, val platform: CoordGrid) {
    Dorgesh(2480, listOf(5535, 5546), CoordGrid(2483, 5538)),
    Keldagrim(2442, listOf(5519, 5530), CoordGrid(2440, 5522));

    fun aboard(tile: CoordGrid): Boolean =
        tile.level == 0 && tile.x in x..x + 1 && starts.any { tile.z in it until it + 7 }
}

internal object TrainCarriages : MapAreaBuilder() {
    override fun onPackMapTask() {
        // The station models are bridge scenery at plane 1. Walking, NPCs and
        // area triggers use plane 0 on the train and both public platforms.
        area(RailAreas.carriages) {
            for (station in Station.entries) for (z in station.starts) polygon {
                val x = station.x
                for ((dx, dz) in listOf(0 to 0, 1 to 0, 1 to 6, 0 to 6)) vertex(
                    VertexCoord.from(CoordGrid(x + dx, z + dz))
                )
            }
        }
    }
}

public class Railway : PluginScript() {
    override fun ScriptContext.startup() {
        for ((entrance, landing) in
            listOf(
                RailLocs.goblinEntrance to CoordGrid(2488, 5536),
                RailLocs.dwarfEntrance to CoordGrid(2435, 5535),
            )) {
            onOpLoc1(entrance) {
                if (player.members && vars[RailBits.railway] >= 11) telejump(landing)
                else mes("The railway is not yet open to you.")
            }
        }
        onOpLoc1(RailLocs.goblinExit) { telejump(CoordGrid(2696, 5277, 1)) }
        onOpLoc1(RailLocs.dwarfExit) { telejump(CoordGrid(2941, 10179)) }
        for (entrance in RailLocs.cityEntrance) onOpLoc1(entrance) {
            if (player.members && vars[RailBits.city] >= 13) telejump(CoordGrid(2747, 5374))
            else mes("You do not yet have permission to enter Dorgesh-Kaan.")
        }
        onOpLoc1(RailLocs.cityExit) { telejump(CoordGrid(3317, 9602)) }
        for (inspector in RailNpcs.inspectors) onOpNpc1(inspector) {
            if (vars[RailBits.railway] >= 11)
                mes("Your journeys are free. Step into a carriage and wait for departure.")
            else mes("The railway is not yet ready for passengers.")
        }
        onArea(RailAreas.carriages) {
            if (
                player.members &&
                    vars[RailBits.railway] >= 11 &&
                    Station.entries.any { it.aboard(coords) }
            )
                timer(RailTimers.departure, 8)
        }
        onAreaExit(RailAreas.carriages) { clearTimer(RailTimers.departure) }
        onPlayerLogin {
            if (
                player.members &&
                    player.vars[RailBits.railway] >= 11 &&
                    Station.entries.any { it.aboard(player.coords) }
            )
                player.timer(RailTimers.departure, 8)
        }
        onPlayerTimer(RailTimers.departure) {
            clearTimer(RailTimers.departure)
            val from = Station.entries.firstOrNull { it.aboard(coords) } ?: return@onPlayerTimer
            if (!player.members || vars[RailBits.railway] < 11) return@onPlayerTimer
            val outbound = from == Station.Dorgesh
            telejump(if (outbound) Station.Keldagrim.platform else Station.Dorgesh.platform)
            if (outbound) player.completeDiaryTask(DiaryTask.RideDorgeshTrain)
        }
    }
}
