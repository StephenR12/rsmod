package org.rsmod.content.skills.firemaking

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.LocUInteractions
import jakarta.inject.Inject
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

class ForestersCampfireTest {
    class Dependencies @Inject constructor(val ops: LocUInteractions, val ground: ObjRepository, val ignition: Firemaking)
    @Test fun GameTestState.colouredPermanentFireCanBeTendedAndReturnsAfterOneAshDrop()=
        runInjectedGameTest(Dependencies::class,null,ForestersCampfire::class){deps->
            player.members=true;player.stats[stats.firemaking]=60
            val pos=CoordGrid(3201,3201)
            val base=placeMapLoc(pos,firemaking_locs.redFire)
            player.placeAt(pos.translate(0,-1))
            val log=objTypes[objs.oak_logs]
            val initial=player.statMap.getFineXP(stats.firemaking)
            for(i in 0..3)player.inv[i]=InvObj(log)
            fun use(target:BoundLocInfo,slot:Int){
                player.withProtectedAccess { deps.ops.interactOp(this,target,target,locTypes[target],log,inv,slot) }
            }
            use(base,0)
            val campType=locTypes.getValue(49928)
            val camp=BoundLocInfo(requireNotNull(findLoc(pos,campType)),campType)
            assertEquals(3,player.count(log))
            val other=placeMapLoc(pos.translate(1,0),firemaking_locs.fire)
            use(other,1)
            assertEquals(3,player.count(log))
            player.opLoc1(camp);advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a,2));advance(12)
            assertEquals(1,player.count(log));assertEquals(1800,player.statMap.getFineXP(stats.firemaking)-initial)
            advance(98)
            assertNotNull(findLoc(pos,campType))
            advance(20)
            assertNull(findLoc(pos,campType))
            assertNotNull(findLoc(pos,firemaking_locs.redFire))
            assertEquals(1,deps.ground.findAll(pos).filter{it.type==objs.ashes.id}.sumOf{it.count})
        }

    @Test fun GameTestState.playerLitFireLeavesOneAshPileWhenTheCampfireOutlivesTheOriginalFire() =
        runInjectedGameTest(Dependencies::class,FiremakingModule(),ForestersCampfire::class,FiremakingScript::class){deps->
            player.members=true
            player.observerUUID=1L
            player.stats[stats.firemaking]=99
            val pos=CoordGrid(3203,3225)
            player.placeAt(pos);allocZoneCollision(pos)
            player.inv[0]=InvObj(objs.tinderbox)
            player.inv[1]=InvObj(objs.logs)
            player.inv[2]=InvObj(objs.oak_logs)
            random.next=0;random.then=Firemaking.MIN_FIRE_TICKS
            player.withProtectedAccess{deps.ignition.lightFromInventory(this,1,FiremakingRecipes.byLog.getValue(objs.logs.id))}
            advance(Firemaking.ATTEMPT_TICKS+1)
            val baseType=locTypes[firemaking_locs.fire]
            val base=BoundLocInfo(requireNotNull(findLoc(pos,baseType)),baseType)
            val log=objTypes[objs.oak_logs]
            player.withProtectedAccess{deps.ops.interactOp(this,base,base,baseType,log,inv,2)}
            val campType=locTypes.getValue(49927)
            assertNotNull(findLoc(pos,campType))
            advance(Firemaking.MIN_FIRE_TICKS+1)
            assertNotNull(findLoc(pos,campType))
            assertTrue(deps.ground.findAll(pos).none{it.type==objs.ashes.id})
            advance(110-Firemaking.MIN_FIRE_TICKS)
            assertNull(findLoc(pos,campType));assertNull(findLoc(pos,baseType))
            assertEquals(1,deps.ground.findAll(pos).filter{it.type==objs.ashes.id}.sumOf{it.count})
        }
}
