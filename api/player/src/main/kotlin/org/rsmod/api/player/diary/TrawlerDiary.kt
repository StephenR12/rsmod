package org.rsmod.api.player.diary
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
private const val TRAWLER_MANTA=512
/** Called only after successfully delivering this player's Trawler catch. */
public fun Player.recordDiaryTrawlerManta(){
 if(!members||hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta)||vars[OrderedProductionVars.progress] and TRAWLER_MANTA!=0)return
 interruptDiaryProductionSequence(DiaryTask.CatchCookTrawlerManta);interruptDiaryKillSequence()
 VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] or TRAWLER_MANTA)
}
public fun Player.recordDiaryTrawlerCook(output:Int,c:CoordGrid){
 if(members&&output==objs.manta_ray.id&&c.level==0&&c.x in 2640..2691&&c.z in 3130..3190&&vars[OrderedProductionVars.progress] and TRAWLER_MANTA!=0){
 completeDiaryTask(DiaryTask.CatchCookTrawlerManta)
 VarPlayerIntMapSetter.set(this,OrderedProductionVars.progress,vars[OrderedProductionVars.progress] and TRAWLER_MANTA.inv())
 }
}
