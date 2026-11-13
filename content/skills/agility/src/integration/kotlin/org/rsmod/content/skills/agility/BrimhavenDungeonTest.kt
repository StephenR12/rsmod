package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class BrimhavenDungeonTest {
    @Test
    fun GameTestState.`northern gate charges once per entry and exits at the surface`() =
        runGameTest(BrimhavenDungeon::class) {
            player.members = true
            fun target(name: String, at: CoordGrid, shape: Int, angle: Int) =
                placeMapLoc(
                    at,
                    locTypes.values.single { it.internalName == name },
                    LocShape[shape],
                    LocAngle[angle],
                )
            val gate = target("dungeon_tree_closed", CoordGrid(2743, 3153), 10, 0)
            val exit = target("karam_dungeon_exit", CoordGrid(2714, 9564), 0, 0)
            val outside = CoordGrid(2744, 3152)
            player.teleport(outside)
            player.inv[0] = InvObj(objs.coins, 874)
            player.opLoc1(gate)
            advance(5)
            assertEquals(874, player.inv.count(objs.coins))
            assertEquals(outside, player.coords)
            player.inv[0] = InvObj(objs.coins, 876)
            player.opLoc1(gate)
            advance(5)
            assertEquals(1, player.inv.count(objs.coins))
            assertEquals(outside, player.coords)
            player.opLoc1(gate)
            advance(6)
            assertEquals(CoordGrid(2713, 9564), player.coords)
            player.opLoc1(exit)
            advance(6)
            assertEquals(outside, player.coords)
        }

    @Test
    fun GameTestState
        .`inward cutting needs the level and axe before western stairs grant credit`() =
        runGameTest(BrimhavenDungeon::class) {
            player.members = true
            fun target(name: String, at: CoordGrid, angle: Int) =
                placeMapLoc(
                    at,
                    locTypes.values.single { it.internalName == name },
                    LocShape[10],
                    LocAngle[angle],
                )
            fun complete(task: DiaryTask) =
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player)
            val vines = target("karam_dungeon_vineblocking1", CoordGrid(2690, 9564), 3)
            player.teleport(CoordGrid(2691, 9564))
            player.opLoc1(vines)
            advance(4)
            val chop = DiaryTask.ChopBrimhavenEntranceVines
            assertFalse(complete(chop))
            player.setCurrentLevel(stats.woodcutting, 10)
            player.opLoc1(vines)
            advance(4)
            assertFalse(complete(chop))
            player.inv[0] = InvObj(objs.bronze_axe)
            player.opLoc1(vines)
            advance(4)
            assertTrue(complete(chop))
            val stairs = target("karam_dungeon_cavestairs2", CoordGrid(2635, 9514), 2)
            val down = target("karam_dungeon_cavestairs_lev2_down2", CoordGrid(2635, 9511, 2), 2)
            player.teleport(CoordGrid(2636, 9517))
            player.opLoc1(stairs)
            advance(5)
            assertEquals(CoordGrid(2636, 9510, 2), player.coords)
            assertTrue(complete(DiaryTask.ClimbBrimhavenDungeonStairs))
            player.opLoc1(down)
            advance(5)
            assertEquals(CoordGrid(2636, 9517), player.coords)
        }
}
