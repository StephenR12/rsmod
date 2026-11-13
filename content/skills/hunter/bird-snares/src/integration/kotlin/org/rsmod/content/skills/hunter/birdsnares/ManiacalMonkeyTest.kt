package org.rsmod.content.skills.hunter.birdsnares

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ManiacalMonkeyTest {
    @Test
    fun GameTestState
        .`postquest hunter mounts a gorilla baits a native boulder and takes a regrowing tail`() =
        runGameTest(ManiacalMonkeys::class) {
            player.members = true
            val at = CoordGrid(2910, 9127)
            val boulder = placeMapLoc(at, locTypes.getValue(28824), LocShape[10], LocAngle[0])
            val mount = spawnNpc(at.translateX(-2), npcTypes.getValue(7098))
            player.teleport(at.translateX(-1))
            player.worn[3] = InvObj(objTypes.getValue(19525))
            player.setBaseLevel(stats.hunter, 60)
            player.setCurrentLevel(stats.hunter, 60)
            val before = player.statMap.getFineXP(stats.hunter)
            player.inv[0] = InvObj(objs.banana)
            player.opNpc1(mount)
            advance(2)
            assertNull(player.transmog)
            player.setVarBit(cacheTypes.varbits.getValue(5027), 195)
            player.opNpc1(mount)
            advance(2)
            assertEquals(7099, player.transmog?.id)
            player.opLoc1(boulder)
            advanceUntil({ locExists(at, locTypes.getValue(28827)) }, 30) {
                "locs=" + findLocs(at).map { it.id }.toList()
            }
            assertEquals(0, player.inv.count(objs.banana))
            assertTrue(locExists(at, locTypes.getValue(28827))) {
                findLocs(at).map { it.id }.toList().toString()
            }
            val monkey = spawnNpc(at.translate(-2, 1), npcTypes.getValue(7212))
            monkey.infoProtocol =
                object : NpcInfoProtocol by NoopNpcInfo {
                    override fun isActive() = true
                }
            random.next = 0
            repeat(100) { random.then = 0 }
            advanceUntil({ locExists(at, locTypes.getValue(28830)) }, 100) {
                "monkey=${monkey.coords}, phase=${findLocs(at).map{it.id}.toList()}"
            }
            assertTrue(monkey.transmog?.id in 7214..7216)
            val full =
                BoundLocInfo(
                    checkNotNull(findLoc(at, locTypes.getValue(28830))),
                    locTypes.getValue(28830),
                )
            player.opLoc1(full)
            advance(2)
            assertEquals(1, player.inv.count(objTypes.getValue(19610)))
            assertEquals(10000, player.statMap.getFineXP(stats.hunter) - before)
            assertTrue(locExists(at, locTypes.getValue(28824)))
            advance(40)
            assertNull(monkey.transmog)
        }

    @Test
    fun GameTestState
        .`quest graduate follows the grass bars and lab passage and returns through the ladder`() =
        runGameTest(ManiacalMonkeys::class) {
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(5027), 195)
            player.setVarBit(cacheTypes.varbits.getValue(5033), 1)
            fun use(
                id: Int,
                x: Int,
                z: Int,
                plane: Int,
                angle: Int,
                from: CoordGrid,
                expected: CoordGrid,
            ) {
                val loc =
                    placeMapLoc(
                        CoordGrid(x, z, plane),
                        locTypes.getValue(id),
                        LocShape[10],
                        LocAngle[angle],
                    )
                player.teleport(from)
                player.opLoc1(loc)
                advance(4)
                assertEquals(expected, player.coords, "loc $id")
            }
            use(28810, 2714, 2788, 0, 2, CoordGrid(2714, 2787), CoordGrid(2509, 9173, 1))
            val bars = CoordGrid(2504, 9189, 1)
            use(28724, 2504, 9190, 1, 0, bars, bars)
            player.worn[3] = InvObj(objTypes.getValue(19525))
            use(28724, 2504, 9190, 1, 0, bars, CoordGrid(2504, 9204, 1))
            use(28813, 2510, 9213, 1, 3, CoordGrid(2509, 9213, 1), CoordGrid(2911, 9095))
            use(28683, 2911, 9093, 0, 1, CoordGrid(2911, 9094), CoordGrid(2509, 9213, 1))
            use(28727, 2504, 9202, 1, 2, CoordGrid(2504, 9204, 1), bars)
            use(28676, 2510, 9173, 1, 2, CoordGrid(2509, 9173, 1), CoordGrid(2714, 2787))
        }
}
