package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.volcanic.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class VolcanicMineTest {
    @Test
    fun GameTestState.entryChambersBoulderAndEscapeSettleTheRoundOnce() =
        runGameTest(VolcanicMine::class, PetrifiedPete::class) {
            player.members = true
            player.stats[stats.mining] = 85
            val entry =
                placeMapLoc(CoordGrid(3813, 3807), locTypes.getValue(31031), angle = LocAngle.South)
            player.inv[0] = InvObj(objTypes.getValue(21555), 30)
            player.inv[1] = InvObj(objTypes.getValue(1275))
            player.telejump(entry.coords.translate(3, 1))
            player.opLoc1(entry)
            advance(8)
            assertEquals(30, player.count(objTypes.getValue(21555)))
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            player.opLoc1(entry)
            advance(8)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertEquals(CoordGrid(3808, 10193, 3), player.coords)
            assertEquals(0, player.count(objTypes.getValue(21555)))
            assertEquals(1, player.count(objTypes.getValue(21539)))
            val stairs =
                placeMapLoc(
                    CoordGrid(3802, 10159, 3),
                    locTypes.getValue(31029),
                    angle = LocAngle.South,
                )
            player.telejump(stairs.coords.translate(1, -1))
            player.opLoc1(stairs)
            advance(6)
            assertEquals(3, player.level)
            advance(50)
            player.opLoc1(stairs)
            advance(6)
            assertEquals(CoordGrid(3803, 10158, 1), player.coords)
            val vent =
                placeMapLoc(
                    CoordGrid(3783, 10138, 1),
                    locTypes.getValue(31040),
                    shape = LocShape.GroundDecor,
                )
            player.telejump(vent.coords.translateX(-1))
            player.opLoc1(vent)
            advance(3)
            player.opLoc1(vent)
            advance(3)
            assertEquals(50, player.vars[cacheTypes.varbits.getValue(5934)])
            val chamber = placeMapLoc(CoordGrid(3785, 10156, 1), locTypes.getValue(31044))
            player.telejump(chamber.coords.translateX(-1))
            player.opLoc1(chamber)
            advance(3)
            assertEquals(100, player.vars[cacheTypes.varbits.getValue(5934)])
            assertNotNull(findLoc(chamber.coords, locTypes.getValue(31043)))
            val rock = placeMapLoc(CoordGrid(3784, 10158, 1), locTypes.getValue(31045))
            player.telejump(rock.coords.translateX(-1))
            player.opLoc1(rock)
            advance(3)
            assertEquals(1, player.count(objTypes.getValue(21540)))
            player.telejump(chamber.coords.translateX(-1))
            player.opLoc1(
                BoundLocInfo(
                    requireNotNull(findLoc(chamber.coords, locTypes.getValue(31043))),
                    locTypes.getValue(31043),
                )
            )
            advance(3)
            assertEquals(150, player.vars[cacheTypes.varbits.getValue(5934)])
            assertEquals(0, player.count(objTypes.getValue(21540)))
            val boulder =
                BoundLocInfo(
                    requireNotNull(findLoc(CoordGrid(3804, 10124, 1), locTypes.getValue(31034))),
                    locTypes.getValue(31034),
                )
            player.telejump(boulder.coords.translate(-1, 1))
            random.next = 0
            random.then = 0
            random.then = 0
            random.then = 1
            random.then = 80
            player.opLoc1(boulder)
            advance(9)
            assertTrue(player.vars[cacheTypes.varbits.getValue(5934)] > 150)
            assertTrue((21532..21538).any { player.count(objTypes.getValue(it)) > 0 })
            val points = player.vars[cacheTypes.varbits.getValue(5934)]
            val before = player.statMap.getFineXP(stats.mining)
            val rope = placeMapLoc(CoordGrid(3807, 10194, 3), locTypes.getValue(31032))
            player.telejump(rope.coords.translateX(-1))
            player.opLoc1(rope)
            advance(3)
            assertEquals(CoordGrid(3815, 3808), player.coords)
            assertEquals(0, player.count(objTypes.getValue(21539)))
            assertEquals(points, player.vars[cacheTypes.varps.getValue(65825)])
            assertEquals(points * 38, player.statMap.getFineXP(stats.mining) - before)
            assertNull(findLoc(boulder.coords, locTypes.getValue(31034)))
        }

    @Test
    fun GameTestState.nativeRewardShopChargesDeliveredGoodsAndUnlocksTheVesselOnce() =
        runGameTest(PetrifiedPete::class) {
            player.members = true
            player.setVarp(cacheTypes.varps.getValue(65825), 11000)
            val pete = spawnNpc(CoordGrid(3815, 3808), npcTypes.getValue(7776))
            player.telejump(pete.coords.translateX(-1))
            player.opNpc4(pete)
            advance(2)
            assertModalOpen(cacheTypes.interfaces.getValue(612))
            fun buy(row: Int, id: Int, op: IfButtonOp) {
                player.ifButton(
                    cacheTypes.components.getValue((612 shl 16) or 3),
                    comsub = row,
                    obj = id,
                    op = op,
                )
                advance()
            }
            buy(11, 21539, IfButtonOp.Op2)
            buy(11, 21539, IfButtonOp.Op2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(5959)])
            assertEquals(1000, player.vars[cacheTypes.varps.getValue(65825)])
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            buy(0, 441, IfButtonOp.Op3)
            assertEquals(1000, player.vars[cacheTypes.varps.getValue(65825)])
            player.inv[0] = null
            buy(0, 441, IfButtonOp.Op3)
            assertEquals(850, player.vars[cacheTypes.varps.getValue(65825)])
            assertEquals(5, player.count(objTypes.getValue(441)))
        }
}
