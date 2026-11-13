package org.rsmod.content.skills.slayer

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SlayerDungeonSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SlayerDungeons>("dungeon-npcs.toml")
    }
}

internal object SlayerDungeonLocs : LocReferences() {
    val cave = find("slayer_dungeon_entrance")
    val kuraskSteps = find("kurask_steps")
    val caveExit = find("slayer_dungeon_exit")
    val basement = find("slayer_tower_dungeon_laddertop")
    val basementExit = find("slayer_tower_dungeon_ladderbottom")
    val firstUp = find("slayer_stairs_lv1")
    val firstDown = find("slayer_stairs_lv1_top")
    val secondUp = find("slayer_stairs_lv2")
    val secondDown = find("slayer_stairs_lv2_top")
    val door = find("slayertower_small_door")
    val openDoor = find("slayertower_small_door_open")
    val left = find("slayertower_small_door_l")
    val openLeft = find("slayertower_small_door_open_l")
    val right = find("slayertower_small_door_r")
    val openRight = find("slayertower_small_door_open_r")
    val entrance = find("slayertower_door")
    val entranceOpen = find("slayertower_door_open")
    val entranceMirror = find("slayertower_door_mirror")
    val entranceMirrorOpen = find("slayertower_door_open_mirror")
}

internal object SlayerDungeonDoors : LocEditor() {
    init {
        with(SlayerDungeonLocs) {
            for ((closed, open) in listOf(door to openDoor)) {
                edit(closed) {
                    param[params.next_loc_stage] = open
                    contentGroup = content.closed_single_door
                }
                edit(open) {
                    param[params.next_loc_stage] = closed
                    contentGroup = content.opened_single_door
                }
            }
            for ((closed, open) in listOf(left to openLeft, entrance to entranceOpen)) {
                edit(closed) {
                    param[params.next_loc_stage] = open
                    contentGroup = content.closed_left_door
                }
                edit(open) {
                    param[params.next_loc_stage] = closed
                    contentGroup = content.opened_left_door
                }
            }
            for ((closed, open) in
                listOf(right to openRight, entranceMirror to entranceMirrorOpen)) {
                edit(closed) {
                    param[params.next_loc_stage] = open
                    contentGroup = content.closed_right_door
                }
                edit(open) {
                    param[params.next_loc_stage] = closed
                    contentGroup = content.opened_right_door
                }
            }
        }
    }
}

class SlayerDungeons : PluginScript() {
    override fun ScriptContext.startup() {
        with(SlayerDungeonLocs) {
            // The upper room's bridge tiles and the clickable steps both resolve to plane zero.
            onOpLoc1(kuraskSteps) {
                if (player.members)
                    telejump(CoordGrid(2703, if (player.coords.z >= 9990) 9989 else 9991))
            }
            for ((loc, landing) in
                mapOf(
                    cave to CoordGrid(2808, 10002),
                    caveExit to CoordGrid(2796, 3615),
                    basement to CoordGrid(3412, 9932, 3),
                    basementExit to CoordGrid(3417, 3536),
                    firstUp to CoordGrid(3433, 3537, 1),
                    firstDown to CoordGrid(3438, 3537),
                    secondUp to CoordGrid(3417, 3541, 2),
                    secondDown to CoordGrid(3412, 3540, 1),
                )) {
                onOpLoc1(loc) { if (player.members) telejump(landing) }
            }
        }
    }
}
