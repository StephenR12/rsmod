package org.rsmod.content.skills.hunter.implings

import jakarta.inject.Inject
import org.rsmod.api.clue.Cache233ClueSteps
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.hunter.canCatchImpling
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class ImplingHunting @Inject constructor(private val npcs:NpcRepository,private val ground:ObjRepository,private val random:GameRandom,private val clues:ClueInventory,private val easy:Cache233EasyClueSteps,private val other:Cache233ClueSteps,private val population:ImplingPopulation) {
    public suspend fun catch(access:ProtectedAccess,npc:Npc,species:ImplingSpecies):Unit=with(access) {
        if(!player.members)return
        if(!npc.canCatchImpling(player)){mes("Another hunter has temporarily claimed this impling.");return}
        val net=player.righthand?.id
        val hasNet=net==ImplingObjs.net.id||net==ImplingObjs.magicNet.id
        val required=species.level+if(hasNet)0 else 10
        if(player.stat(stats.hunter)<required){mes("You need a Hunter level of $required to catch this impling${if(hasNet)"" else " barehanded"}.");return}
        if(puro(npc.coords)&&ImplingObjs.empty !in player.inv){mes("You need an empty impling jar to catch implings in Puro-Puro.");return}
        faceEntitySquare(npc);anim(ImplingSeqs.catch);delay(1)
        if(!npc.isValidTarget()||!npc.canCatchImpling(player)||!isWithinDistance(npc,1)||player.righthand?.id!=net||player.stat(stats.hunter)<required)return
        if(puro(npc.coords)&&ImplingObjs.empty !in player.inv)return
        val enhanced=!hasNet||net==ImplingObjs.magicNet.id
        if(random.randomDouble()>=chance(species,player.stat(stats.hunter),enhanced))return
        val jarred=ImplingObjs.empty in player.inv
        if(jarred) {
            val result=player.invTransaction(player.inv){val inv=select(player.inv);delete(inv,ImplingObjs.empty.id,1);add(inv,species.jar.id,1)}
            if(!result.success)return
        } else reward(this,species)
        val isMaze=species.maze.any{it.id==npc.id}
        statAdvance(stats.hunter,if(isMaze)species.mazeXp else species.wildXp)
        if(species.number in 5..6&&puro(npc.coords))player.completeDiaryTask(DiaryTask.CatchPuroEssenceEclectic)
        if(!population.replace(npc))npcs.despawn(npc,npc.type.respawnRate)
    }

    public suspend fun open(access:ProtectedAccess,slot:Int,species:ImplingSpecies):Unit=with(access) {
        if(!player.members||slot !in player.inv.indices||player.inv[slot]?.id!=species.jar.id)return
        // The September-2025 warning applies when a carried clue/box is present and ownership is full.
        if(species.clues.any{(tier,_)->!clues.state(player,tier).canReceive&&clues.ownsAny(player.inv,player.inv,tier)}) {
            if(menu("You already hold a clue and cannot receive another. Loot the jar anyway?","Yes.","No.")!=1)return
            if(player.inv[slot]?.id!=species.jar.id)return
        }
        if(!player.invDel(player.inv,species.jar,slot=slot).success)return
        reward(this,species)
        if(random.of(10)!=0)player.invAddOrDrop(ground,ImplingObjs.empty)
        else mes("The impling jar breaks.")
    }

    /** Roll only after consuming the source; full inventories receive owner-only ground loot. */
    public fun reward(access:ProtectedAccess,species:ImplingSpecies):Unit=with(access) {
        if(species.number==11){
            val outcome=LuckyImplingLoot.select(random.of(5),random.randomDouble())
            for(item in outcome.items)player.invAddOrDrop(ground,item.obj,if(item.minimum==item.maximum)item.minimum else random.of(item.minimum..item.maximum))
            return
        }
        if(species.number==3&&random.of(500)==0)player.invAddOrDrop(ground,ImplingObjs.grubbyKey)
        else {
            val mm2=vars[ImplingBits.mm2]>=195
            val table=ImplingLoot.tables.getValue(species.name).filter{!it.mm2||mm2}
            val drop=ImplingLoot.select(species.name,random.of(table.sumOf{it.weight}),mm2)
            drop.obj?.let{player.invAddOrDrop(ground,it,random.of(drop.minimum..drop.maximum))}
        }
        if(species.number==12&&random.of(128)==0)player.invAddOrDrop(ground,ImplingObjs.signet)
        for((tier,denominator)in species.clues)if(random.of(denominator)==0){
            val reward=clues.reward(clues.state(player,tier),tier){when(tier){ClueTier.Beginner->objs.clue_scroll_beginner;ClueTier.Easy->easy.select(random.of(easy.steps.size));else->other.select(tier,random.of(other.steps(tier).size))}}
            reward.message?.let{mes(it)}
            reward.obj?.let{player.invAddOrDrop(ground,it,duration=checkNotNull(reward.groundDuration))}
        }
    }
    public companion object {
        public fun puro(c:CoordGrid):Boolean=c.level==0&&c.x in 2560..2623&&c.z in 4288..4351
        public fun chance(species:ImplingSpecies,level:Int,enhanced:Boolean):Double=SkillingSuccessRate.successRate(species.low+if(enhanced)20 else 0,species.high+if(enhanced)20 else 0,level.coerceIn(1,99),99).coerceAtMost(1.0)
    }
}
