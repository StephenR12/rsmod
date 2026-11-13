package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class StashUnitsTest {
    @Test
    fun GameTestState
        .`a beginner STASH consumes matching nails only once and safely stores a full outfit`() =
        runGameTest(StashUnits::class) {
            player.members = true
            player.setCurrentLevel(stats.construction, 12)
            val xp = player.statMap.getFineXP(stats.construction)
            val pos = CoordGrid(3206, 3422)
            allocZoneCollision(pos)
            player.teleport(pos.translateZ(-1))
            val loc =
                placeMapLoc(pos, locTypes.values.single { it.internalName == "hh_beginner001" })
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "poh_saw" })
            player.inv[2] = InvObj(objs.plank)
            player.inv[3] = InvObj(objs.plank)
            player.inv[4] = InvObj(objs.bronze_nails, 5)
            player.inv[5] = InvObj(objs.steel_nails, 5)
            player.opLoc1(loc)
            advance(6)
            assertEquals(xp, player.statMap.getFineXP(stats.construction))
            player.inv[4] = InvObj(objs.bronze_nails, 10)
            player.opLoc1(loc)
            advance(6)
            assertEquals(xp + 1500, player.statMap.getFineXP(stats.construction))
            assertEquals(0, player.inv.count(objTypes[objs.plank]))
            assertEquals(5, player.inv.count(objTypes[objs.steel_nails]))
            val ring = objTypes.values.single { it.internalName == "gold_ring" }
            val necklace = objTypes.values.single { it.internalName == "gold_necklace" }
            player.inv[2] = InvObj(ring)
            player.opLoc2(loc)
            advance(3)
            assertEquals(1, player.inv.count(ring))
            player.inv[3] = InvObj(necklace)
            player.opLoc2(loc)
            advance(3)
            assertEquals(0, player.inv.count(ring))
            assertEquals(0, player.inv.count(necklace))
            player.opLoc2(loc)
            advance(3)
            assertEquals(1, player.inv.count(ring))
            assertEquals(1, player.inv.count(necklace))
            assertEquals(xp + 1500, player.statMap.getFineXP(stats.construction))
        }
}
