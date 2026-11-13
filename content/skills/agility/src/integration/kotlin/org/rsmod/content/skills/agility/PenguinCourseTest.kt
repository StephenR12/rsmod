package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class PenguinCourseTest {
    @Test
    fun GameTestState
        .`Larry fits the workshop suit and the underwater stones icicles and closing gate award 540 XP`() =
        runGameTest(PenguinCourse::class) {
            player.members = true
            player.observerUUID = 1
            fun tile(x: Int, z: Int, l: Int = 1) = CoordGrid(x, z, l)
            for (l in 0..1) for (x in 2628..2667 step 4) for (z in
                4030..4086 step 4) allocZoneCollision(tile(x, z, l))
            player.setBaseLevel(stats.agility, 80)
            player.setCurrentLevel(stats.agility, 80)
            val quest = cacheTypes.varbits.getValue(3293)
            player.setVarBit(quest, 100)
            val suit = objTypes.getValue(10595)
            val island = tile(2661, 3990)
            allocZoneCollision(island)
            val larry = spawnNpc(island, npcTypes.getValue(829))
            player.inv[0] = InvObj(suit)
            player.teleport(island.translateX(1))
            player.opNpc3(larry)
            advance(4)
            assertEquals(suit.id, player.worn[Wearpos.Back.slot]?.id)
            val start = player.statMap.getFineXP(stats.agility)
            player.teleport(tile(2636, 4054))
            advance(3)
            assertEquals(0, player.coords.level)
            fun cross(
                id: Int,
                x: Int,
                z: Int,
                l: Int,
                from: CoordGrid,
                shape: Int = 22,
                angle: Int = 0,
            ) {
                val loc =
                    placeMapLoc(
                        tile(x, z, l),
                        locTypes.getValue(id),
                        LocShape[shape],
                        LocAngle[angle],
                    )
                player.teleport(from)
                player.opLoc1(loc)
                advance(20)
            }
            cross(21120, 2630, 4057, 0, tile(2630, 4056, 0), 10)
            assertEquals(tile(2630, 4057), player.coords)
            for ((i, pt) in
                listOf(
                        2631 to 4057,
                        2631 to 4059,
                        2633 to 4059,
                        2635 to 4059,
                        2635 to 4061,
                        2635 to 4063,
                        2635 to 4065,
                    )
                    .withIndex()) {
                val id = listOf(21126, 21128, 21129, 21130, 21131, 21132, 21133)[i]
                cross(id, pt.first, pt.second, 1, player.coords)
                assertEquals(tile(pt.first, pt.second), player.coords)
            }
            for ((x, z) in listOf(2644 to 4083, 2652 to 4080, 2658 to 4082, 2662 to 4082)) cross(
                21134,
                x,
                z,
                1,
                tile(x - 1, z),
            )
            cross(21148, 2665, 4077, 1, tile(2665, 4078))
            assertEquals(tile(2656, 4040), player.coords)
            cross(21172, 2652, 4039, 1, tile(2653, 4039), 0)
            assertEquals(tile(2651, 4039), player.coords)
            assertEquals(5400, player.statMap.getFineXP(stats.agility) - start)
        }
}
