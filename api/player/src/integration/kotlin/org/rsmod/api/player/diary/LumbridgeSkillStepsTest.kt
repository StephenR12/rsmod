package org.rsmod.api.player.diary

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class LumbridgeSkillStepsTest {
    @Test
    fun GameTestState
        .`oak task requires local cutting before local burning and clears its stage`() =
        runGameTest {
            player.members = true
            val local = CoordGrid(3205, 3225)
            val remote = CoordGrid(3090, 3230)
            player.recordLumbridgeOakFire(local)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
            player.recordLumbridgeOakCut(remote)
            assertEquals(0, player.vars[LumbridgeSkillStepVars.oakCut])
            player.recordLumbridgeOakCut(local)
            assertEquals(1, player.vars[LumbridgeSkillStepVars.oakCut])
            player.recordLumbridgeOakFire(remote)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
            assertEquals(1, player.vars[LumbridgeSkillStepVars.oakCut])
            player.recordLumbridgeOakFire(local)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
            assertEquals(0, player.vars[LumbridgeSkillStepVars.oakCut])
        }

    @Test
    fun GameTestState.`free worlds and upstairs actions cannot progress the oak stage`() =
        runGameTest {
            player.members = false
            player.recordLumbridgeOakCut(CoordGrid(3205, 3225))
            assertEquals(0, player.vars[LumbridgeSkillStepVars.oakCut])
            player.members = true
            player.recordLumbridgeOakCut(CoordGrid(3205, 3225, 1))
            assertEquals(0, player.vars[LumbridgeSkillStepVars.oakCut])
        }
}
