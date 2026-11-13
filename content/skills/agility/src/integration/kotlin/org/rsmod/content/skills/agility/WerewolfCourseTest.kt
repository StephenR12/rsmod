package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.ObjEvents
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.NpcList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class WerewolfCourseTest {
    data class Deps @Inject constructor(val ground: ObjRepository,val npcs:NpcList)

    @Test fun GameTestState.`a genuine course stick is collectible after the pipe and rewards one completed lap`()=
        runInjectedGameTest(Deps::class,null,WerewolfCourse::class){ deps ->
            fun tile(x:Int,z:Int)=CoordGrid(x,z)
            val start=tile(3538,9874)
            for(x in 3526..3546 step 4) for(z in 9870..9914 step 4)allocZoneCollision(tile(x,z))
            player.members=true
            player.observerUUID=1L
            for(skill in listOf(stats.agility,stats.strength)){
                player.setBaseLevel(skill,80);player.setCurrentLevel(skill,80)
            }
            val stoneType=locTypes.values.single{it.internalName=="werewolf_steping_stone"}
            val first=placeMapLoc(tile(3538,9875),stoneType,LocShape.GroundDecor)
            player.teleport(start)
            player.opLoc1(first);advance(5)
            assertEquals(start,player.coords)
            player.setVarp(cacheTypes.varps.getValue(399),9)
            player.worn[Wearpos.Ring.slot]=InvObj(objTypes.values.single{it.internalName=="ring_of_charos"})
            val before=player.statMap.getFineXP(stats.agility)
            player.opLoc1(first);advance(5)
            assertEquals(first.coords,player.coords)
            for(at in listOf(tile(3538,9877),tile(3540,9877),tile(3540,9879),tile(3540,9881))){
                player.opLoc1(placeMapLoc(at,stoneType,LocShape.GroundDecor));advance(5)
            }
            assertEquals(tile(3540,9883),player.coords)
            val stick=listOf(3538,3541,3544).firstNotNullOf{ x -> deps.ground.findAll(tile(x,9910)).firstOrNull() }
            fun take(){
                player.withProtectedAccess { check(eventBus.publish(this,ObjEvents.Op3(stick))) }
            }
            player.teleport(stick.coords);take()
            assertEquals(0,player.count(objTypes.values.single{it.internalName=="waa_stick"}))
            fun cross(name:String,loc:CoordGrid,from:CoordGrid,ground:Boolean=false){
                val type=locTypes.values.single{it.internalName==name}
                val target=placeMapLoc(loc,type,if(ground)LocShape.GroundDecor else LocShape.CentrepieceStraight, LocAngle[if (name == "waa_pipe" || name=="werewolf_skull_climb_1") 3 else 0])
                player.teleport(from);player.opLoc1(target);advance(12)
            }
            for(z in listOf(9893,9896,9899)) cross("werewolf_hurdle_mid",tile(3539,z),tile(3539,z-1))
            cross("waa_pipe",tile(3538,9905),tile(3538,9904))
            assertEquals(tile(3538,9910),player.coords)
            player.teleport(stick.coords);take();take()
            assertEquals(1,player.count(objTypes.values.single{it.internalName=="waa_stick"}))
            cross("werewolf_skull_climb_1",tile(3532,9910),tile(3533,9910),true)
            val slideType=locTypes.values.single{it.internalName=="werewolf_slide_center"}
            val slide=placeMapLoc(tile(3528,9911),slideType)
            player.teleport(tile(3528,9910))
            player.worn[Wearpos.Hat.slot]=InvObj(objTypes.values.single{it.internalName=="bronze_full_helm"})
            player.opLoc1(slide);advance(12)
            assertEquals(tile(3528,9910),player.coords)
            player.worn[Wearpos.Hat.slot]=null
            player.opLoc1(slide);advance(12)
            assertEquals(tile(3528,9873),player.coords)
            val trainer=spawnNpc(tile(3530,9865),npcTypes.values.single{it.internalName=="werewolf_trainer_stick"})
            player.teleport(trainer.coords.translateX(1))
            player.opNpc1(trainer);advance(3);player.opNpc1(trainer);advance(3)
            assertEquals(0,player.count(objTypes.values.single{it.internalName=="waa_stick"}))
            assertEquals(7300,player.statMap.getFineXP(stats.agility)-before)
        }

    @Test fun GameTestState.`only the ten ordered Skullball goals and final hole finish the timed game`()=
        runInjectedGameTest(Deps::class,null,Skullball::class){deps->
            fun tile(x:Int,z:Int)=CoordGrid(x,z)
            for(x in 3547..3580 step 4)for(z in 9859..9916 step 4)allocZoneCollision(tile(x,z))
            player.members=true;player.observerUUID=1L
            player.setBaseLevel(stats.agility,25);player.setCurrentLevel(stats.agility,25)
            player.setVarp(cacheTypes.varps.getValue(399),9)
            player.worn[Wearpos.Ring.slot]=InvObj(objTypes.values.single{it.internalName=="ring_of_charos"})
            val boss=spawnNpc(tile(3549,9867),npcTypes.values.single{it.internalName=="werewolf_skullballboss"})
            val before=player.statMap.getFineXP(stats.agility)
            player.teleport(boss.coords.translateX(1));player.opNpc1(boss);advance(4)
            val ball=deps.npcs.single{it.visType.id==npcTypes.values.single{it.internalName=="waa_skullball"}.id}
            fun tap(x:Int,z:Int,dx:Int,dz:Int){
                val from=tile(x-dx,z-dz)
                ball.telejump(from);player.teleport(from.translate(-dx,-dz))
                player.opNpc1(ball);advance(3)
            }
            tap(3563,9866,0,1)
            assertEquals(before,player.statMap.getFineXP(stats.agility))
            for((x,z) in listOf(3555 to 9870,3556 to 9883,3558 to 9891,3557 to 9900,3558 to 9906))tap(x,z,0,1)
            tap(3563,9911,1,0)
            for((x,z) in listOf(3575 to 9905,3574 to 9888,3575 to 9878))tap(x,z,0,-1)
            tap(3568,9864,-1,0)
            assertEquals(before,player.statMap.getFineXP(stats.agility))
            tap(3563,9866,0,1)
            assertEquals(before+7500,player.statMap.getFineXP(stats.agility))
        }
}
