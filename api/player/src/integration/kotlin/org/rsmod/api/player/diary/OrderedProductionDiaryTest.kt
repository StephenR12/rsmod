package org.rsmod.api.player.diary

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class OrderedProductionDiaryTest {
    @Test fun GameTestState.`crossbow needs local string limbs stock and assembly before final stringing`()=runGameTest{
        player.members=true;val town=CoordGrid(2612,3080)
        player.recordDiaryAdvancedFletch(objs.rune_crossbow.id,town)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateArdougneRuneCrossbow))
        player.recordDiaryCrossbowString(objs.crossbow_string.id,CoordGrid(2731,3278,1))
        player.recordDiaryAdvancedAnvil(objs.runite_limbs.id,CoordGrid(2613,3078))
        player.recordDiaryAdvancedFletch(objs.yew_stock.id,town)
        player.recordDiaryAdvancedFletch(objs.unstrung_rune_crossbow.id,town)
        player.recordDiaryAdvancedFletch(objs.rune_crossbow.id,town)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateArdougneRuneCrossbow))
        player.recordDiaryCrossbowString(objs.crossbow_string.id,CoordGrid(2731,3278))
        player.recordDiaryAdvancedFletch(objs.unstrung_rune_crossbow.id,town)
        player.recordDiaryAdvancedFletch(objs.rune_crossbow.id,CoordGrid(3200,3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateArdougneRuneCrossbow))
        player.recordDiaryAdvancedFletch(objs.rune_crossbow.id,town)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CreateArdougneRuneCrossbow))
    }
    @Test fun GameTestState.`another diary progress update resets crossbow and golem sequences but preserves own task`()=runGameTest{
        player.members=true
        player.recordDiaryCrossbowString(objs.crossbow_string.id,CoordGrid(2731,3278))
        player.interruptDiaryProductionSequence(DiaryTask.CreateArdougneRuneCrossbow)
        assertEquals(1,player.vars[OrderedProductionVars.progress]and 15)
        player.recordDiaryRuniteGolemOre(CoordGrid(3185,3930))
        assertEquals(0,player.vars[OrderedProductionVars.progress]and 15)
        player.interruptDiaryProductionSequence(DiaryTask.FishMolchTrout)
        assertEquals(0,player.vars[OrderedProductionVars.progress]and 240)
    }
    @Test fun GameTestState.`resource scimitar requires two own golem ores and two locally smelted bars`()=runGameTest{
        player.members=true;val c=CoordGrid(3190,3938)
        player.recordDiaryAdvancedSmelt(objs.runite_bar.id,c);player.recordDiaryAdvancedSmelt(objs.runite_bar.id,c)
        player.recordDiaryAdvancedAnvil(1333,c)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithResourceRuneScimitar))
        repeat(2){player.recordDiaryRuniteGolemOre(c)}
        player.recordDiaryAdvancedSmelt(objs.runite_bar.id,CoordGrid(3200,3200))
        player.recordDiaryAdvancedSmelt(objs.runite_bar.id,c)
        player.recordDiaryAdvancedAnvil(1333,c)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithResourceRuneScimitar))
        player.recordDiaryAdvancedSmelt(objs.runite_bar.id,c);player.recordDiaryAdvancedAnvil(1333,c)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithResourceRuneScimitar))
    }
    @Test fun GameTestState.`Guild battlestaff requires own celastrus harvest and local bark fletching`()=runGameTest{
        player.members=true;val c=CoordGrid(1240,3740)
        player.recordDiaryAdvancedFletch(objs.battlestaff.id,c)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateFarmingGuildBattlestaff))
        player.recordDiaryCelastrusHarvest(c)
        player.recordDiaryAdvancedFletch(objs.battlestaff.id,CoordGrid(3200,3200))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CreateFarmingGuildBattlestaff))
        player.recordDiaryAdvancedFletch(objs.battlestaff.id,c)
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CreateFarmingGuildBattlestaff))
    }
}
