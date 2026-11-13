package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.diary.*
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CityChestTest {
    @Test fun GameTestState.`two players searching a full Hemenster chest share one reward and restock`() = game { d ->
        val c=CoordGrid(2639,3424)
        val chest=placeMapLoc(c,locTypes.getValue(11742),LocShape.CentrepieceStraight,LocAngle.North)
        player.telejump(c.translate(1,0))
        val second=registerPlayer(c.translate(1,0))
        for(p in listOf(player,second)) {
            p.members=true
            p.setCurrentLevel(stats.thieving,47)
            p.inv[0]=InvObj(SpecialTheftObjs.lockpick)
            p.opLoc2(chest)
        }
        advance(6)
        assertEquals(5,player.count(objs.steel_arrowheads)+second.count(objs.steel_arrowheads))
        assertEquals(1500,player.statMap.getFineXP(stats.thieving)+second.statMap.getFineXP(stats.thieving))
        assertEquals(1,listOf(player,second).count { it.hasCompletedDiaryTask(DiaryTask.LootHemensterChest) })
        assertNotNull(d.locs.findExact(c,locTypes.getValue(11740)))
        advance(150)
        assertNotNull(d.locs.findExact(c,locTypes.getValue(11742)))
    }

    @Test fun GameTestState.`castle chest trap deals damage and four loot items must all fit before teleport`() = game { _ ->
        val c=CoordGrid(2588,3291,1)
        val chest=placeMapLoc(c,locTypes.getValue(11739),LocShape.CentrepieceStraight,LocAngle.South)
        player.telejump(c.translate(-1,0))
        player.setBaseLevel(stats.hitpoints,40)
        player.setCurrentLevel(stats.hitpoints,40)
        player.setCurrentLevel(stats.thieving,72)
        player.opLoc1(chest)
        advance(4)
        assertTrue(player.stats[stats.hitpoints] < 40)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.LootArdougneCastleChest))
        player.fillInv(InvObj(objs.knife))
        player.inv[0]=InvObj(objs.coins,20)
        player.inv[1]=null
        player.opLoc2(chest)
        advance(5)
        assertEquals(20,player.count(objs.coins))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.LootArdougneCastleChest))
        player.clearInv()
        player.opLoc2(chest)
        advance(5)
        assertEquals(1000,player.count(objs.coins))
        for(o in listOf(objs.raw_shark,objs.uncut_sapphire,objs.adamantite_ore)) assertEquals(1,player.count(o))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.LootArdougneCastleChest))
        assertEquals(0,player.coords.level)
        assertEquals(5000,player.statMap.getFineXP(stats.thieving))
    }

    @Test fun GameTestState.`rich chest requires base 78 and a regular lockpick and doubles elite wire`() = game { _ ->
        val c=CoordGrid(2700,5348,1)
        val chest=placeMapLoc(c,locTypes.getValue(22681),LocShape.CentrepieceStraight)
        player.telejump(c.translate(0,1))
        player.setBaseLevel(stats.thieving,77)
        player.setCurrentLevel(stats.thieving,82)
        player.inv[0]=InvObj(SpecialTheftObjs.lockpick)
        player.opLoc2(chest)
        advance(4)
        player.setBaseLevel(stats.thieving,78)
        player.clearInv()
        player.opLoc2(chest)
        advance(4)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.LootDorgeshRichChest))
        player.inv[0]=InvObj(SpecialTheftObjs.lockpick)
        player.setVarBit(DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).complete,1)
        val startingXp=player.statMap.getFineXP(stats.thieving)
        random.next=12
        player.opLoc2(chest)
        advance(4)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.LootDorgeshRichChest))
        assertEquals(2,player.count(objTypes.getValue(10981)))
        assertEquals(6500,player.statMap.getFineXP(stats.thieving)-startingXp)
    }

    @Test fun GameTestState.`open bag receives the hard diary quantity at the Rogues castle`() = game { _ ->
        val c=CoordGrid(3283,3946)
        val chest=placeMapLoc(c,locTypes.getValue(26757),LocShape.CentrepieceStraight)
        player.telejump(c.translate(0,-1))
        player.setCurrentLevel(stats.thieving,84)
        player.setVarBit(DiaryRegion.Wilderness.tier(DiaryDifficulty.Hard).complete,1)
        player.inv[0]=InvObj(objs.looting_bag)
        player.withProtectedAccess { opHeld1(0) }
        assertEquals(1,player.count(objs.looting_bag_open))
        random.next=0
        player.opLoc2(chest)
        advance(6)
        assertEquals(0,player.count(objs.nature_rune))
        assertEquals(50,player.invMap[cacheTypes.invs.getValue(516)]?.count(objs.nature_rune))
        assertEquals(7017,player.statMap.getFineXP(stats.thieving))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.LootRoguesCastleChest))
    }

    private fun GameTestState.game(body:GameTestScope.(Dependencies)->Unit) = runInjectedGameTest(Dependencies::class,null,CityChestTheft::class,LootingBag::class,PlayerHitScript::class) { d ->
        player.members=true
        for(c in listOf(CoordGrid(2639,3424),CoordGrid(2588,3291,1),CoordGrid(2700,5348,1),CoordGrid(3283,3946),CoordGrid(2695,3283))) {
            for(x in -8..8 step 8) for(z in -8..8 step 8) allocZoneCollision(c.translate(x,z))
        }
        body(d)
    }
    class Dependencies @Inject constructor(val locs:LocRepository)
}
