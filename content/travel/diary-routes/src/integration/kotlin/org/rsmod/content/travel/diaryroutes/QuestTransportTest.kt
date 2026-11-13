package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class QuestTransportTest {
    data class Deps @Inject constructor(val vars: VarpTypeList, val bits: VarBitTypeList)

    @Test
    fun GameTestState.`the ghost captain requires speech and an exact return fare`() =
        runInjectedGameTest(Deps::class, null, QuestTransport::class) { deps ->
            val start = CoordGrid(3702, 3487)
            val island = CoordGrid(3791, 3560)
            allocZoneCollision(island)
            player.members = true
            player.teleport(start)
            val captain = spawnNpc(start.translateX(1), npcTypes.getValue(3005))
            val tokens = objTypes.getValue(4278)
            fun sail() {
                player.opNpc3(captain)
                advance(2)
            }
            player.inv[0] = InvObj(tokens, 25)
            sail()
            assertEquals(start, player.coords)
            assertEquals(25, player.count(tokens))
            player.worn[Wearpos.Front.slot] = InvObj(objTypes.getValue(552))
            player.inv[0] = InvObj(tokens, 24)
            sail()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.VisitDragontooth))
            assertEquals(24, player.count(tokens))
            player.worn[Wearpos.Ring.slot] = InvObj(objTypes.getValue(6465))
            player.setVarBit(deps.bits[217]!!, 8)
            sail()
            assertEquals(island, player.coords)
            assertEquals(14, player.count(tokens))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.VisitDragontooth))
        }

    @Test
    fun GameTestState
        .`the Hollows requires Nature Spirit and Mortton quick boarding spends ten coins`() =
        runInjectedGameTest(Deps::class, null, QuestTransport::class) { deps ->
            player.members = true
            val north = placeMapLoc(CoordGrid(3498, 3377), locTypes.getValue(6970))
            val south = placeMapLoc(CoordGrid(3523, 3284), locTypes.getValue(6969))
            player.teleport(CoordGrid(3497, 3377))
            player.opLoc1(north)
            advance(3)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.BoardHollowsBoat))
            player.setVarp(deps.vars.values.single { it.internalName == "druidspirit" }, 110)
            player.opLoc1(north)
            advance(3)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BoardHollowsBoat))
            assertEquals(CoordGrid(3522, 3285), player.coords)
            player.setVarp(deps.vars[387]!!, 20)
            player.worn[Wearpos.Ring.slot] = InvObj(objTypes.getValue(6465))
            player.inv[0] = InvObj(objs.coins, 10)
            player.teleport(CoordGrid(3522, 3284))
            player.opLoc2(south)
            advance(3)
            assertEquals(CoordGrid(3496, 3379), player.coords)
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`Crandor must be reached through the newly discovered north side of the wall`() =
        runInjectedGameTest(Deps::class, null, QuestTransport::class) { deps ->
            val door =
                placeMapLoc(
                    CoordGrid(2836, 9600),
                    locTypes.getValue(2606),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            val rope = placeMapLoc(CoordGrid(2833, 9657), locTypes.getValue(25213))
            allocZoneCollision(CoordGrid(2833, 3254))
            player.members = true
            player.setVarp(deps.vars[176]!!, 8)
            fun cross(z: Int) {
                player.teleport(CoordGrid(2836, z))
                player.opLoc1(door)
                advance(3)
            }
            fun ascend() {
                player.teleport(CoordGrid(2833, 9656))
                player.opLoc1(rope)
                advance(3)
            }
            cross(9599)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DiscoverCrandorWall))
            cross(9600)
            assertEquals(1, player.vars[deps.bits[3741]!!])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DiscoverCrandorWall))
            ascend()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.VisitCrandorByDungeon))
            cross(9599)
            ascend()
            assertEquals(CoordGrid(2833, 3254), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.VisitCrandorByDungeon))
        }

    @Test
    fun GameTestState
        .`Smokin Joe must refer the player before the Port Sarim manhole permits entry`() =
        runInjectedGameTest(Deps::class, null, QuestTransport::class) { deps ->
            player.members = true
            val entrance = placeMapLoc(CoordGrid(3018, 3232), locTypes.getValue(10321))
            val ladder = placeMapLoc(CoordGrid(2962, 9651), locTypes.getValue(10309))
            player.teleport(CoordGrid(3018, 3231))
            player.setVarBit(deps.bits[1404]!!, 100)
            player.opLoc1(entrance)
            advance(2)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterSarimRatPits))
            player.setVarBit(deps.bits[1404]!!, 105)
            player.opLoc1(entrance)
            advance(2)
            assertEquals(CoordGrid(2962, 9650), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterSarimRatPits))
            player.opLoc1(ladder)
            advance(2)
            assertEquals(CoordGrid(3018, 3231), player.coords)
        }
}
