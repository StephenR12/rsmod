package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RegionalShopTest {
    @Test fun GameTestState.`rescued guests unlock the chest and only a paid full quest glove purchase counts`() = runGameTest(CulinaromancerChest::class,ShopScript::class) {
        val c=CoordGrid(3219,9623)
        player.members=true
        player.teleport(c.translate(-1,0))
        val chest=placeMapLoc(c,locTypes.getValue(12308),angle=LocAngle.North)
        val quest=cacheTypes.varbits.getValue(1850)
        fun equipment() { player.opLoc5(chest);advance(3) }
        equipment()
        assertNull(player.openedShop)
        player.setVarBit(quest,3)
        player.setVarBit(cacheTypes.varbits.getValue(1852),2)
        equipment()
        assertEquals(368,player.openedShop?.inv?.type?.id)
        assertTrue(player.openedShop!!.inv.any { it?.id==7455 })
        assertTrue(player.openedShop!!.inv.none { it?.id==7462 })
        player.ifClose()
        advance()
        player.setVarBit(quest,5)
        equipment()
        val inv=player.openedShop!!.inv
        val slot=inv.indexOfFirst { it?.id==7462 }
        assertTrue(slot>=0)
        fun buy() { player.ifButton(ShopComponents.shop_inv,comsub=slot+1,obj=7462,op=IfButtonOp.Op2);advance() }
        player.inv[0]=InvObj(objs.coins,129999)
        buy()
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.BuyCulinaromancerGloves))
        player.inv[0]=InvObj(objs.coins,130000)
        buy()
        assertEquals(0,player.count(objs.coins))
        assertEquals(1,player.count(objTypes.getValue(7462)))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.BuyCulinaromancerGloves))
    }

    @Test fun GameTestState.`only a paid colour selection with an activated ring earns the kitten task`() = runGameTest(KittenAndGenie::class) {
        player.members=true
        player.teleport(CoordGrid(3151,3408))
        val gertrude=spawnNpc(CoordGrid(3151,3409),npcTypes.getValue(3500))
        player.setVarp(cacheTypes.varps.getValue(180),6)
        player.inv[0]=InvObj(objs.coins,200)
        random.next=3
        player.opNpc3(gertrude)
        advance(2)
        assertEquals(1,player.count(objTypes.getValue(1558)))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SelectKittenColour))
        player.worn[Wearpos.Ring.slot]=InvObj(objTypes.getValue(6465))
        player.opNpc3(gertrude)
        advance(2)
        assertEquals(100,player.count(objs.coins))
        player.inv[player.inv.indexOfFirst { it?.id==1558 }]=null
        player.opNpc3(gertrude)
        advance(2)
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton,1))
        advance()
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,4))
        advance(2)
        assertEquals(1,player.count(objTypes.getValue(1559)))
        assertEquals(0,player.count(objs.coins))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SelectKittenColour))
    }

    @Test fun GameTestState.`the crevice needs rope and light and the Genie room checks the saved quest stage`() = runGameTest(KittenAndGenie::class) {
        player.members=true
        val c=CoordGrid(3373,2905)
        val crevice=placeMapLoc(c,locTypes.getValue(10416),LocShape.GroundDecor)
        player.teleport(c)
        player.inv[0]=InvObj(objs.rope)
        player.opLoc1(crevice)
        advance(2)
        assertEquals(1,player.count(objs.rope))
        player.inv[1]=InvObj(objs.lit_candle)
        player.opLoc1(crevice)
        advance(2)
        assertEquals(CoordGrid(3372,9305),player.coords)
        val door=placeMapLoc(CoordGrid(3371,9312),locTypes.getValue(10431),LocShape.WallStraight,LocAngle.North)
        player.teleport(CoordGrid(3371,9312))
        player.opLoc1(door)
        advance(2)
        assertEquals(9312,player.coords.z)
        player.setVarBit(cacheTypes.varbits.getValue(1444),40)
        player.opLoc1(door)
        advance(2)
        assertEquals(9313,player.coords.z)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.VisitElidGenie))
        player.teleport(CoordGrid(3371,9319))
        val genie=spawnNpc(CoordGrid(3371,9320),npcTypes.getValue(4738))
        player.opNpc1(genie)
        advance(2)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.VisitElidGenie))
    }
}
