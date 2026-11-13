package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import java.sql.DriverManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.account.character.main.CharacterAccountData
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.player.events.TelescopeObservation
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.input.ResumePStringDialogInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class HouseFloorsTest {
    data class Dependencies @Inject constructor(val homes: StarterHouse, val rooms: HouseRoomRepository, val pipeline:HouseRoomPipeline)

    @ParameterizedTest
    @ValueSource(ints=[-1,1])
    fun `build and use a matching staircase and preserve the floor through character reload`(direction:Int,state:GameTestState) =
        state.runInjectedGameTest(Dependencies::class,null,StarterHouseScript::class){d->
            player.members=true
            player.displayName="Builder"
            player.setVarBit(varbits.poh_house_location,1)
            player.setBaseLevel(stats.construction,99)
            player.setCurrentLevel(stats.construction,99)
            fun loc(id:Int)=locTypes.getValue(id)
            for((id,tile)in listOf(15380 to CoordGrid(1867,5747),15381 to CoordGrid(1883,5747),15380 to CoordGrid(1875,5723),15390 to CoordGrid(1899,5747),15391 to CoordGrid(1915,5747))){
                placeMapLoc(tile,loc(id),LocShape[10],LocAngle[0])
            }
            placeMapLoc(CoordGrid(1893,5743),loc(15424),LocShape[10],LocAngle[2])
            val cell=HouseRoom.ENTRANCE_CELL+1
            val hall=HouseRoom(cell,HouseRoomType.SkillHall)
            d.rooms.replace(player,d.rooms.layout(player)+(cell to hall))
            val entry=checkNotNull(d.homes.prepare(player,true))
            player.teleport(entry.translate(6,1))
            fun bind(tile:CoordGrid,id:Int)=BoundLocInfo(checkNotNull(findLoc(tile,loc(id))){"Missing $id at $tile"},loc(id))
            fun select(index:Int){player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list,index))}
            fun supply(){
                for(i in 3..12)player.inv[i]=InvObj(objs.oak_plank)
                for(i in 13..16)player.inv[i]=InvObj(objs.steel_bar)
            }
            player.inv[0]=InvObj(objs.hammer)
            player.inv[1]=InvObj(objTypes.values.single{it.internalName=="poh_saw"})
            player.inv[2]=InvObj(objs.coins,25000)
            supply()
            player.opLoc5(bind(entry.translate(7,1),15380));advance(2)
            select(0)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton,if(direction>0)1 else 2))
            advance(5)
            assertEquals(if(direction>0)1 else 2,d.rooms.snapshot(player).getValue(cell).variant(15))
            val groundId=if(direction>0)13497 else 13498
            val otherId=if(direction>0)13498 else 13497
            val choice=if(direction>0)1 else 2
            fun approach(tile:CoordGrid,dir:Int)=tile.translateZ(if(dir>0)-1 else 2)
            fun buy(){
                player.opLoc1(bind(player.coords.translateZ(if(direction>0)1 else -2),groundId));advance(2);select(choice);advance(2)
            }
            buy()
            assertEquals(25000,player.inv.count(objTypes[objs.coins]))
            assertNull(d.rooms.snapshot(player)[cell+direction*256])
            supply()
            buy()
            val matching=d.rooms.snapshot(player).getValue(cell+direction*256)
            assertEquals(if(direction>0)HouseRoomType.QuestHall else HouseRoomType.DungeonStairs,matching.type)
            assertEquals(if(direction>0)2 else 1,matching.variant(15))
            assertEquals(if(direction>0)0 else 17500,player.inv.count(objTypes[objs.coins]))
            assertEquals(0,player.inv.count(objTypes[objs.oak_plank]))
            assertEquals(if(direction>0)1 else 0,player.coords.level)
            player.opLoc1(bind(player.coords.translateZ(if(direction<0)1 else -2),otherId));advance(3)
            assertEquals(if(direction>0)0 else 1,player.coords.level)
            player.withProtectedAccess{d.homes.leave(this)}
            // Save via the login/logout database stage, then load into an empty character layout.
            val study=HouseRoom(matching.neighbour(1),HouseRoomType.Study).furnish(6,3)
            val layout=d.rooms.snapshot(player)+(study.cell to study)
            d.rooms.replace(player,layout)
            DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
                for(resource in listOf("V10__add_house_rooms","V11__persist_house_pet_rocks")){
                    val sql=checkNotNull(javaClass.getResource("/db/migration/$resource.sql")).readText()
                    connection.createStatement().use{it.execute(sql)}
                }
                val db=DatabaseConnection(connection)
                d.pipeline.save(db,player,1)
                d.rooms.replace(player,emptyMap())
                val account=CharacterAccountData(0,1,1,"builder","Builder","",null,true,null,false,null,null,null,null,0,0,0,emptyMap(),null,null,null,null,null,10000,1.0)
                val metadata=CharacterMetadataList(account,mutableListOf())
                d.pipeline.append(db,metadata)
                metadata.single().apply(player)
                assertEquals(layout,d.rooms.snapshot(player))
            }
            val fresh=checkNotNull(d.homes.prepare(player,false))
            player.teleport(fresh)
            val guest=registerPlayer(HouseLocation.Rimmington.outside).apply{members=true;displayName="Guest"}
            val portal=placeMapLoc(guest.coords.translateZ(1),loc(15478),LocShape[10],LocAngle[0])
            guest.opLoc4(portal);advance(2);guest.resumeActiveCoroutine(ResumePStringDialogInput("Builder"));advance(2)
            val first=bind(fresh.translate(7,1),groundId)
            val second=bind(first.coords.translate(0,0,direction),otherId)
            for(who in listOf(player,guest)){
                who.teleport(approach(first.coords,direction));who.opLoc1(first);advance(2)
                assertEquals(approach(second.coords,-direction),who.coords)
                val telescope=bind(second.coords.translate(2,12),13658)
                assertEquals(direction<0,TelescopeObservation(who,telescope).also{eventBus.publish(it)}.blocked)
                who.opLoc1(second);advance(2)
                assertEquals(approach(first.coords,direction),who.coords)
            }
        }
}
