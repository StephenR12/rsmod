package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RegionalShortcutsTest {
    @Test
    fun GameTestState
        .`both sides of the Karamja island swing and lava stones work at their native map anchors`() =
        runGameTest(RegionalShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 12)
            fun target(name: String, x: Int, z: Int, shape: Int, angle: Int) =
                placeMapLoc(
                    CoordGrid(x, z),
                    locTypes.values.single { it.internalName == name },
                    LocShape[shape],
                    LocAngle[angle],
                )
            val east = target("tree_ropeswing1", 2705, 3209, 10, 1)
            val west = target("tree_ropeswing2", 2703, 3205, 10, 3)
            val north = target("karam_dungeon_stone1", 2649, 9561, 22, 3)
            val south = target("karam_dungeon_stone2", 2647, 9558, 22, 3)
            player.teleport(CoordGrid(2710, 3209))
            player.opLoc1(east)
            advance(9)
            assertEquals(CoordGrid(2704, 3209), player.coords)
            val island = DiaryTask.SwingMossGiantIsland
            assertTrue(island.region.tasks(island.difficulty)[island.number - 1].isComplete(player))
            player.teleport(CoordGrid(2704, 3205))
            player.opLoc1(west)
            advance(9)
            assertEquals(CoordGrid(2709, 3205), player.coords)
            player.teleport(CoordGrid(2649, 9562))
            player.opLoc1(north)
            advance(8)
            assertEquals(CoordGrid(2647, 9557), player.coords)
            val lava = DiaryTask.JumpBrimhavenLava
            assertTrue(lava.region.tasks(lava.difficulty)[lava.number - 1].isComplete(player))
            player.setCurrentLevel(stats.agility, 1)
            player.opLoc1(south)
            advance(8)
            assertEquals(CoordGrid(2649, 9562), player.coords)
            assertEquals(210, player.statMap.getFineXP(stats.agility))
        }

    @Test
    fun GameTestState.`Musa Point grapple requires worn gear and unlocks its return crossing`() =
        runGameTest(RegionalShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 53)
            player.setCurrentLevel(stats.ranged, 42)
            player.setCurrentLevel(stats.strength, 21)
            val type =
                locTypes.values.single { it.internalName == "xbows_jungletree_karamja_basic" }
            val tree = placeMapLoc(CoordGrid(2873, 3134), type, LocShape[10], LocAngle[3])
            val north = CoordGrid(2878, 3139)
            val south = CoordGrid(2874, 3127)
            player.teleport(north)
            player.opLoc1(tree)
            advance(10)
            assertEquals(north, player.coords)
            val crossbow = objTypes.values.single { it.internalName == "xbows_crossbow_bronze" }
            player.righthand = InvObj(crossbow)
            player.quiver = InvObj(objs.mith_grapple, 2)
            player.teleport(south)
            player.opLoc1(tree)
            advance(10)
            assertEquals(south, player.coords)
            player.teleport(north)
            player.opLoc1(tree)
            advance(10)
            assertEquals(south, player.coords)
            val task = DiaryTask.GrappleKaramjaVolcano
            assertTrue(task.region.tasks(task.difficulty)[task.number - 1].isComplete(player))
            player.opLoc1(tree)
            advance(10)
            assertEquals(north, player.coords)
        }

    @Test
    fun GameTestState
        .`Coal Trucks cross from the mapped bridge and boulder travels only toward the mine`() =
        runGameTest(RegionalShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 49)
            val log = locTypes.values.single { it.internalName == "mine_log_balance1" }
            val west = placeMapLoc(CoordGrid(2599, 3477), log, LocShape[22], LocAngle[1])
            val east = placeMapLoc(CoordGrid(2602, 3477), log, LocShape[22], LocAngle[1])
            player.teleport(CoordGrid(2598, 3477))
            player.opLoc1(west)
            advance(8)
            assertEquals(CoordGrid(2603, 3477), player.coords)
            player.opLoc1(east)
            advance(8)
            assertEquals(CoordGrid(2598, 3477), player.coords)
            val boulder =
                placeMapLoc(
                    CoordGrid(1776, 3883),
                    locTypes.values.single {
                        it.internalName == "archeuus_runestone_shortcut_boulder"
                    },
                    LocShape[10],
                    LocAngle[2],
                )
            player.teleport(CoordGrid(1776, 3884))
            player.opLoc1(boulder)
            advance(8)
            assertEquals(CoordGrid(1776, 3880), player.coords)
            val task = DiaryTask.LeapDenseEssenceBoulder
            assertTrue(task.region.tasks(task.difficulty)[task.number - 1].isComplete(player))
        }
}
