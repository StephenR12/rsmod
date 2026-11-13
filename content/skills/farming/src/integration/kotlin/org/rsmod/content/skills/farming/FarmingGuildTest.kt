package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class FarmingGuildTest {
    @Test
    fun GameTestState
        .`walking through the main guild door checks Farming and records entry only once`() =
        runGameTest(FarmingGuild::class) {
            player.members = true
            val door =
                locTypes.values.single { it.internalName == "kebos_farming_guild_door_left_closed" }
            val target =
                placeMapLoc(CoordGrid(1248, 3723), door, LocShape.WallStraight, LocAngle.South)
            val outside = CoordGrid(1248, 3722)
            player.placeAt(outside)
            player.setCurrentLevel(stats.farming, 44)
            player.opLoc1(target)
            advance(5)
            assertEquals(outside, player.coords)
            player.setCurrentLevel(stats.farming, 45)
            player.opLoc1(target)
            advance(5)
            assertEquals(CoordGrid(1248, 3724), player.coords)
            val task = DiaryTask.EnterFarmingGuild
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
        }
}
