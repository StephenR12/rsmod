package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

class StonemasonTest {
    @Test
    fun GameTestState.`trading at the actual Keldagrim supplier marks the diary`() =
        runGameTest(Stonemason::class) {
            player.members = true
            val trader = npcTypes.values.single { it.internalName == "poh_stonemason" }
            val npc = spawnNpc(CoordGrid(2849, 10185), trader)
            player.teleport(npc.coords.translateX(1))
            player.opNpc3(npc)
            advance(3)
            val task = DiaryTask.BrowseKeldagrimStonemason
            assertEquals(
                true,
                task.region.tasks(task.difficulty)[task.number - 1].isComplete(player),
            )
        }
}
