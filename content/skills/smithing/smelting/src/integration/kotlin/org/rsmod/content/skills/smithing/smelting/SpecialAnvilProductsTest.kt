package org.rsmod.content.skills.smithing.smelting

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SpecialAnvilProductsTest {
    @Test fun GameTestState.`shield halves combine atomically at Smithing60 with correct xp`()=test{d->
        prepare();val anvil=placeMapLoc(CoordGrid(2000,2000),AnvilLocs.ordinary.first());player.inv[0]=InvObj(objs.hammer);player.inv[1]=InvObj(SpecialAnvilObjs.left);player.inv[2]=InvObj(SpecialAnvilObjs.right)
        player.stats[stats.smithing]=59;player.withProtectedAccess{d.products.make(this,anvil,SpecialAnvilProduct.DragonShield,1)};advance(6);assertEquals(0,player.count(SpecialAnvilObjs.shield))
        player.stats[stats.smithing]=60;val xp=player.statMap.getFineXP(stats.smithing)
        player.withProtectedAccess{d.products.make(this,anvil,SpecialAnvilProduct.DragonShield,1)};advance(5)
        assertEquals(1,player.count(SpecialAnvilObjs.shield));assertEquals(0,player.count(SpecialAnvilObjs.left));assertEquals(0,player.count(SpecialAnvilObjs.right));assertEquals(750,player.statMap.getFineXP(stats.smithing)-xp)
    }
    @Test fun GameTestState.`gold helmet enforces learned recipe and unique bank ownership`()=test{d->
        prepare();val anvil=placeMapLoc(CoordGrid(2000,2000),AnvilLocs.ordinary.first());player.inv[0]=InvObj(objs.hammer);repeat(3){player.inv[it+1]=InvObj(objs.gold_bar)}
        player.withProtectedAccess{d.products.make(this,anvil,SpecialAnvilProduct.GoldHelmet,1)};advance(6);assertEquals(0,player.count(SpecialAnvilObjs.helmet))
        player.withProtectedAccess{vars[SpecialAnvilBits.rock]=80}
        val bank=player.invMap.getOrPut(d.invs[invs.bank]);bank[0]=InvObj(SpecialAnvilObjs.helmet)
        player.withProtectedAccess{d.products.make(this,anvil,SpecialAnvilProduct.GoldHelmet,1)};advance(6);assertEquals(3,player.count(objs.gold_bar))
        bank[0]=null;val xp=player.statMap.getFineXP(stats.smithing)
        player.withProtectedAccess{d.products.make(this,anvil,SpecialAnvilProduct.GoldHelmet,1)};advance(5)
        assertEquals(1,player.count(SpecialAnvilObjs.helmet));assertEquals(300,player.statMap.getFineXP(stats.smithing)-xp)
    }
    @Test fun GameTestState.`West Ardougne and Resource Area products credit the correct native diaries`()=test{d->
        prepare();val shieldAnvil=placeMapLoc(CoordGrid(2499,3332),AnvilLocs.ordinary.first());player.telejump(shieldAnvil.coords.translateZ(1));player.inv[0]=InvObj(objs.hammer);player.inv[1]=InvObj(SpecialAnvilObjs.left);player.inv[2]=InvObj(SpecialAnvilObjs.right)
        player.withProtectedAccess{d.products.make(this,shieldAnvil,SpecialAnvilProduct.DragonShield,1)};advance(5);assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithWestArdougneDragonSquare))
        val goldAnvil=placeMapLoc(CoordGrid(3190,3938),AnvilLocs.ordinary.first());player.telejump(goldAnvil.coords.translateZ(1));player.clearInv();player.inv[0]=InvObj(objs.hammer);repeat(3){player.inv[it+1]=InvObj(objs.gold_bar)}
        player.withProtectedAccess{vars[SpecialAnvilBits.rock]=80;d.products.make(this,goldAnvil,SpecialAnvilProduct.GoldHelmet,1)};advance(5)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithResourceGoldHelmet))
    }
    private fun GameTestScope.prepare(){player.members=true;player.stats[stats.smithing]=99;player.telejump(CoordGrid(2000,2001));for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(2000+dx,2000+dz))}
    private fun GameTestState.test(body:GameTestScope.(Deps)->Unit)=runInjectedGameTest(Deps::class,Module,SpecialAnvilScript::class,testBody=body)
    private object Module:AbstractModule(){override fun configure(){install(SmeltingModule())}}
    data class Deps @Inject constructor(val products:SpecialAnvilProducts,val invs:InvTypeList)
}
