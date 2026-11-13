package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

class SawmillTest {
    @Test
    fun GameTestState.`Varrock mill charges per log and honours vouchers on the NPC operation`() =
        runGameTest(Sawmill::class) {
            player.members = true
            val pos = CoordGrid(3302, 3492)
            allocZoneCollision(pos)
            val operator =
                spawnNpc(pos, npcTypes.values.single { it.internalName == "poh_sawmill_opp" })
            player.placeAt(pos.translateZ(-1))
            player.inv[0] = InvObj(objs.logs)
            player.inv[1] = InvObj(objs.logs)
            player.inv[2] = InvObj(objs.coins, 199)
            player.inv[3] = InvObj(objs.sawmill_voucher)
            player.opNpc3(operator)
            advance(2)
            val modal =
                requireNotNull(
                    player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id))
                )
            player.ui.queueClose(modal)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a, 2))
            advance()
            assertEquals(2, player.count(objs.plank))
            assertEquals(1, player.count(objs.logs))
            assertEquals(99, player.count(objs.coins))
            assertEquals(0, player.count(objs.sawmill_voucher))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SawmillPlank))
        }
}
