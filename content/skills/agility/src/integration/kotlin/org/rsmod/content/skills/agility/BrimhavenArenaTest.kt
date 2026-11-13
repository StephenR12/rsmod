package org.rsmod.content.skills.agility

import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class BrimhavenArenaTest {
    @Test
    fun GameTestState
        .`paid entry signals two distinct ticket dispensers and the trader exchanges the earned ticket`() =
        runGameTest(BrimhavenArena::class) {
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            fun item(name: String) = objTypes.values.single { it.internalName == name }
            player.members = true
            player.setCurrentLevel(stats.agility, 99)
            random.next = 0
            val clerk =
                spawnNpc(
                    CoordGrid(2808, 3193),
                    npcTypes.values.single { it.internalName == "agilityarena_clerk" },
                )
            val down =
                placeMapLoc(
                    CoordGrid(2809, 3194),
                    loc("agilityarena_ladderdown"),
                    LocShape[10],
                    LocAngle[1],
                )
            player.teleport(CoordGrid(2809, 3193))
            player.opLoc1(down)
            advance(4)
            assertEquals(0, player.coords.level)
            player.inv[0] = InvObj(objs.coins, 199)
            player.opNpc3(clerk)
            advance(3)
            assertEquals(199, player.count(objs.coins))
            player.inv[0] = InvObj(objs.coins, 200)
            player.opNpc3(clerk)
            advance(3)
            assertEquals(0, player.count(objs.coins))
            fun waitArrow(): CoordGrid {
                repeat(110) {
                    advance()
                    val arrow =
                        client.outgoingMessages.filterIsInstance<HintArrow>().lastOrNull()?.type
                            as? HintArrow.TileHintArrow
                    if (arrow != null) return CoordGrid(arrow.x, arrow.z, 3)
                }
                error("Missing ticket dispenser arrow")
            }
            player.opLoc1(down)
            val first = waitArrow()
            assertEquals(CoordGrid(2805, 9589, 3), player.coords)
            val swing =
                placeMapLoc(
                    CoordGrid(2806, 9585, 3),
                    loc("agilityarena_ropeswing"),
                    LocShape[10],
                    LocAngle[0],
                )
            player.opLoc1(swing)
            advance(16)
            assertEquals(CoordGrid(2805, 9581, 3), player.coords)
            assertEquals(200, player.statMap.getFineXP(stats.agility))
            allocZoneCollision(CoordGrid(2772, 9555, 3))
            val spike = CoordGrid(2772, 9551, 3)
            placeMapLoc(spike, loc("agilityarena_floorspikes"), LocShape[10], LocAngle[3])
            player.teleport(CoordGrid(2772, 9549, 3))
            advance(2)
            player.moveGameClick(CoordGrid(2772, 9555, 3))
            advance(16)
            assertEquals(440, player.statMap.getFineXP(stats.agility))
            val ticket = item("agilityarena_ticket_new")
            val voucher = item("agilityarena_voucher")
            fun tag(at: CoordGrid) {
                val target = placeMapLoc(at, loc("agility_ticketpillar"))
                player.teleport(at.translateX(1))
                player.opLoc1(target)
                advance(3)
            }
            tag(first)
            assertEquals(0, player.count(ticket))
            val second = waitArrow()
            assertNotEquals(first, second)
            tag(second)
            assertEquals(1, player.count(ticket))
            assertEquals(1, player.count(voucher))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ClaimBrimhavenArenaTicket))
            val above = placeMapLoc(CoordGrid(2805, 9590, 3), loc("agilityarena_ladderup"))
            player.teleport(above.coords.translateZ(-1))
            player.opLoc1(above)
            advance(4)
            assertEquals(CoordGrid(2809, 3193), player.coords)
            val trader =
                spawnNpc(
                    CoordGrid(2809, 3192),
                    npcTypes.values.single { it.internalName == "agilityarena_tickettrader_2ops" },
                )
            player.worn[9] = InvObj(item("atjun_gloves_med"))
            val before = player.statMap.getFineXP(stats.agility)
            player.opNpc3(trader)
            advance(4)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            player.resumeActiveCoroutine(ResumePCountDialogInput(1))
            assertEquals(0, player.count(ticket))
            assertEquals(before + 3795, player.statMap.getFineXP(stats.agility))
        }
}
