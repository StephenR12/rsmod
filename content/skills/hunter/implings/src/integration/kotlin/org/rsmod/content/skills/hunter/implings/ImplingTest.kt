package org.rsmod.content.skills.hunter.implings

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.game.area.AreaIndex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.game.interact.HeldOp
import org.rsmod.api.player.hunter.reserveImplingCatch
import org.rsmod.api.player.hunter.canCatchImpling
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.entity.NpcList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
class ImplingTest {
    @Test fun GameTestState.`all native species and jar operations retain cache identity and exact curves`()=test {d->
        assertEquals(12,ImplingSpeciesList.all.size)
        assertEquals(208,ImplingLoot.tables.values.sumOf{it.size})
        for(s in ImplingSpeciesList.all){
            for(n in s.wild+s.maze)assertEquals("Catch",d.npcs[n].op[0])
            assertEquals("Loot",d.objs[s.jar].iop[2])
            assertTrue(ImplingHunting.chance(s,99,true)>=ImplingHunting.chance(s,99,false))
        }
        assertEquals(listOf(17,22,28,36,42,50,58,65,74,80,83,89),ImplingSpeciesList.all.map{it.level})
    }
    @Test fun GameTestState.`every species can be jarred with exact wild xp through native Catch`()=test{d->
        prepare()
        for(s in ImplingSpeciesList.all){
            player.clearInv();player.inv[0]=InvObj(ImplingObjs.empty)
            player.righthand=InvObj(ImplingObjs.magicNet)
            val npc=spawnNpc(player.coords.translateZ(1),d.npcs[s.wild.first()]){noneMode()}
            val xp=player.statMap.getFineXP(stats.hunter);random.next=0
            player.opNpc1(npc);advance(2)
            assertFalse(npc.isValidTarget(),s.name);assertEquals(1,player.count(s.jar),s.name)
            assertEquals((s.wildXp*10).toInt(),player.statMap.getFineXP(stats.hunter)-xp,s.name)
        }
    }
    @Test fun GameTestState.`Puro requires a jar even barehanded and awards essence diary only on success`()=test{d->
        prepare();player.telejump(CoordGrid(2594,4320));player.righthand=null
        val s=ImplingSpeciesList.all.first{it.number==5};val npc=spawnNpc(player.coords.translateX(1),d.npcs[s.maze.first()]){noneMode()}
        player.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2)
        assertTrue(npc.isValidTarget());assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchPuroEssenceEclectic))
        player.inv[0]=InvObj(ImplingObjs.empty);random.next=0
        player.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2)
        assertEquals(1,player.count(s.jar));assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchPuroEssenceEclectic))
    }
    @Test fun GameTestState.`barehand requirement is ten levels higher and failed catches preserve jars`()=test{d->
        prepare();player.righthand=null;player.stats[stats.hunter]=26
        val s=ImplingSpeciesList.all.first();val npc=spawnNpc(player.coords.translateZ(1),d.npcs[s.wild.first()]){noneMode()}
        player.inv[0]=InvObj(ImplingObjs.empty)
        player.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2);assertTrue(npc.isValidTarget())
        player.stats[stats.hunter]=27;random.next=99
        player.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2);assertTrue(npc.isValidTarget());assertEquals(1,player.count(ImplingObjs.empty))
    }
    @Test fun GameTestState.`native Loot consumes the jar awards table reward and independently returns empty jar`()=test{d->
        prepare();val s=ImplingSpeciesList.all.first();player.inv[0]=InvObj(s.jar)
        for(n in listOf(0,1,1,1,1))random.then=n
        player.withProtectedAccess{d.held.interact(this,player.inv,0,HeldOp.Op3)};advance()
        assertEquals(0,player.count(s.jar));assertEquals(1,player.count(objs.chisel));assertEquals(1,player.count(ImplingObjs.empty))
    }
    @Test fun GameTestState.`dragon quest gated javelins are excluded from rerolled main table until mm2 completion`()=test{
        val table=ImplingLoot.tables.getValue("Dragon");assertEquals(1,table.count{it.mm2})
        val gated=table.first{it.mm2}
        for(i in 0 until table.filter{!it.mm2}.sumOf{it.weight})assertFalse(ImplingLoot.select("Dragon",i,false).mm2)
        assertTrue((0 until table.sumOf{it.weight}).any{ImplingLoot.select("Dragon",it,true)==gated})
    }
    @Test fun GameTestState.`Elnock free kit is atomic and one time before exchanges consume only their costs`()=test{d->
        prepare();player.withProtectedAccess{d.elnock.talk(this)}
        assertEquals(1,player.count(ImplingObjs.net));assertEquals(7,player.count(ImplingObjs.empty));assertEquals(1,player.vars[ImplingBits.freeKit])
        player.clearInv();player.inv[0]=InvObj(ImplingObjs.jars[0])
        player.withProtectedAccess{assertTrue(d.elnock.tradeItems(this,listOf(ImplingObjs.jars[0].id to 1),ImplingObjs.empty.id,3))}
        assertEquals(3,player.count(ImplingObjs.empty));assertEquals(0,player.count(ImplingObjs.jars[0]))
    }
    @Test fun GameTestState.`generator preserves charge across 33 impling jars then final butterfly jar consumes it`()=test{
        prepare();player.inv[0]=InvObj(ImplingObjs.generator)
        repeat(33){player.withProtectedAccess{JarGenerator().generate(this,0,true)};assertEquals(1,player.count(ImplingObjs.empty));player.inv[1]=null}
        assertEquals(99,player.inv[0]?.vars)
        player.withProtectedAccess{JarGenerator().generate(this,0,true)};assertEquals(0,player.count(ImplingObjs.empty));assertEquals(99,player.inv[0]?.vars)
        player.withProtectedAccess{JarGenerator().generate(this,0,false)}
        assertEquals(0,player.count(ImplingObjs.generator));assertEquals(1,player.count(ImplingObjs.butterflyJar))
    }
    @Test fun GameTestState.`Puro entry checks base Hunter grants affinity and native vortex tiles return to entrance`()=test{d->
        prepare();val origin=player.coords;val circle=placeMapLoc(origin.translateZ(1),PuroLocs.circle)
        player.setBaseLevel(stats.hunter,16);player.setCurrentLevel(stats.hunter,99)
        player.withProtectedAccess{d.puro.enter(this,circle)};assertEquals(origin,player.coords)
        player.setBaseLevel(stats.hunter,17);player.withProtectedAccess{d.puro.enter(this,circle)}
        assertEquals(CoordGrid(2593,4320),player.coords);assertTrue(player.vars[PuroVars.affinity]>PuroPuro.now())
        assertEquals(1,player.vars[ElnockBits.strengthXpOff])
        // The integration harness starts with an empty AreaIndex; register the packed centre tile.
        d.areas.registerAll(CoordGrid(2592,4320),listOf(PuroAreas.exit.id.toShort()).iterator())
        player.telejump(CoordGrid(2592,4320));advance(2)
        assertEquals(origin,player.coords)
    }
    @Test fun GameTestState.`Lucky single roll tables sum to one exclude casket tertiary and retain potion bundles`()=test{d->
        assertEquals(listOf(211,192,233,182,173),LuckyImplingLoot.tables.map{it.size})
        for(table in LuckyImplingLoot.tables)assertEquals(1.0,table.sumOf{it.probability},1e-12)
        val bundle=LuckyImplingLoot.tables[2].single{it.items.size==3}
        assertEquals(listOf(5,5,5),bundle.items.map{it.minimum})
        assertEquals(1.0/16250,bundle.probability,1e-15)
        assertTrue(LuckyImplingLoot.tables.flatten().flatMap{it.items}.none{d.objs[it.obj].name.contains("clue scroll",true)||d.objs[it.obj].name.contains("Bloodhound",true)})
        prepare();val lucky=ImplingSpeciesList.all.first{it.number==11};player.inv[0]=InvObj(lucky.jar)
        random.next=0;random.then=0;random.then=1
        player.withProtectedAccess{d.held.interact(this,player.inv,0,HeldOp.Op3)};advance()
        assertEquals(0,player.count(lucky.jar));assertEquals(1,player.count(LuckyImplingLoot.tables[0][0].items.single().obj));assertEquals(1,player.count(ImplingObjs.empty))
    }

    @Test fun GameTestState.`binding catch priority permits tagger blocks other hunter then expires after five ticks`()=test{d->
        prepare();val s=ImplingSpeciesList.all.first()
        val npc=spawnNpc(player.coords.translateZ(1),d.npcs[s.wild.first()]){noneMode()}
        val other=registerPlayer(player.coords);other.members=true;other.stats[stats.hunter]=99;other.righthand=InvObj(ImplingObjs.magicNet);other.inv[0]=InvObj(ImplingObjs.empty)
        npc.reserveImplingCatch(player)
        assertTrue(npc.canCatchImpling(player));assertFalse(npc.canCatchImpling(other))
        other.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2)
        assertTrue(npc.isValidTarget());assertEquals(0,other.count(s.jar))
        advance(3);assertTrue(npc.canCatchImpling(other));random.next=0
        other.withProtectedAccess{d.hunting.catch(this,npc,s)};advance(2)
        assertEquals(1,other.count(s.jar));assertFalse(npc.isValidTarget())
    }

    @Test fun GameTestState.`invisible precursors stay hidden and high Puro roller respawns after capture`()=test{d->
        prepare();player.telejump(CoordGrid(2580,4336))
        for(type in ImplingNpcs.precursors){val def=d.npcs[type];assertTrue(def.multiNpc.isNotEmpty());assertTrue(def.multiNpc.all{it.toInt()==-1})}
        val seed=spawnNpc(player.coords.translateZ(1),d.npcs[ImplingNpcs.precursors[2]]){noneMode()}
        random.next=300;random.then=0;d.population.roll(seed);advance()
        val lucky=d.liveNpcs.first{it.id==7302&&it.isValidTarget()}
        player.righthand=InvObj(ImplingObjs.magicNet);player.inv[0]=InvObj(ImplingObjs.empty);random.next=0
        player.withProtectedAccess{d.hunting.catch(this,lucky,ImplingSpeciesList.all.first{it.number==11})};advance(2)
        assertFalse(lucky.isValidTarget());assertEquals(1,player.count(ImplingObjs.jars[10]));assertTrue(d.liveNpcs.any{it.id==ImplingNpcs.precursors[2].id&&it.isValidTarget()})
    }

    private fun GameTestScope.prepare(){player.members=true;player.observerUUID=1L;player.stats[stats.hunter]=99;player.telejump(CoordGrid(2000,2000));for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(2000+dx,2000+dz))}
    private fun GameTestState.test(body:GameTestScope.(Deps)->Unit)=runInjectedGameTest(Deps::class,scripts=arrayOf(ImplingScript::class,JarGenerator::class,PuroPuroScript::class),testBody=body)
    data class Deps @Inject constructor(val hunting:ImplingHunting,val elnock:ElnockInquisitor,val puro:PuroPuro,val npcs:NpcTypeList,val objs:ObjTypeList,val held:HeldInteractions,val areas:AreaIndex,val population:ImplingPopulation,val liveNpcs:NpcList)
}
