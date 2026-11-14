package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class MenagerieTest{
    data class Deps @Inject constructor(val house:StarterHouse,val rooms:HouseRoomRepository,val pets:SkillPets,val npcs:org.rsmod.game.entity.NpcList)
    @Test fun GameTestState.`the arena restores both pets and Probita cannot reclaim its insured participant`() =
        runInjectedGameTest(Deps::class,null,StarterHouseScript::class,Menagerie::class,org.rsmod.content.skills.pets.PetScript::class){d->
            player.members=true
            player.setVarBit(varbits.poh_house_location,1)
            player.setVarBit(cacheTypes.varbits.getValue(2169),1)
            val hotspot=locTypes.getValue(26865)
            placeMapLoc(CoordGrid(1914,5698),hotspot,LocShape[22],LocAngle[2])
            val cell=HouseRoom.ENTRANCE_CELL+1
            d.rooms.replace(player,d.rooms.layout(player)+(cell to HouseRoom(cell,HouseRoomType.Menagerie).furnish(3,1)))
            val entry=checkNotNull(d.house.prepare(player,false))
            val mat=locTypes.getValue(26866)
            val tile=entry.translate(6,0)
            val arena=BoundLocInfo(checkNotNull(findLoc(tile,mat)),mat)
            player.teleport(tile)
            fun add(id:Int){
                val item=objTypes.getValue(id)
                player.inv[2]=InvObj(item)
                player.withProtectedAccess{check(eventBus.publish(this,LocUDefaultEvents.OpType(arena,arena,mat,item,2)))}
            }
            add(24701);add(1561)
            val holding=player.invMap[cacheTypes.invs.getValue(65012)]!!
            assertEquals(listOf(24701,1561),holding.filterNotNull().map{it.id})
            add(6670)
            assertEquals(6670,player.inv[2]?.id)
            advance(5)
            player.withProtectedAccess{d.house.leave(this)}
            val insurance=cacheTypes.interfaces.values.single{it.internalName=="pet_insurance"}
            player.withProtectedAccess{ifOpenMainModal(insurance)}
            player.ifButton(cacheTypes.components.values.single{it.internalName=="pet_insurance:pets"},comsub=6+29*5)
            advance(2)
            assertEquals(0,player.inv.count(objTypes.getValue(20659)))
            player.ifClose()
            val reentry=checkNotNull(d.house.prepare(player,false))
            val pet=d.npcs.single{it.id==9638 && it.isSlotAssigned && it.coords.chebyshevDistance(reentry)<16}
            player.teleport(pet.coords.translate(0,-1))
            player.opNpc3(pet);advance(3)
            assertEquals(1,player.inv.count(objTypes.getValue(24701)))
            assertEquals(listOf(1561),holding.filterNotNull().map{it.id})
        }

    @Test fun GameTestState.`oak pet house shares its three places between unique forms and extras and native Take returns each item`()=
        runInjectedGameTest(Deps::class,null,StarterHouseScript::class,Menagerie::class){d->
            player.members=true
            player.setVarBit(varbits.poh_house_location,1)
            val hotspot=locTypes.getValue(26296)
            placeMapLoc(CoordGrid(1916,5700),hotspot,LocShape[10],LocAngle[0])
            val cell=HouseRoom.ENTRANCE_CELL+1
            d.rooms.replace(player,d.rooms.layout(player)+(cell to HouseRoom(cell,HouseRoomType.Menagerie).furnish(1,1)))
            val entry=checkNotNull(d.house.prepare(player,false))
            val at=entry.translate(8,2)
            val oak=locTypes.getValue(26297)
            var bound=BoundLocInfo(checkNotNull(findLoc(at,oak)),oak)
            player.teleport(at.translateX(-1))
            fun use(id:Int){
                val item=objTypes.getValue(id)
                player.inv[5]=InvObj(item)
                player.withProtectedAccess{check(eventBus.publish(this,LocUDefaultEvents.OpType(bound,bound,oak,item,5)))}
            }
            use(24701);use(3695);use(3695)
            val store=player.invMap[cacheTypes.invs.getValue(517)]!!
            assertEquals(2,store.count(objTypes.getValue(3695)))
            assertEquals(listOf(24701),d.pets.storedInMenagerie(player).map{it.item})
            use(6670);assertEquals(6670,player.inv[5]?.id)
            player.withProtectedAccess { d.house.leave(this) }
            val returnTile=checkNotNull(d.house.prepare(player,false)).translate(8,2)
            bound=BoundLocInfo(checkNotNull(findLoc(returnTile,oak)),oak)
            player.teleport(returnTile.translateX(-1));player.opLoc1(bound);advance(3)
            val list=cacheTypes.components.values.single{it.internalName=="poh_menagerie:list"}
            player.ifButton(list,comsub=64,op=IfButtonOp.Op1);advance(2)
            assertEquals(1,store.count(objTypes.getValue(3695)))
            assertEquals(1,player.inv.count(objTypes.getValue(3695)))
            val unique=d.pets.storedInMenagerie(player).single().index
            player.ifButton(list,comsub=unique,op=IfButtonOp.Op1);advance(2)
            assertEquals(1,player.inv.count(objTypes.getValue(24701)))
            assertTrue(d.pets.storedInMenagerie(player).isEmpty())
        }
}
