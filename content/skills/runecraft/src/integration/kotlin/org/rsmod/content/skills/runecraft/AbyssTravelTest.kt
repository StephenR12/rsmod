package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class AbyssTravelTest {
    @Test
    fun GameTestState
        .`Wilderness mage applies skull or bracelet after access checks and a clear passage leads to the rifts`() =
        runGameTest(AbyssTravel::class) {
            player.members = true
            player.stats[stats.prayer] = 70
            val wild = CoordGrid(3107, 3555)
            player.teleport(wild.translate(1, 0))
            val mage = spawnNpc(wild, npcTypes.getValue(3228))
            player.setVarp(cacheTypes.varps.getValue(492), 4)
            val bracelet =
                objTypes.values.single { it.internalName == "jewl_runerunning_bracelet_1" }
            player.worn[Wearpos.Hands.slot] = InvObj(bracelet)
            player.setVarBit(varbits.teleblock_cycles, 500)
            player.opNpc4(mage)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterAbyss))
            assertEquals(bracelet.id, player.worn[Wearpos.Hands.slot]?.id)
            player.setVarBit(varbits.teleblock_cycles, 0)
            random.next = 0
            player.opNpc4(mage)
            advance()
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterAbyss))
            assertEquals(CoordGrid(3042, 4810), player.coords)
            assertEquals(0, player.stats[stats.prayer])
            assertNull(player.worn[Wearpos.Hands.slot])
            assertNull(player.skullIcon)

            player.teleport(wild.translate(1, 0))
            player.opNpc4(mage)
            advance()
            assertEquals(constants.skullicon_default, player.skullIcon)
            player.setVarBit(cacheTypes.varbits.getValue(625), 0)
            val wall = CoordGrid(3038, 4853)
            player.teleport(CoordGrid(3039, 4856))
            val passage =
                placeMapLoc(
                    wall,
                    locTypes.getValue(26208),
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.opLoc1(passage)
            advance(3)
            assertEquals(CoordGrid(3039, 4839), player.coords)
            val air = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(25378))
            player.opLoc1(air)
            advance()
            assertEquals(CoordGrid(2841, 4829), player.coords)
        }

    @Test
    fun GameTestState.`law rift enforces the quest and the monks equipment check before travel`() =
        runGameTest(AbyssTravel::class) {
            player.members = true
            val start = player.coords
            val rift = placeMapLoc(start.translate(1, 0), locTypes.getValue(25034))
            player.opLoc1(rift)
            advance()
            assertEquals(start, player.coords)
            player.setVarp(cacheTypes.varps.getValue(317), 50)
            player.inv[15] = InvObj(objTypes.getValue(1205))
            player.opLoc1(rift)
            advance()
            assertEquals(start, player.coords)
            player.inv[15] = null
            player.opLoc1(rift)
            advance()
            assertEquals(CoordGrid(2465, 4817), player.coords)
        }
}
