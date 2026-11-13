package org.rsmod.content.skills.hunter.implings

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

/** Published tiers/weights and mapped anchors; invisible duration is the source's approximate 2min. */
@Singleton
public class ImplingPopulation @Inject constructor(private val repo:NpcRepository,private val types:NpcTypeList,private val random:GameRandom){
    private data class Roller(val kind:Int,val origin:CoordGrid)
    private val visible=WeakHashMap<Npc,Roller>()
    private val waiting=WeakHashMap<Npc,Boolean>()
    private val anchors=checkNotNull(javaClass.getResourceAsStream("impling-anchors.tsv")).bufferedReader().use{it.readLines()}.filter{it.isNotBlank()}.map{it.split('\t')}.groupBy{it[0]}.mapValues{(_,rows)->rows.map{CoordGrid(it[1].toInt(),it[2].toInt())}}
    public fun roll(npc:Npc){
        val kind=ImplingNpcs.precursors.indexOfFirst{it.id==npc.id};if(kind<0)return
        val generic=npc.spawnCoords in anchors.getValue("generic")
        if(generic||kind==4){
            if(waiting.put(npc,true)==null){npc.aiTimer(2800);return}
            if(kind==4){val points=anchors.getValue("crystal");spawn(12,points[random.of(points.size)],Roller(-1,npc.spawnCoords))}
            else repeat(3){val chance=random.of(100);val category=when{chance<14->0;chance<21->1;chance<25->3;else->-1};if(category>=0)spawn(select(category),npc.coords,Roller(-1,npc.spawnCoords))}
            npc.aiTimer(3000);return
        }
        val number=select(kind)
        spawn(number,npc.coords,Roller(kind,npc.spawnCoords));repo.del(npc,Int.MAX_VALUE)
    }
    private fun select(kind:Int):Int{
        val table=when(kind){0->listOf(1 to 20,2 to 20,3 to 20,4 to 20,5 to 10,6 to 10);1->listOf(3 to 10,4 to 10,5 to 20,6 to 37,7 to 20,8 to 2,9 to 1);2->listOf(7 to 150,8 to 114,9 to 27,10 to 9,11 to 1);3->listOf(7 to 10,8 to 50,9 to 30,10 to 10,11 to 1);else->listOf(12 to 1)}
        var roll=random.of(table.sumOf{it.second});var number=table.last().first
        for((n,w)in table){if(roll<w){number=n;break};roll-=w}
        return number
    }
    private fun spawn(number:Int,coords:CoordGrid,roller:Roller){
        val species=ImplingSpeciesList.all.first{it.number==number}
        val variants=if(ImplingHunting.puro(coords))species.maze else species.wild
        val type=variants[random.of(variants.size)]
        val caught=Npc(types[type],coords);caught.spawnCoords=roller.origin
        repo.add(caught,Int.MAX_VALUE);caught.aiTimer(if(number==12)500 else 3000)
        visible[caught]=roller
    }
    public fun replace(npc:Npc):Boolean{
        val roller=visible.remove(npc)?:return false
        if(roller.kind<0){repo.del(npc,Int.MAX_VALUE);return true}
        val precursor=Npc(types[ImplingNpcs.precursors[roller.kind]],roller.origin)
        repo.add(precursor,Int.MAX_VALUE);precursor.aiTimer(200);repo.del(npc,Int.MAX_VALUE);return true
    }
    public fun expire(npc:Npc){replace(npc)}
}
