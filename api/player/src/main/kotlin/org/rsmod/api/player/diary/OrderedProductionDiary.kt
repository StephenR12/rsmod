package org.rsmod.api.player.diary

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid

public object OrderedProductionVars:VarpReferences(){public val progress:VarpType=find("diary_advanced_production_steps")}
internal object OrderedProductionBuilds:VarpBuilder(){init{build("diary_advanced_production_steps"){transmitNever=true}}}
private const val CROSSBOW_MASK=15
private const val GOLEM_MASK=240
private const val CELASTRUS=256

/** These two elite rows explicitly reset whenever another diary task gains progress. */
public fun Player.interruptDiaryProductionSequence(currentTask:DiaryTask?=null){
    val old=vars[OrderedProductionVars.progress]
    val mask=(if(currentTask!=DiaryTask.CreateArdougneRuneCrossbow)CROSSBOW_MASK else 0) or (if(currentTask!=DiaryTask.SmithResourceRuneScimitar)GOLEM_MASK else 0)
    if(old and mask!=0)VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,old and mask.inv())
}
private fun Player.productionStep(task:DiaryTask,mask:Int){
    if(!members||hasCompletedDiaryTask(task))return
    if(vars[OrderedProductionVars.progress] and mask==mask)return
    interruptDiaryProductionSequence(task);interruptDiaryKillSequence()
    VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] or mask)
}
private fun crossbowTown(c:CoordGrid):Boolean=c.level==0&&((c.x in 2535..2630&&c.z in 3070..3125)||(c.x in 2700..2746&&c.z in 3260..3300))
private fun guild(c:CoordGrid):Boolean=c.level==0&&c.x in 1216..1279&&c.z in 3712..3767

public fun Player.recordDiaryCrossbowString(output:Int,wheel:CoordGrid){
    if(output==objs.crossbow_string.id&&wheel==CoordGrid(2731,3278))productionStep(DiaryTask.CreateArdougneRuneCrossbow,1)
}
public fun Player.recordDiaryAdvancedFletch(output:Int,coords:CoordGrid){
    if(!members)return
    val progress=vars[OrderedProductionVars.progress]
    if(output==objs.battlestaff.id&&guild(coords)&&progress and CELASTRUS!=0){completeDiaryTask(DiaryTask.CreateFarmingGuildBattlestaff);VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] and CELASTRUS.inv())}
    if(!crossbowTown(coords))return
    if(output==objs.yew_stock.id)productionStep(DiaryTask.CreateArdougneRuneCrossbow,4)
    if(output==objs.unstrung_rune_crossbow.id&&progress and 7==7)productionStep(DiaryTask.CreateArdougneRuneCrossbow,8)
    if(output==objs.rune_crossbow.id&&progress and 15==15){completeDiaryTask(DiaryTask.CreateArdougneRuneCrossbow);VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] and CROSSBOW_MASK.inv())}
}
/** Actual own-tree harvest already proves that the player grew the native Guild crop. */
public fun Player.recordDiaryCelastrusHarvest(coords:CoordGrid){if(guild(coords))productionStep(DiaryTask.CreateFarmingGuildBattlestaff,CELASTRUS)}
public fun Player.recordDiaryAdvancedAnvil(output:Int,coords:CoordGrid){
    if(!members)return
    if(output==objs.runite_limbs.id&&coords in listOf(CoordGrid(2613,3078),CoordGrid(2613,3081),CoordGrid(2616,3081)))productionStep(DiaryTask.CreateArdougneRuneCrossbow,2)
    if(output==1333&&SkillDiaryAreas.resourceArea(coords)&&(vars[OrderedProductionVars.progress] ushr 6)and 3==2){completeDiaryTask(DiaryTask.SmithResourceRuneScimitar);VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] and GOLEM_MASK.inv())}
}
/** Called only for ore successfully mined from the player's own defeated Resource Area golem. */
public fun Player.recordDiaryRuniteGolemOre(coords:CoordGrid){
    if(!members||!SkillDiaryAreas.resourceArea(coords)||hasCompletedDiaryTask(DiaryTask.SmithResourceRuneScimitar))return
    val old=vars[OrderedProductionVars.progress];val ores=(old ushr 4)and 3;val bars=(old ushr 6)and 3
    if(ores+bars>=2)return
    interruptDiaryProductionSequence(DiaryTask.SmithResourceRuneScimitar);interruptDiaryKillSequence()
    VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,(vars[OrderedProductionVars.progress] and 48.inv()) or ((ores+1) shl 4))
}
public fun Player.recordDiaryAdvancedSmelt(output:Int,coords:CoordGrid){
    if(!members||output!=objs.runite_bar.id||!SkillDiaryAreas.resourceArea(coords)||hasCompletedDiaryTask(DiaryTask.SmithResourceRuneScimitar))return
    val old=vars[OrderedProductionVars.progress];val ores=(old ushr 4)and 3;val bars=(old ushr 6)and 3
    if(ores==0||bars>=2)return
    interruptDiaryProductionSequence(DiaryTask.SmithResourceRuneScimitar);interruptDiaryKillSequence()
    VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,(vars[OrderedProductionVars.progress] and GOLEM_MASK.inv()) or ((ores-1)shl 4) or ((bars+1)shl 6))
}
