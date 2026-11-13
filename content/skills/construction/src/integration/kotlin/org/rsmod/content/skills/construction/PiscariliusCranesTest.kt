package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class PiscariliusCranesTest {
    @Test
    fun GameTestState
        .`real port crane bends a nail then consumes matching planks and pays both skills`() =
        runGameTest(PiscariliusCranes::class) {
            val at = CoordGrid(1819, 3728)
            val broken =
                locTypes.values.single { it.internalName == "piscarilius_fishingcrane_broken" }
            val repaired =
                locTypes.values.single { it.internalName == "piscarilius_fishingcrane_repaired" }
            val crane = placeMapLoc(at, broken)
            player.members = true
            player.teleport(at.translateX(-1))
            player.setBaseLevel(stats.crafting, 29)
            player.setCurrentLevel(stats.crafting, 35)
            player.setBaseLevel(stats.construction, 30)
            player.setCurrentLevel(stats.construction, 30)
            for (i in 0..2) player.inv[i] = InvObj(objs.oak_plank)
            player.inv[3] = InvObj(objs.hammer)
            player.inv[4] = InvObj(objs.bronze_nails, 5)
            player.inv[5] = InvObj(objs.steel_nails, 5)
            player.opLoc1(crane)
            advance(5)
            assertEquals(5, player.count(objs.bronze_nails))
            player.setBaseLevel(stats.crafting, 30)
            val before = player.statMap.getFineXP(stats.crafting)
            random.next = 9999
            random.then = 0
            player.opLoc1(crane)
            advance(4)
            assertEquals(4, player.count(objs.bronze_nails))
            advance(10)
            assertNotNull(findLoc(at, repaired))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RepairPiscariliusCrane))
            assertEquals(0, player.count(objs.oak_plank))
            assertEquals(0, player.count(objs.steel_nails))
            assertEquals(before + 1200, player.statMap.getFineXP(stats.crafting))
            advance(105)
            assertNotNull(findLoc(at, broken))
        }
}
