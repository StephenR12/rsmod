package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class KingdomApprovalTest {
    @Test
    fun GameTestState.`raking restores royal support and caps further contributions`() =
        runGameTest(KingdomApproval::class) {
            player.members = true
            player.setBaseLevel(stats.farming, 10)
            player.setCurrentLevel(stats.farming, 10)
            player.inv[0] = InvObj(objs.rake)
            val patch = placeMapLoc(CoordGrid(2527, 3853), locTypes.getValue(15079))
            player.teleport(CoordGrid(2528, 3853))
            val approval = cacheTypes.varbits.getValue(72)
            player.setVarBit(approval, 125)
            player.opLoc1(patch)
            advance(10)
            assertEquals(125, player.vars[approval])
            player.setVarp(cacheTypes.varps.getValue(359), 100)
            player.opLoc1(patch)
            advance(13)
            assertEquals(127, player.vars[approval])
            assertEquals(2, player.count(objTypes.getValue(6055)))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SupportMiscellania))
            player.opLoc1(patch)
            advance(8)
            assertEquals(2, player.count(objTypes.getValue(6055)))
        }
}
