package org.rsmod.content.skills.firemaking

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FlamtaerTest {
    @Test
    fun GameTestState.buildLightAndBlessOliveOilWithPersonalSanctity() =
        runGameTest(FlamtaerTemple::class) {
            val pos = CoordGrid(3505, 3314)
            val altar = CoordGrid(3506, 3316)
            val wall = placeMapLoc(pos, locTypes.getValue(4068))
            placeMapLoc(altar, locTypes.getValue(4092))
            player.members = true
            player.stats[stats.crafting] = 99
            random.next = 0
            random.then = 0
            player.inv[0] = InvObj(objTypes.getValue(3678))
            player.inv[1] = InvObj(objs.tinderbox)
            player.inv[2] = InvObj(objTypes.getValue(3422))
            player.inv[3] = InvObj(objTypes.getValue(1941), 50)
            for (i in 0..9) {
                player.inv[4 + i] = InvObj(objTypes.getValue(8837))
                player.inv[14 + i] = InvObj(objTypes.getValue(3420))
            }
            player.telejump(pos.translate(0, -1))
            advance(2)
            player.opLoc1(wall)
            advance(3)
            assertEquals(0, player.vars[cacheTypes.varps.getValue(345)])
            player.setVarp(varps.morttonquest, 85)
            player.opLoc1(wall)
            advance(155)
            assertTrue(player.vars[cacheTypes.varps.getValue(345)] >= 95)
            assertEquals(100, player.vars[cacheTypes.varps.getValue(343)])
            fun fire(id: Int) =
                BoundLocInfo(
                    requireNotNull(findLoc(altar, locTypes.getValue(id))),
                    locTypes.getValue(id),
                )
            player.telejump(altar.translate(0, -1))
            player.opLoc1(fire(4091))
            advance(2)
            assertEquals(1000, player.statMap.getFineXP(stats.firemaking))
            val holy = cacheTypes.varps.getValue(65812)
            val before = player.vars[holy]
            player.opLoc1(fire(4090))
            advance(2)
            assertEquals(before - 36, player.vars[holy])
            assertEquals(1, player.count(objs.sacred_oil_4))
            assertEquals(0, player.count(objTypes.getValue(3422)))
        }

    @Test
    fun GameTestState.incompleteSuppliesAreRetainedAndTheAltarStaysBroken() =
        runGameTest(FlamtaerTemple::class) {
            player.members = true
            player.stats[stats.crafting] = 20
            player.setVarp(varps.morttonquest, 85)
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.plank)
            player.inv[2] = InvObj(objTypes.getValue(1941), 5)
            val pos = CoordGrid(3505, 3314)
            val wall = placeMapLoc(pos, locTypes.getValue(4068))
            player.telejump(pos.translate(0, -1))
            advance(2)
            player.opLoc1(wall)
            advance()
            assertMessageSent(
                "You need a timber beam or plank, a limestone brick and five swamp paste."
            )
            assertEquals(1, player.count(objs.plank))
            assertEquals(5, player.count(objTypes.getValue(1941)))
            assertEquals(0, player.vars[cacheTypes.varps.getValue(344)])
        }

    @Test
    fun GameTestState.razmireOnlyOpensStockedNativeShopsAfterQuest() =
        runGameTest(FlamtaerSupplies::class) {
            player.members = true
            val npc = spawnNpc(CoordGrid(3489, 3296), npcTypes.getValue(1290))
            player.placeAt(npc.coords.translate(0, -1))
            player.opNpc4(npc)
            advance(2)
            assertNull(player.openedShop)
            player.setVarp(varps.morttonquest, 85)
            player.opNpc4(npc)
            advance(2)
            val building = requireNotNull(player.openedShop)
            assertTrue(building.inv.any { it?.id == 8837 && it.count == 1000 })
            player.ifClose()
            advance()
            player.opNpc3(npc)
            advance(2)
            assertTrue(
                requireNotNull(player.openedShop).inv.any { it?.id == 3422 && it.count == 150 }
            )
        }
}
