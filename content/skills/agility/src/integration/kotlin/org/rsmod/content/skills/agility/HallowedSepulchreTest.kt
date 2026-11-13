package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class HallowedSepulchreTest {
    private fun GameTestScope.loc(id: Int, x: Int, z: Int, p: Int, shape: Int, angle: Int) =
        placeMapLoc(CoordGrid(x, z, p), locTypes.getValue(id), LocShape[shape], LocAngle[angle])

    @Test
    fun GameTestState
        .`native mausoleum honours quest stage and stranger gives one allotment of shards`() =
        runGameTest(HallowedSepulchre::class, SepulchreStranger::class) {
            player.members = true
            val door = loc(38574, 3654, 3385, 0, 0, 3)
            player.teleport(CoordGrid(3654, 3384))
            player.setVarBit(cacheTypes.varbits.getValue(7255), 137)
            player.opLoc1(door)
            advance(3)
            assertEquals(CoordGrid(3654, 3384), player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(7255), 138)
            player.opLoc1(door)
            advance(3)
            assertEquals(CoordGrid(2400, 5970), player.coords)
            val stranger = spawnNpc(CoordGrid(2400, 5986), npcTypes.getValue(9806))
            player.teleport(CoordGrid(2400, 5985))
            player.opNpc1(stranger)
            advance(3)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10408)])
            assertEquals(5, player.inv.count(objTypes.getValue(24709)))
            player.opNpc1(stranger)
            advance(2)
            assertEquals(5, player.inv.count(objTypes.getValue(24709)))
        }

    @Test
    fun GameTestState.`the stranger exchanges tools and tokens through both native panels`() =
        runGameTest(HallowedToolStorage::class) {
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(10408), 1)
            val tile = CoordGrid(2400, 5986)
            val stranger = spawnNpc(tile, npcTypes.getValue(9806))
            player.teleport(tile.translate(0, -1))
            val hammer = objTypes.getValue(24727)
            val token = objTypes.getValue(24719)
            player.inv[0] = InvObj(hammer)
            for (slot in 1..8) player.inv[slot] = InvObj(token)
            player.opNpc5(stranger)
            advance(3)
            fun click(name: String) {
                player.ifButton(cacheTypes.components.values.single { it.internalName == name })
                advance(1)
            }
            click("hallowed_tool_side:store_all")
            assertEquals(0, player.inv.count(hammer))
            assertEquals(0, player.inv.count(token))
            click("hallowed_tools:quantity_5")
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(7792)])
            assertEquals(8, player.vars[cacheTypes.varbits.getValue(10592)])
            click("hallowed_tools:token")
            assertEquals(5, player.inv.count(token))
            click("hallowed_tools:withdrawal_all")
            assertEquals(1, player.inv.count(hammer))
            assertEquals(8, player.inv.count(token))
        }

    @Test
    fun GameTestState.`timeout permits floor XP once but prevents descent and late tokens`() =
        runGameTest(HallowedSepulchre::class) {
            player.members = true
            player.setBaseLevel(stats.agility, 92)
            player.setCurrentLevel(stats.agility, 92)
            player.setVarBit(cacheTypes.varbits.getValue(7255), 138)
            player.setVarBit(cacheTypes.varbits.getValue(10408), 1)
            val startingXp = player.statMap.getFineXP(stats.agility)
            val start = loc(38452, 2385, 5981, 0, 10, 2)
            player.teleport(CoordGrid(2388, 5982))
            player.opLoc1(start)
            advance(3)
            val i =
                listOf(
                        CoordGrid(2252, 6018, 2),
                        CoordGrid(2309, 6010, 2),
                        CoordGrid(2293, 5949, 2),
                        CoordGrid(2234, 5960, 2),
                    )
                    .indexOf(player.coords)
            assertTrue(i >= 0, "First floor entry: ${player.coords}")
            val coordinates =
                listOf(
                    CoordGrid(2281, 5988, 2),
                    CoordGrid(2285, 5983, 2),
                    CoordGrid(2259, 5983, 2),
                    CoordGrid(2261, 5988, 2),
                )
            val upper =
                listOf(
                    CoordGrid(2282, 5989, 2),
                    CoordGrid(2285, 5982, 2),
                    CoordGrid(2259, 5982, 2),
                    CoordGrid(2262, 5989, 2),
                )
            val drop =
                coordinates[i].let { loc(38462 + i, it.x, it.z, 2, 10, listOf(0, 3, 3, 0)[i]) }
            player.teleport(upper[i])
            player.opLoc1(drop)
            advance(5)
            assertEquals(
                listOf(
                    CoordGrid(2282, 5985, 1),
                    CoordGrid(2283, 5984, 1),
                    CoordGrid(2261, 5984, 1),
                    CoordGrid(2262, 5985, 1),
                )[i],
                player.coords,
            )
            advance(210)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(10392)])
            player.inv[0] = InvObj(objTypes.getValue(24719))
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(1, player.inv.count(objTypes.getValue(24719)))
            val east = i < 2
            val platform =
                if (east) loc(38458, 2279, 5984, 1, 22, 2) else loc(38456, 2265, 5984, 1, 22, 2)
            player.teleport(CoordGrid(if (east) 2281 else 2263, 5984, 1))
            player.opLoc1(platform)
            advance(5)
            assertEquals(CoordGrid(if (east) 2277 else 2267, 5984, 1), player.coords)
            val stair = loc(39622, if (east) 2274 else 2269, 5983, 1, 10, if (east) 2 else 0)
            player.opLoc1(stair)
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(10394)])
            val obelisk = loc(38451, if (east) 2277 else 2267, 5982, 1, 10, 0)
            player.opLoc3(obelisk)
            advance(5)
            assertEquals(CoordGrid(2400, 5970), player.coords)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(10394)])
            assertEquals(5250, player.statMap.getFineXP(stats.agility) - startingXp)
        }

    private fun GameTestScope.admit(bits: org.rsmod.game.type.varbit.VarBitTypeList) {
        player.members = true
        player.setBaseLevel(stats.agility, 92)
        player.setCurrentLevel(stats.agility, 92)
        player.setVarBit(bits.getValue(7255), 138)
        player.setVarBit(bits.getValue(10408), 1)
        val start = loc(38452, 2385, 5981, 0, 10, 2)
        player.teleport(CoordGrid(2388, 5982))
        random.next = 0
        player.opLoc1(start)
        advance(3)
    }

    @Test
    fun GameTestState
        .`ordered public stairs carry time through all five floors before the final obelisk`() =
        runGameTest(HallowedSepulchre::class) {
            admit(cacheTypes.varbits)
            fun step(
                id: Int,
                at: CoordGrid,
                angle: Int,
                from: CoordGrid,
                to: CoordGrid,
                shape: Int = 10,
                op: Int = 1,
            ) {
                val obj = loc(id, at.x, at.z, at.level, shape, angle)
                player.teleport(from)
                if (op == 3) player.opLoc3(obj) else player.opLoc1(obj)
                advance(5)
                assertEquals(to, player.coords, "$id at $at")
            }
            fun descend(id: Int, at: CoordGrid, angle: Int, from: CoordGrid, to: CoordGrid) {
                random.next = 0
                step(id, at, angle, from, to)
            }
            fun c(x: Int, z: Int, p: Int) = CoordGrid(x, z, p)
            step(38462, c(2281, 5988, 2), 0, c(2282, 5989, 2), c(2282, 5985, 1))
            step(38458, c(2279, 5984, 1), 2, c(2281, 5984, 1), c(2277, 5984, 1), 22)
            descend(39622, c(2274, 5983, 1), 2, c(2277, 5984, 1), c(2528, 5988, 2))
            assertTrue(player.vars[cacheTypes.varbits.getValue(10392)] > 200)
            step(38466, c(2549, 6008, 2), 0, c(2548, 6008, 2), c(2548, 6008, 1))
            step(38457, c(2528, 5991, 1), 1, c(2528, 5993, 1), c(2528, 5989, 1), 22)
            descend(39623, c(2527, 5986, 1), 1, c(2528, 5989, 1), c(2404, 5856, 2))
            step(38471, c(2405, 5827, 2), 1, c(2406, 5827, 2), c(2402, 5827, 1))
            step(38459, c(2405, 5856, 1), 0, c(2405, 5854, 1), c(2403, 5856, 1), 22)
            descend(39624, c(2399, 5858, 1), 1, c(2400, 5857, 1), c(2528, 5860, 2))
            step(38473, c(2543, 5859, 2), 1, c(2544, 5859, 2), c(2544, 5858, 1))
            step(38460, c(2528, 5845, 1), 3, c(2528, 5844, 1), c(2528, 5846, 1), 0)
            descend(39625, c(2527, 5855, 1), 3, c(2527, 5854, 1), c(2272, 5884, 2))
            step(38475, c(2246, 5826, 2), 3, c(2245, 5826, 2), c(2246, 5829, 1))
            step(38476, c(2299, 5831, 1), 0, c(2299, 5832, 1), c(2298, 5831, 0))
            step(38477, c(2272, 5863, 0), 3, c(2272, 5865, 0), c(2272, 5860, 0), 22)
            step(38451, c(2270, 5858, 0), 3, c(2271, 5859, 0), c(2400, 5970, 0), op = 3)
            assertTrue(player.vars[cacheTypes.varbits.getValue(10415)] > 0)
            assertTrue(
                listOf(10406, 10407, 10410, 10411, 10412).all {
                    player.vars[cacheTypes.varbits.getValue(it)] > 0
                }
            )
        }

    @Test
    fun GameTestState
        .`purchased private access copies the real route and lets its owner descend the midfloor obstacle`() =
        runGameTest(HallowedSepulchre::class) {
            loc(38462, 2281, 5988, 2, 10, 0)
            player.setVarBit(cacheTypes.varbits.getValue(10751), 1)
            player.setVarBit(cacheTypes.varbits.getValue(10752), 1)
            admit(cacheTypes.varbits)
            val entry = player.coords
            assertNotEquals(CoordGrid(2252, 6018, 2), entry)
            val placed = entry.translate(29, -30)
            val type = locTypes.getValue(38462)
            val copy = org.rsmod.game.loc.BoundLocInfo(checkNotNull(findLoc(placed, type)), type)
            player.teleport(placed.translate(1, 1))
            player.opLoc1(copy)
            advance(5)
            assertEquals(CoordGrid(entry.x + 30, entry.z - 33, 1), player.coords)
        }

    @Test
    fun GameTestState
        .`a repaired bridge opens its coffin once and lets the player return after time expires`() =
        runGameTest(HallowedSepulchre::class) {
            admit(cacheTypes.varbits)
            player.setBaseLevel(stats.construction, 56)
            player.setCurrentLevel(stats.construction, 56)
            player.setBaseLevel(stats.thieving, 66)
            player.setCurrentLevel(stats.thieving, 66)
            for ((slot, id) in listOf(24727, 8794, 960, 960, 1539).withIndex()) player.inv[slot] =
                InvObj(objTypes.getValue(id), if (id == 1539) 5 else 1)
            val bridge = loc(39527, 2262, 5980, 1, 22, 3)
            val reverse = loc(39527, 2262, 5976, 1, 22, 1)
            val coffin = loc(39544, 2272, 5972, 1, 10, 2)
            player.teleport(CoordGrid(2262, 5981, 1))
            player.opLoc1(bridge)
            advance(5)
            assertEquals(CoordGrid(2262, 5975, 1), player.coords)
            assertEquals(0, player.inv.count(objTypes.getValue(960)))
            player.teleport(CoordGrid(2271, 5972, 1))
            random.next = 1
            player.opLoc1(coffin)
            advance(5)
            assertEquals(1, player.inv.count(objTypes.getValue(24711)))
            player.opLoc1(coffin)
            advance(5)
            assertEquals(1, player.inv.count(objTypes.getValue(24711)))
            advance(200)
            player.teleport(CoordGrid(2262, 5975, 1))
            player.opLoc1(reverse)
            advance(5)
            assertEquals(CoordGrid(2262, 5981, 1), player.coords)
        }
}
