package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

class DungeonShortcutsTest {
    @Test
    fun GameTestState.`Taverley pipe enforces its level and works in both directions`() =
        runGameTest(DungeonShortcuts::class) {
            player.members = true
            val type = locTypes.values.single { it.internalName == "taverly_dungeon_pipe_sc" }
            val west = placeMapLoc(CoordGrid(2887, 9799), type)
            val east = placeMapLoc(CoordGrid(2890, 9799), type, angle = LocAngle.East)
            val origin = CoordGrid(2886, 9799)
            player.teleport(origin)
            player.setCurrentLevel(stats.agility, 69)
            player.opLoc1(west)
            advance(8)
            assertEquals(origin, player.coords)
            player.setCurrentLevel(stats.agility, 70)
            player.opLoc1(west)
            advance(8)
            assertEquals(CoordGrid(2891, 9799), player.coords)
            player.opLoc1(east)
            advance(8)
            assertEquals(origin, player.coords)
            assertEquals(200, player.statMap.getFineXP(stats.agility))
        }

    @Test
    fun GameTestState.`a successful jump over the actual strange floor advances the elite task`() =
        runGameTest(DungeonShortcuts::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 99)
            val floor =
                locTypes.values.single { it.internalName == "taverly_dungeon_floor_spikes_sc" }
            val target = placeMapLoc(CoordGrid(2879, 9813), floor)
            player.teleport(CoordGrid(2878, 9813))
            player.opLoc1(target)
            advance(7)
            assertEquals(CoordGrid(2881, 9813), player.coords)
            assertEquals(125, player.statMap.getFineXP(stats.agility))
            val task = DiaryTask.JumpTaverleyStrangeFloor
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
        }
}
