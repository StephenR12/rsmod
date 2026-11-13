package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class PrifddinasCourseTest {
    @Test
    fun GameTestState
        .`a portal lap walks between the bank and treetops and awards its single crystal shard`() =
        runGameTest(PrifddinasCourse::class) {
            player.members = true
            player.setCurrentLevel(stats.agility, 91)
            val startXp = player.statMap.getFineXP(stats.agility)
            fun native(id: Int, x: Int, z: Int, p: Int, angle: Int, shape: Int = 10) =
                placeMapLoc(
                    CoordGrid(x, z, p),
                    locTypes.getValue(id),
                    LocShape[shape],
                    LocAngle[angle],
                )
            val ladder = native(36221, 3254, 6109, 0, 0)
            val portal = native(36241, 3257, 6111, 2, 3)
            val chimney = native(36227, 3273, 6107, 2, 1)
            val roof = native(36228, 3269, 6116, 2, 1)
            val hole = native(36229, 3269, 6118, 0, 1)
            val tree = native(36231, 3294, 6145, 0, 1)
            val bridge = native(36233, 3288, 6142, 2, 2)
            val rope = native(36234, 3277, 6142, 2, 0)
            val otherBridge = native(36235, 3270, 6151, 2, 2)
            val otherRope = native(36236, 3267, 6161, 2, 0)
            val lastRope = native(36237, 3277, 6170, 2, 0)
            val finish = native(36238, 3282, 6184, 0, 2)
            player.teleport(CoordGrid(3253, 6109))
            player.setVarBit(varbits.song_of_the_elves, 199)
            player.opLoc1(ladder)
            advance(6)
            assertEquals(CoordGrid(3253, 6109), player.coords)
            player.setVarBit(varbits.song_of_the_elves, 200)
            random.next = 0
            val obstacles =
                listOf(
                    ladder,
                    portal,
                    chimney,
                    roof,
                    hole,
                    tree,
                    bridge,
                    rope,
                    otherBridge,
                    otherRope,
                    lastRope,
                    finish,
                )
            val ends =
                listOf(
                    CoordGrid(3255, 6109, 2),
                    CoordGrid(3272, 6105, 2),
                    CoordGrid(3269, 6112, 2),
                    CoordGrid(3270, 6116),
                    CoordGrid(3293, 6141),
                    CoordGrid(3293, 6144, 2),
                    CoordGrid(3281, 6142, 2),
                    CoordGrid(3270, 6147, 2),
                    CoordGrid(3270, 6158, 2),
                    CoordGrid(3274, 6168, 2),
                    CoordGrid(3284, 6176),
                    CoordGrid(3240, 6109),
                )
            for ((i, obj) in obstacles.withIndex()) {
                player.opLoc1(obj)
                advance(28)
                assertEquals(ends[i], player.coords, "native loc ${obj.id}")
            }
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65782)])
            assertTrue(player.vars[cacheTypes.varps.getValue(65762)] > 0)
            assertEquals(1, player.inv.count(objTypes.getValue(23962)))
            assertEquals(13365, player.statMap.getFineXP(stats.agility) - startXp)
        }
}
