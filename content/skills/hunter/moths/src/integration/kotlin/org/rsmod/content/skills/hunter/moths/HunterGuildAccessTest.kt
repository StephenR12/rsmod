package org.rsmod.content.skills.hunter.moths

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HunterGuildAccessTest {
    @Test fun GameTestState.`Burrow stairs require base level and return to surface`() = runGameTest(HunterGuildAccess::class) {
        player.members=true;player.setBaseLevel(stats.hunter,45);player.setCurrentLevel(stats.hunter,99)
        val loc=placeMapLoc(CoordGrid(2000,2000),HunterGuildLocs.down)
        player.telejump(loc.coords.translateX(-1));allocZoneCollision(player.coords)
        player.opLoc1(loc);advance();assertEquals(0,player.coords.level);assertTrue(player.coords.z<9000)
        player.setBaseLevel(stats.hunter,46);player.opLoc1(loc);advance()
        assertEquals(CoordGrid(1557,9446),player.coords)
    }
    @Test fun GameTestState.`knife roots provide three normal logs without xp then preserve inventory`() = runGameTest(HunterGuildAccess::class) {
        player.members=true;player.inv[0]=InvObj(objs.knife)
        val xp=player.statMap.getFineXP(stats.woodcutting)
        repeat(3){ player.withProtectedAccess { with(HunterGuildAccess()){takeLog()} };advance() }
        assertEquals(3,player.count(objs.logs));assertEquals(xp,player.statMap.getFineXP(stats.woodcutting))
        player.withProtectedAccess { with(HunterGuildAccess()){takeLog()} };advance()
        assertEquals(3,player.count(objs.logs));assertEquals(1,player.count(objs.knife))
    }
}
