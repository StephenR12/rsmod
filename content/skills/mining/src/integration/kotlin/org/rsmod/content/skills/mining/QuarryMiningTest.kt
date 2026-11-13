package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.diary.recordDiaryOre
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class QuarryMiningTest {
    @Test fun GameTestState.`quarry cascade stops at first success and preserves exact size xp`() = runGameTest {
        val calls=mutableListOf<Int>();var n=0
        val reward=QuarryRocks.roll(QuarryRocks.granite){lo,_->calls+=lo;n++==1}
        assertEquals(listOf(6,8),calls);assertEquals(QuarryObjs.granite[1],reward?.obj);assertEquals(60.0,reward?.xp)
        assertNull(QuarryRocks.roll(QuarryRocks.sandstone){_,_->false})
    }
    @Test fun GameTestState.`native granite mining produces largest successful block with size xp`() = runGameTest(Mining::class) {
        player.members=true;player.stats[stats.mining]=99;player.righthand=InvObj(MiningObjs.runePickaxe)
        val rock=placeMapLoc(CoordGrid(2000,2000),QuarryLocs.granite)
        player.telejump(rock.coords.translateZ(1));for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(2000+dx,2000+dz));random.next=128
        val xp=player.statMap.getFineXP(stats.mining)
        random.then=0;player.opLoc1(rock);advance(9)
        assertEquals(1,player.count(QuarryObjs.granite[2]))
        assertEquals(750,player.statMap.getFineXP(stats.mining)-xp)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineDesertGranite))
        player.recordDiaryOre(QuarryObjs.granite[2].id,CoordGrid(3164,2909))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineDesertGranite))
    }
}
