package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal object MenagerieInv:InvReferences(){val extras=find("poh_menagerie_pets");val arena=find("poh_menagerie_arena_pets")}
internal object MenagerieInvEdit:InvEditor(){init{edit(MenagerieInv.extras){scope=InvScope.Perm;stack=InvStackType.Never;protect=false}}}
internal object MenagerieUI:InterfaceReferences(){val house=find("poh_menagerie");val list=find("poh_petlist")}
internal object MenagerieButtons:ComponentReferences(){val pets=find("poh_menagerie:list");val roaming=find("poh_menagerie:roaming")}
internal object MenagerieBits:VarBitReferences(){val closed=find("poh_menagerie_closed")}

@Singleton
internal class MenagerieResidents @Inject constructor(
    private val pets:SkillPets,private val npcs:NpcRepository,private val npcTypes:NpcTypeList,
    private val inventories:InvTypeList,private val collision:CollisionFlagMap,
){
    val extras=buildMap<Int,Int>{
        put(3695,6657);put(7771,6661);put(29519,13665);put(30970,14517)
        for(i in 0..2)put(6670+i,6658+i)
        for(i in 0..5){put(1561+i,6662+i);put(1567+i,6676+i)}
        for(i in 0..5){put(listOf(6550,6549,6551,6552,6553,6554)[i],6683+i);put(listOf(6556,6555,6557,6558,6559,6560)[i],6690+i)}
        put(7581,6682);put(7582,6668);put(7584,6689);put(7585,6696)
    }
    private val fighters = WeakHashMap<Player, MutableMap<Npc, Int>>()
    private val rosters=WeakHashMap<Player,MutableMap<Npc,Int>>()
    fun count(player:Player)=pets.storedInMenagerie(player).size+extraCount(player)
    private fun extraCount(player:Player)=player.invMap[MenagerieInv.extras]?.filterNotNull()?.size?:0
    fun available(player:Player)=pets.availableForMenagerie(player)
    fun clear(player:Player){
        player.clearTimer(ArenaPetTimers.fight)
        for(npc in fighters.remove(player)?.keys.orEmpty())if(npc.isSlotAssigned)npcs.del(npc,Int.MAX_VALUE)
        for(npc in rosters.remove(player)?.keys.orEmpty())if(npc.isSlotAssigned)npcs.del(npc,Int.MAX_VALUE)}
    fun restore(player:Player,origin:CoordGrid,room:HouseRoom){
        clear(player)
        val battling=mutableMapOf<Npc,Int>()
        if(room.variant(3)>0){
            player.invMap[MenagerieInv.arena]?.let{storage->
                for(slot in 0 until storage.size){
                    val item=storage[slot]?:continue
                    val type=petNpc(item.id)?:continue
                    val relative=org.rsmod.game.region.util.RegionRotations.translateCoords(room.rotation,org.rsmod.map.zone.ZoneGrid(2,2+slot))
                    val tile=origin.translate(relative.x,relative.z)
                    val npc=Npc(npcTypes.getValue(type),tile)
                    npc.mode=NpcMode.None
                    npcs.add(npc,Int.MAX_VALUE)
                    battling[npc]=slot
                }
            }
            fighters[player]=battling
            if(battling.size==2)player.timer(ArenaPetTimers.fight,4)
        }
        val list=pets.storedInMenagerie(player).map{it.item to it.npc}.toMutableList()
        player.invMap[MenagerieInv.extras]?.filterNotNull()?.forEach{item->extras[item.id]?.let{list+=item.id to it}}
        val tiles=buildList{for(x in 1..6)for(z in 1..6){val tile=origin.translate(x,z);if(collision[tile.x,tile.z,tile.level] and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR)==0)add(tile)}}
        if(tiles.isEmpty())return
        val roster=mutableMapOf<Npc,Int>()
        for((i,form)in list.withIndex()){
            val npc=Npc(npcTypes.getValue(form.second),tiles[i%tiles.size])
            npc.mode=if(player.vars[MenagerieBits.closed]==0 && form.first!=3695)NpcMode.Wander else NpcMode.None
            npcs.add(npc,Int.MAX_VALUE)
            roster[npc]=form.first
        }
        rosters[player]=roster
    }
    private fun petNpc(item:Int)=extras[item]?:pets.housePet(item)?.npc

    fun hasFighters(player:Player) = player.invMap[MenagerieInv.arena]?.any{it!=null}==true

    fun enterArena(player:Player,slot:Int):Boolean{
        val item=player.inv[slot]?:return false
        if(petNpc(item.id)==null)return false
        val arena=player.invMap.getOrPut(inventories[MenagerieInv.arena])
        return player.invTransaction(player.inv,arena){transfer(select(player.inv),slot,1,select(arena))}.success
    }

    fun bout(player:Player){
        val pair=fighters[player]?.keys?.toList().orEmpty()
        if(pair.size!=2 || pair.any{!it.isSlotAssigned})return
        for((i,pet)in pair.withIndex()){
            pet.faceNpc(pair[1-i])
            val seq=pet.type.paramOrNull(org.rsmod.api.config.refs.params.attack_anim)
            if(seq!=null)pet.anim(seq)
            else if(pet.id in 6662..6696)pet.anim(MenagerieSequences.swipe)
            if(pet.id==6657)pet.say("...")
            else if(pet.id in 6662..6696)pet.say("Hiss!")
        }
    }

    fun scratch(player:Player,item:Int,tile:CoordGrid):Boolean{
        val type=extras[item]?:return false
        if(type !in 6662..6696)return false
        val cat=Npc(npcTypes.getValue(type),player.coords)
        cat.mode=NpcMode.None
        npcs.add(cat,5)
        cat.faceSquare(tile)
        cat.anim(MenagerieSequences.swipe)
        return true
    }

    fun deposit(player:Player,item:Int,slot:Int,tier:Int):Boolean{
        val capacity=listOf(0,3,5,7,9,12,65).getOrNull(tier)?:return false
        if(item !in extras)return pets.depositInMenagerie(player,item,if(tier==6)65 else capacity-extraCount(player))
        if(player.inv[slot]?.id!=item || (tier<6 && count(player)>=capacity))return false
        val storage=player.invMap.getOrPut(inventories[MenagerieInv.extras])
        return player.invTransaction(player.inv,storage){transfer(select(player.inv),slot,1,select(storage))}.success
    }
    fun withdraw(player:Player,item:Int,extraSlot:Int=-1):Boolean{
        if(item !in extras)return pets.withdrawFromMenagerie(player,item)
        val storage=player.invMap[MenagerieInv.extras]?:return false
        val slot=if(extraSlot>=0)extraSlot else storage.indexOfFirst{it?.id==item}
        if(slot<0||storage[slot]?.id!=item)return false
        val taken=player.invTransaction(player.inv,storage){transfer(select(storage),slot,1,select(player.inv))}.success
        if(taken && item==3695)player.completeDiaryTask(DiaryTask.RetrieveMenageriePetRock)
        return taken
    }
    fun pickUp(player:Player,npc:Npc):Boolean{
        val pair=fighters[player]
        val slot=pair?.get(npc)
        if(slot!=null){
            val storage=player.invMap[MenagerieInv.arena]?:return false
            val taken=player.invTransaction(player.inv,storage){transfer(select(storage),slot,1,select(player.inv))}.success
            if(taken){pair.remove(npc);npcs.del(npc,Int.MAX_VALUE);player.clearTimer(ArenaPetTimers.fight)}
            return taken
        }
        val roster=rosters[player]?:return false
        val item=roster[npc]?:return false
        if(!withdraw(player,item))return false
        roster.remove(npc);npcs.del(npc,Int.MAX_VALUE)
        return true
    }
    fun inSlot(player:Player,sub:Int):Int?{
        if(sub>=64)return player.invMap[MenagerieInv.extras]?.get(sub-64)?.id
        return pets.storedInMenagerie(player).firstOrNull{it.index==sub}?.item
    }
}

