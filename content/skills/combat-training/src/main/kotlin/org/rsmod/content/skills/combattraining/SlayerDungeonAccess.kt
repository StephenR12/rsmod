package org.rsmod.content.skills.combattraining

import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.feet
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonDoors : LocReferences() {
    val ice = find("fai_trapdoor")
    val wyvern = find("wyvern_snow_cave_entrance")
    val wyvernExit = find("wyvern_snow_cave_exit")
    val upper = find("wyvern_steps")
    val shortWest = find("cavewall_shortcut_wyvern_west")
    val shortEast = find("cavewall_shortcut_wyvern_east")
    val fossil = find("fossil_wyvern_cave_entrance")
    val fossilExit = find("fossil_wyvern_cave_exit")
    val fossilTask = find("fossil_mushroom_trapdoor")
    val fossilTaskExit = find("fossil_slayer_cave_task")
    val fossilUp = find("fossil_wyvern_task_stairs_1b")
    val fossilDown = find("fossil_wyvern_task_stairs_1a")
    val fossilUp2 = find("fossil_cave_stairs_2_lower")
    val fossilDown2 = find("fossil_cave_stairs_2_top_dummy")
    val lift = listOf(find("brimstone_dungeon_entry"), find("brimstone_elevator_central_tile"))
    val liftOut = find("brimstone_dungeon_exit")
    val rocks = find("karuulm_safe_stepover")
    val gap = find("karuulm_lava_jump")
    val waste = find("pog_canyon_sewer_entrance_lower_02")
    val wasteExit =
        listOf("01", "02", "03", "01_mirror", "02_mirror", "03_mirror").map {
            find("pog_sewer_pipe_exit_bottom_$it")
        }
    val ladder = find("pog_sewer_pipe_side_ladder")
    val ladderTop = find("pog_sewer_ladder_top")
    val grate = find("pog_sewer_grate_door_closed")
    val village = find("roving_golrie_ladder_to_cellar")
    val villageBack = find("roving_golrie_ladder_from_cellar")
    val tunnel =
        listOf(
            find("pog_cave_wall_entrance_golrie_waterfall"),
            find("pog_cave_wall_entrance_golrie_roving"),
        )
    val tunnelOut = find("pog_cave_wall_entrance")
    val chest = find("pog_golrie_big_chest")
    val bowl =
        listOf(
            find("pog_singing_bowl"),
            find("pog_singing_bowl_sing"),
            find("pog_singing_bowl_inspect"),
        )
}

internal object DungeonQuest : VarBitReferences() {
    val voyage = find("fossilquest_progress")
    val path = find("pog")
    val elite = find("kourend_elite_reward")
}

internal object DungeonTools : ObjReferences() {
    val seed = find("crystal_chime_seed")
    val chime = find("crystal_chime")
}

internal object DungeonHeat : TimerReferences() {
    val floor = find("karuulm_floor_heat")
}

