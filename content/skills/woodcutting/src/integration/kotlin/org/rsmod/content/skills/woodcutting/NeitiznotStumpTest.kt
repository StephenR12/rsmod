package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.scripts.NeitiznotStump
import org.rsmod.content.skills.woodcutting.scripts.NeitiznotStumpLocs
import org.rsmod.content.skills.woodcutting.scripts.NeitiznotStumpObjs
import org.rsmod.content.skills.woodcutting.scripts.NeitiznotStumpScript
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class NeitiznotStumpTest {
    @Test fun GameTestState.`native stump splits one arctic log per three ticks for five xp`()=test{d->
        prepare();val stump=placeMapLoc(CoordGrid(2000,2000),NeitiznotStumpLocs.stump);player.telejump(stump.coords.translateX(-1));player.inv[0]=InvObj(objs.rune_axe);player.inv[1]=InvObj(objs.arctic_pine_logs)
        val xp=player.statMap.getFineXP(stats.woodcutting)
        player.withProtectedAccess{d.stump.make(this,stump,false,1)};advance(2);assertEquals(0,player.count(NeitiznotStumpObjs.split));advance(2)
        assertEquals(1,player.count(NeitiznotStumpObjs.split));assertEquals(50,player.statMap.getFineXP(stats.woodcutting)-xp)
    }
    @Test fun GameTestState.`shield needs Mawnis instruction and consumes exact supplies for Crafting xp`()=test{d->
        prepare();val stump=placeMapLoc(CoordGrid(2342,3807),NeitiznotStumpLocs.stump);player.telejump(stump.coords.translateX(-1));player.inv[0]=InvObj(objs.hammer);repeat(2){player.inv[it+1]=InvObj(objs.arctic_pine_logs)};player.inv[3]=InvObj(objs.rope);player.inv[4]=InvObj(objs.bronze_nails)
        player.withProtectedAccess{d.stump.make(this,stump,true,1)};advance(3);assertEquals(0,player.count(NeitiznotStumpObjs.shield))
        val xp=player.statMap.getFineXP(stats.crafting);player.withProtectedAccess{vars[varbits.fremennik_isles_progress]=290;d.stump.make(this,stump,true,1)};advance(4)
        assertEquals(1,player.count(NeitiznotStumpObjs.shield));assertEquals(0,player.count(objs.arctic_pine_logs));assertEquals(0,player.count(objs.rope));assertEquals(0,player.count(objs.bronze_nails));assertEquals(340,player.statMap.getFineXP(stats.crafting)-xp);assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftNeitiznotShield))
    }
    private fun GameTestScope.prepare(){player.members=true;player.stats[stats.woodcutting]=56;player.telejump(CoordGrid(2000,2001));for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(2000+dx,2000+dz))}
    private fun GameTestState.test(body:GameTestScope.(Deps)->Unit)=runInjectedGameTest(Deps::class,scripts=arrayOf(NeitiznotStumpScript::class),testBody=body)
    data class Deps @Inject constructor(val stump:NeitiznotStump)
}
