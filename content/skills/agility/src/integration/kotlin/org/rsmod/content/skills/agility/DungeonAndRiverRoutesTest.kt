package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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

class DungeonAndRiverRoutesTest {
    @Test
    fun GameTestState.`both ends of the Edgeville pipe join the sewers`() =
        runGameTest(DungeonShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 99)
            val type = locTypes.values.single { it.internalName == "varrock_dungeon_pipe_sc" }
            val west = placeMapLoc(CoordGrid(3150, 9906), type, LocShape[10], LocAngle[2])
            val east = placeMapLoc(CoordGrid(3153, 9906), type, LocShape[10], LocAngle[0])
            player.teleport(CoordGrid(3149, 9906))
            player.opLoc1(west)
            advance(12)
            assertEquals(CoordGrid(3155, 9906), player.coords)
            val task = DiaryTask.SqueezeEdgevillePipe
            assertTrue(task.region.tasks(task.difficulty)[task.number - 1].isComplete(player))
            player.opLoc1(east)
            advance(12)
            assertEquals(CoordGrid(3149, 9906), player.coords)
        }

    @Test
    fun GameTestState.`native chain level requirement and upper climb grant the tower diary`() =
        runGameTest(SlayerTowerChains::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 70)
            val chain =
                placeMapLoc(
                    CoordGrid(3447, 3576, 1),
                    locTypes.values.single { it.internalName == "slayertower_sc_chainbottom" },
                    LocShape[10],
                    LocAngle[0],
                )
            val down =
                placeMapLoc(
                    CoordGrid(3447, 3576, 2),
                    locTypes.values.single { it.internalName == "slayertower_sc_chaintop" },
                    LocShape[10],
                    LocAngle[0],
                )
            val base = CoordGrid(3447, 3575, 1)
            player.teleport(base)
            player.opLoc1(chain)
            advance(8)
            assertEquals(base, player.coords)
            player.setCurrentLevel(stats.agility, 99)
            player.opLoc1(chain)
            advance(8)
            assertEquals(CoordGrid(3447, 3575, 2), player.coords)
            val task = DiaryTask.ClimbSlayerTowerAdvancedChain
            assertTrue(task.region.tasks(task.difficulty)[task.number - 1].isComplete(player))
            player.opLoc1(down)
            advance(7)
            assertEquals(base, player.coords)
        }

    @Test
    fun GameTestState
        .`River Lum returns in either direction but Water Obelisk goes only to shore`() =
        runGameTest(RegionalShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 36)
            player.setCurrentLevel(stats.ranged, 39)
            player.setCurrentLevel(stats.strength, 22)
            player.righthand =
                InvObj(objTypes.values.single { it.internalName == "xbows_crossbow_bronze" })
            player.quiver = InvObj(objs.mith_grapple, 4)
            val raft =
                placeMapLoc(
                    CoordGrid(3252, 3179),
                    locTypes.values.single { it.internalName == "xbows_raft_br" },
                    LocShape[22],
                    LocAngle[0],
                )
            val east = CoordGrid(3259, 3179)
            val west = CoordGrid(3246, 3179)
            player.teleport(west)
            player.opLoc1(raft)
            advance(13)
            assertEquals(east, player.coords)
            player.opLoc1(raft)
            advance(13)
            assertEquals(west, player.coords)
            val tree =
                placeMapLoc(
                    CoordGrid(2841, 3434),
                    locTypes.values.single {
                        it.internalName == "xbows_beach_to_island_tree_basic"
                    },
                    LocShape[10],
                    LocAngle[1],
                )
            val island = CoordGrid(2842, 3427)
            val shore = CoordGrid(2842, 3433)
            player.teleport(island)
            player.opLoc1(tree)
            advance(10)
            assertEquals(shore, player.coords)
            player.opLoc1(tree)
            advance(10)
            assertEquals(shore, player.coords)
            for (task in
                listOf(
                    DiaryTask.GrappleWaterObeliskIsland,
                    DiaryTask.CrossRiverLumRaft,
                )) assertTrue(
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player)
            )
        }

    @Test
    fun GameTestState.`Trollheim's two ledges join the one-way Wilderness descent`() =
        runGameTest(RegionalShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 64)
            val type = locTypes.values.single { it.internalName == "trollheim_wildy_climb_rocks" }
            fun target(x: Int, z: Int, angle: Int) =
                placeMapLoc(CoordGrid(x, z), type, LocShape[22], LocAngle[angle])
            val west = target(2916, 3672, 2)
            val middle = target(2922, 3672, 2)
            val east = target(2947, 3678, 2)
            player.teleport(CoordGrid(2915, 3672))
            player.opLoc1(west)
            advance(10)
            assertEquals(CoordGrid(2918, 3672), player.coords)
            player.opLoc1(middle)
            advance(14)
            assertEquals(CoordGrid(2924, 3673), player.coords)
            player.opLoc1(east)
            advance(55)
            assertEquals(CoordGrid(2950, 3681), player.coords)
            val task = DiaryTask.ClimbTrollheimIntoWilderness
            assertTrue(task.region.tasks(task.difficulty)[task.number - 1].isComplete(player))
        }
}