class SlayerDungeonAccess : PluginScript() {
    override fun ScriptContext.startup() {
        with(DungeonDoors) {
            onOpLoc1(ice) { telejump(CoordGrid(3007, 9550)) }
            // Other cellar ladders retain the generic content-group handler.
            onOpLoc1(wyvern) { if (player.members) telejump(CoordGrid(3056, 9555)) }
            onOpLoc1(wyvernExit) { telejump(CoordGrid(3056, 9563)) }
            onOpLoc1(upper) {
                telejump(CoordGrid(3060, if (player.coords.z <= 9556) 9559 else 9554))
            }
            onOpLoc1(shortWest) { shortcut(CoordGrid(3024, 9553)) }
            onOpLoc1(shortEast) { shortcut(CoordGrid(3013, 9549)) }
            onOpLoc1(fossil) { fossilTravel(CoordGrid(3604, 10230)) }
            onOpLoc1(fossilExit) { telejump(CoordGrid(3746, 3780)) }
            onOpLoc1(fossilTask) { fossilTravel(CoordGrid(3596, 10290)) }
            onOpLoc1(fossilTaskExit) { telejump(CoordGrid(3678, 3853)) }
            onOpLoc1(fossilDown) { telejump(CoordGrid(3605, 10287)) }
            onOpLoc1(fossilUp) { telejump(CoordGrid(3603, 10291)) }
            onOpLoc1(fossilUp2) { telejump(CoordGrid(3633, 10260)) }
            onOpLoc1(fossilDown2) { telejump(CoordGrid(3633, 10264)) }
            for (control in lift) onOpLoc1(control) {
                if (player.members) {
                    telejump(CoordGrid(1311, 10187))
                    player.softTimer(DungeonHeat.floor, 1)
                }
            }
            onOpLoc1(liftOut) {
                telejump(CoordGrid(1311, 3806))
                player.clearSoftTimer(DungeonHeat.floor)
            }
            onOpLoc1(rocks) {
                val c = it.loc.coords
                val dest =
                    when (c.x) {
                        1302 -> CoordGrid(if (player.coords.x >= 1303) 1301 else 1304, 10205)
                        1321 -> CoordGrid(if (player.coords.x <= 1320) 1323 else 1320, 10205)
                        else -> CoordGrid(1311, if (player.coords.z <= 10214) 10217 else 10214)
                    }
                if (
                    !safeFloor(dest) &&
                        !protectedFeet(player) &&
                        !choice2("Proceed onto the burning floor.", true, "Stay here.", false)
                )
                    return@onOpLoc1
                telejump(dest)
                player.softTimer(DungeonHeat.floor, 1)
            }
            onOpLoc1(gap) {
                val c = it.loc.coords
                telejump(CoordGrid(c.x + 1, if (player.coords.z < c.z + 2) c.z + 4 else c.z - 1))
            }
            onOpLoc1(waste) { if (pathComplete()) telejump(CoordGrid(1473, 4238)) }
            for (exit in wasteExit) onOpLoc1(exit) { telejump(CoordGrid(2321, 3100)) }
            onOpLoc1(ladder) {
                val c = it.loc.coords
                telejump(
                    if (it.loc.angle.id == 1) CoordGrid(c.x, c.z + 1, 1)
                    else CoordGrid(c.x + 1, c.z, 1)
                )
            }
            onOpLoc1(ladderTop) {
                val c = it.loc.coords
                telejump(
                    if (it.loc.angle.id == 0) CoordGrid(c.x, c.z - 1) else CoordGrid(c.x - 1, c.z)
                )
            }
            onOpLoc1(grate) {
                val c = it.loc.coords
                val dx = listOf(-1, 0, 1, 0)[it.loc.angle.id]
                val dz = listOf(0, 1, 0, -1)[it.loc.angle.id]
                val outside = c.translate(dx, dz)
                val towardOutside = (player.coords.x - c.x) * dx + (player.coords.z - c.z) * dz
                telejump(if (towardOutside > 0) c else outside)
            }
            onOpLoc1(village) { telejump(CoordGrid(2596, 4435)) }
            onOpLoc1(villageBack) { telejump(CoordGrid(2532, 3155)) }
            for (door in tunnel) onOpLoc1(door) {
                if (pathComplete()) telejump(CoordGrid(2789, 4242))
            }
            onOpLoc1(tunnelOut) { telejump(CoordGrid(2608, 4450)) }
            onOpLoc1(chest) {
                if (pathComplete() && DungeonTools.seed !in inv && DungeonTools.chime !in inv) {
                    if (invAdd(inv, DungeonTools.seed).success)
                        mes("You find a crystal chime seed.")
                }
            }
            for (b in bowl) {
                onOpLoc1(b) { sing() }
                onOpLocU(b, DungeonTools.seed) { sing() }
            }
        }
        onPlayerLogin { if (inside(player.coords)) player.softTimer(DungeonHeat.floor, 1) }
        onPlayerSoftTimer(DungeonHeat.floor) {
            if (!inside(player.coords)) player.clearSoftTimer(DungeonHeat.floor)
            else if (!safeFloor(player.coords) && !protectedFeet(player))
                player.queueHit(0, HitType.Typeless, 4)
        }
    }

    private fun ProtectedAccess.fossilTravel(c: CoordGrid) {
        if (player.members && vars[DungeonQuest.voyage] >= 50) telejump(c)
        else mes("You must complete Bone Voyage first.")
    }

    private fun ProtectedAccess.pathComplete(): Boolean {
        if (player.members && vars[DungeonQuest.path] >= 50) return true
        mes("You must complete The Path of Glouphrie first.")
        return false
    }

    private fun ProtectedAccess.sing() {
        if (!pathComplete() || DungeonTools.seed !in inv) return
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, DungeonTools.seed.id, 1)
                add(bag, DungeonTools.chime.id, 1)
            }
        if (result.success) mes("The crystal resonates and takes the shape of a chime.")
    }

    private fun ProtectedAccess.shortcut(dest: CoordGrid) {
        if (player.members && player.stat(stats.agility) >= 82) telejump(dest)
        else mes("You need 82 Agility to squeeze through here.")
    }

    private fun inside(c: CoordGrid) = c.x in 1240..1343 && c.z in 10136..10287 && c.level in 0..2

    private fun safeFloor(c: CoordGrid) = c.level == 0 && c.x in 1303..1320 && c.z in 10179..10215

    private fun protectedFeet(p: Player) =
        p.feet?.id in setOf(23037, 22951, 21643) || p.vars[DungeonQuest.elite] != 0
}
