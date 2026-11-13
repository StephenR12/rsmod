package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class DaeyaltMineTest {
    @Test
    fun GameTestState
        .`active deposit rotates after mining and the staircase checks the saved quest`() =
        runGameTest(DaeyaltMine::class) {
            player.members = true
            val rocks =
                listOf(CoordGrid(3671, 9750, 2), CoordGrid(3674, 9765, 2), CoordGrid(3687, 9755, 2))
            val active = locTypes.getValue(39095)
            val deposit = placeMapLoc(rocks.first(), active)
            for (c in rocks.drop(1)) placeMapLoc(c, locTypes.getValue(39094))
            val entrance = placeMapLoc(CoordGrid(3631, 3339), locTypes.getValue(39092))
            player.teleport(CoordGrid(3634, 3340))
            player.opLoc1(entrance)
            advance(3)
            assertEquals(0, player.coords.level)
            player.setVarBit(RunecraftVarbits.sinsOfTheFather, 138)
            player.opLoc1(entrance)
            advance(3)
            assertEquals(CoordGrid(3696, 9764, 2), player.coords)
            player.teleport(rocks.first().translate(-1, 0))
            val shard = objTypes.getValue(24706)
            player.inv[0] = InvObj(objs.bronze_pickaxe)
            player.setBaseLevel(stats.mining, 59)
            player.setCurrentLevel(stats.mining, 59)
            player.opLoc1(deposit)
            advance(8)
            assertEquals(0, player.count(shard))
            player.setBaseLevel(stats.mining, 60)
            player.setCurrentLevel(stats.mining, 60)
            player.opLoc1(deposit)
            advance(24)
            assertTrue(player.count(shard) >= 2)
            player.moveGameClick(player.coords.translate(-2, 0))
            advance(6)
            val before = player.count(shard)
            advance(110)
            assertEquals(before, player.count(shard))
            assertFalse(locExists(rocks.first(), active))
            assertEquals(1, rocks.count { locExists(it, active) })
            val exit = placeMapLoc(CoordGrid(3696, 9765, 2), locTypes.getValue(39093))
            player.teleport(CoordGrid(3696, 9764, 2))
            player.opLoc1(exit)
            advance(3)
            assertEquals(CoordGrid(3634, 3340), player.coords)
        }

    @Test
    fun GameTestState
        .`Noranna converts the chosen shard quantity and respects the bank capacity`() =
        runGameTest(DaeyaltMine::class) {
            player.teleport(CoordGrid(3692, 9763, 2))
            player.members = true
            val npc = spawnNpc(CoordGrid(3692, 9764, 2), npcTypes.getValue(9822))
            advance(2)
            player.setVarBit(RunecraftVarbits.sinsOfTheFather, 138)
            val intro = cacheTypes.varbits.getValue(10448)
            player.opNpc1(npc)
            advance()
            assertEquals(0, player.vars[intro])
            assertMessageSent(
                "Noranna refuses to deal with you. You need your full vyre noble outfit."
            )
            for ((slot, id) in listOf(4 to 24794, 7 to 24800, 10 to 24680)) player.worn[slot] =
                InvObj(objTypes.getValue(id))
            player.opNpc1(npc)
            advance(2)
            assertEquals(1, player.vars[intro])
            val shard = objTypes.getValue(24706)
            player.inv[0] = InvObj(shard, 12)
            player.setVarBit(varbits.bank_capacity, 1)
            player.withProtectedAccess { bank[0] = InvObj(objs.beer) }
            fun convert() {
                player.opNpc3(npc)
                advance(3)
                player.resumeCountDialog(7)
                advance()
            }
            convert()
            assertEquals(12, player.count(shard))
            player.withProtectedAccess { bank[0] = null }
            convert()
            assertEquals(5, player.count(shard))
            player.withProtectedAccess {
                assertEquals(7, invTotal(bank, RunecraftObjs.daeyaltEssence))
            }
            player.setVarBit(cacheTypes.varbits.getValue(1777), 2)
            convert()
            assertEquals(0, player.count(shard))
            assertEquals(5, player.count(RunecraftObjs.daeyaltEssence))
        }
}
