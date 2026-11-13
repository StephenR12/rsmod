package org.rsmod.content.skills.hunter.herbiboar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.hunter.experts.GuildRumours
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class HerbiboarTest {
    @Test
    fun GameTestState
        .`a real Fossil Island trail appears privately then the native multinpc yields herbs only once`() =
        runGameTest(HerbiboarHunting::class, GuildRumours::class) {
            player.members = true
            player.setBaseLevel(stats.hunter, 80)
            player.setCurrentLevel(stats.hunter, 80)
            player.setBaseLevel(stats.herblore, 31)
            player.setCurrentLevel(stats.herblore, 31)
            fun tile(x: Int, z: Int) = CoordGrid(x, z)
            val tracks = (5737..5746) + (5768..5777) + (5747..5750)
            val start =
                placeMapLoc(tile(3686, 3870), locTypes.getValue(30519), LocShape[10], LocAngle[0])
            val guest = registerPlayer(start.coords.translateX(2)).apply { members = true }
            fun inspectStart() {
                player.teleport(start.coords.translateZ(-1))
                player.opLoc1(start)
                advance(2)
            }
            inspectStart()
            assertTrue(tracks.all { player.vars[cacheTypes.varbits.getValue(it)] == 0 })
            player.setVarBit(cacheTypes.varbits.getValue(5795), 50)
            inspectStart()
            assertTrue(tracks.any { player.vars[cacheTypes.varbits.getValue(it)] != 0 })
            assertTrue(tracks.all { guest.vars[cacheTypes.varbits.getValue(it)] == 0 })
            val clueSites =
                listOf(
                    Triple(30540, 3680, 3838),
                    Triple(30547, 3680, 3836),
                    Triple(31479, 3706, 3811),
                    Triple(30533, 3699, 3875),
                    Triple(30534, 3670, 3889),
                    Triple(30535, 3681, 3860),
                    Triple(30536, 3708, 3876),
                    Triple(30539, 3668, 3865),
                    Triple(30541, 3694, 3847),
                    Triple(30543, 3698, 3847),
                    Triple(30544, 3681, 3859),
                    Triple(30545, 3697, 3875),
                    Triple(30546, 3672, 3890),
                    Triple(30549, 3667, 3862),
                    Triple(30551, 3710, 3877),
                    Triple(30537, 3728, 3893),
                    Triple(30542, 3715, 3851),
                    Triple(30550, 3713, 3850),
                    Triple(31480, 3713, 3840),
                )
            val clues =
                clueSites.map { (id, x, z) ->
                    placeMapLoc(tile(x, z), locTypes.getValue(id), LocShape[10], LocAngle[0])
                }
            fun end() = player.vars[cacheTypes.varbits.getValue(5766)]
            repeat(15) {
                for (loc in clues) {
                    if (end() != 0) break
                    player.teleport(loc.coords.translateZ(-1))
                    player.opLoc1(loc)
                    advance(2)
                }
            }
            val found = end()
            assertTrue(found in 1..9)
            val tunnels =
                listOf(
                    tile(3693, 3798),
                    tile(3702, 3808),
                    tile(3703, 3826),
                    tile(3710, 3881),
                    tile(3700, 3877),
                    tile(3715, 3840),
                    tile(3751, 3849),
                    tile(3685, 3869),
                    tile(3681, 3863),
                )
            val tunnel =
                placeMapLoc(tunnels[found - 1], locTypes.getValue(30532), LocShape[10], LocAngle[0])
            val xp = player.statMap.getFineXP(stats.hunter)
            player.teleport(tunnel.coords.translateZ(-1))
            player.opLoc1(tunnel)
            advance(4)
            assertEquals(19500, player.statMap.getFineXP(stats.hunter) - xp)
            assertEquals(found, player.vars[cacheTypes.varbits.getValue(5943)])
            val animals =
                listOf(
                    tile(3696, 3796),
                    tile(3704, 3808),
                    tile(3705, 3827),
                    tile(3710, 3883),
                    tile(3697, 3876),
                    tile(3717, 3840),
                    tile(3753, 3851),
                    tile(3683, 3870),
                    tile(3681, 3865),
                )
            val npc = spawnNpc(animals[found - 1], npcTypes.getValue(7830 + found))
            val near = npc.coords.translateZ(-1)
            guest.teleport(near)
            guest.opNpc1(npc)
            advance(2)
            assertEquals(0, guest.vars[cacheTypes.varps.getValue(65755)])
            // Teco has an active Herbiboar assignment with one catch left before its guaranteed
            // part.
            player.setVarp(cacheTypes.varps.getValue(65756), 26 shl 20)
            player.setVarp(cacheTypes.varps.getValue(65757), (5 shl 2) or (13 shl 15) or 1)
            player.inv[27] = InvObj(objTypes.getValue(7409))
            player.teleport(near)
            player.opNpc1(npc)
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65755)])
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(5943)])
            assertEquals(1, player.inv.count(objTypes.getValue(29239)))
            assertTrue(
                player.inv.count {
                    it?.id in
                        setOf(199, 201, 203, 205, 207, 209, 211, 213, 215, 217, 219, 2485, 3051)
                } in 2..4
            )
            player.opNpc1(npc)
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65755)])
        }
}