public class Menagerie @Inject internal constructor(private val homes:StarterHouse,private val residents:MenagerieResidents,private val locTypes:LocTypeList,private val npcTypes:NpcTypeList,private val objects:ObjTypeList):PluginScript(){
    private val views=WeakHashMap<Player,CoordGrid>()
    override fun ScriptContext.startup(){
        val types=locTypes.values.associateBy{it.internalName}
        onPlayerTimer(ArenaPetTimers.fight){residents.bout(player)}
        for(name in (1..3).map{"poh_menagerie_combatring_$it"}+"poh_menagerie_combatring_mat"){
            onOpLocU(types.getValue(name)){e->
                val room=homes.roomAt(player,e.loc.coords)?:return@onOpLocU
                if(!room.type.isMenagerie || room.variant(3)==0)return@onOpLocU
                if(residents.enterArena(player,e.invSlot)){refresh(e.loc.coords);mes("You put your pet into the arena.")}
                else mes("The arena has space for two pets.")
            }
        }
        for(i in 1..3){
            onOpLocU(types.getValue("poh_menagerie_scratchingpost_$i")){e->
                val room=homes.roomAt(player,e.loc.coords)?:return@onOpLocU
                if(room.type.isMenagerie && room.variant(2)==i && residents.scratch(player,e.objType.id,e.loc.coords))
                    mes("Your cat plays with the scratching post.")
            }
        }
        for(i in 1..6){
            val house=types.getValue("poh_menagerie_pethouse_$i")
            onOpLoc1(house){if(owner(it.loc.coords,i))show(it.loc.coords,i)}
            onOpLocU(house){e->
                if(!owner(e.loc.coords,i))return@onOpLocU
                val item=inv[e.invSlot]?:return@onOpLocU
                if(residents.deposit(player,item.id,e.invSlot,i)){refresh(e.loc.coords);mes("You add ${objects[item].name.lowercase()} to your menagerie.")}
                else mes("You can't add that pet to this house.")
            }
        }
        onOpLoc1(types.getValue("poh_menagerie_petlist_1")){if(homes.roomAt(player,it.loc.coords)?.type?.isMenagerie==true){ifOpenMainModal(MenagerieUI.list);runClientScript(647)}}
        for(type in npcTypes.values){
            if(type.id==6657 || (type.id !in residents.extras.values && type.internalName?.startsWith("poh_skillpet")!=true && type.internalName!="poh_chompybird_pet" && type.internalName!="poh_tempoross_pet" && type.internalName!="poh_quetzal_pet" && type.internalName!="poh_herbiboar_pet"))continue
            if(type.op.getOrNull(2).equals("Pick-up",true))onOpNpc3(type){residents.pickUp(player,it.npc)}
        }
        onIfModalButton(MenagerieButtons.pets){e->
            val tile=views[player]?:return@onIfModalButton
            if(!owner(tile,null))return@onIfModalButton
            val item=residents.inSlot(player,e.comsub)?:return@onIfModalButton
            if(e.op.slot==10){mes(objects.getValue(item).desc);return@onIfModalButton}
            if(e.op.slot==1 && residents.withdraw(player,item,e.comsub-64)){refresh(tile)}
        }
        onIfModalButton(MenagerieButtons.roaming){
            val tile=views[player]?:return@onIfModalButton
            if(!owner(tile,null))return@onIfModalButton
            vars[MenagerieBits.closed]=1-vars[MenagerieBits.closed];refresh(tile)
        }
        onIfClose(MenagerieUI.house){views.remove(player);player.invMap[MenagerieInv.extras]?.let(player::stopInvTransmit)}
    }
    private fun ProtectedAccess.owner(tile:CoordGrid,tier:Int?):Boolean{
        val room=homes.roomAt(player,tile)?:return false
        return room.type.isMenagerie && room.variant(1)>0 && (tier==null || room.variant(1)==tier)
    }
    private fun ProtectedAccess.refresh(tile:CoordGrid){val room=homes.roomAt(player,tile)?:return;residents.restore(player,ZoneKey.from(tile).toCoords(),room)}
    private suspend fun ProtectedAccess.show(tile:CoordGrid,tier:Int){
        val available=residents.available(player)
        if(available.isNotEmpty() && choice2("View stored pets.",false,"Add a pet.",true)){
            val pet=available.getOrNull(menu("Add a pet",true,available.map{objects.getValue(it.item).name}))?:return
            if(residents.deposit(player,pet.item,-1,tier))refresh(tile) else mes("There is no room for another pet.")
        }
        ifOpenMainModal(MenagerieUI.house);views[player]=tile
        invTransmit(inv(MenagerieInv.extras))
        ifSetEvents(MenagerieButtons.pets,0..75,IfEvent.Op1,IfEvent.Op10)
        ifSetEvents(MenagerieButtons.roaming,-1..0,IfEvent.Op1)
    }
}
