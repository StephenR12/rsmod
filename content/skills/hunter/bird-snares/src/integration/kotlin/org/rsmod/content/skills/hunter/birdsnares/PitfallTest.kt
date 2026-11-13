package org.rsmod.content.skills.hunter.birdsnares

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class PitfallTest {
    @Test
    fun GameTestState.`native pit morph Trap operation builds and dismantles without modifying player save bits`() = runPitfallTest { deps ->
        prepare()
        val origin=CoordGrid(2000,2000)
        val source=placeMapLoc(origin,PitfallLocs.sources[7])
        player.inv[0]=InvObj(objs.knife);player.inv[1]=InvObj(objs.logs)
        val nativeBit=deps.locs[PitfallLocs.sources[7]].multiVarBit
        val bitBefore=player.vars[deps.varbits.getValue(nativeBit)]
        player.opLoc3(source);advance(12)
        assertEquals(0,player.count(objs.logs))
        assertTrue(locExists(origin,PitfallLocs.set))
        player.opLoc2(bound(deps,origin,PitfallLocs.set.id));advance(3)
        assertTrue(locExists(origin,PitfallLocs.sources[7]))
        assertEquals(bitBefore,player.vars[deps.varbits.getValue(nativeBit)])
    }

    @Test
    fun GameTestState.`five-tier tease jump and dismantle awards exact xp supplies and three diaries`() = runPitfallTest { deps ->
        prepare()
        val tasks=listOf(DiaryTask.CatchSpinedLarupia,DiaryTask.CatchHornedGraahk,DiaryTask.CatchSabreKyatt)
        for(species in PitfallSpeciesList.all) {
            val origin=CoordGrid(2000+species.index*8,2000)
            val pit=preparePit(species,origin)
            player.withProtectedAccess { deps.traps.setup(this,pit,species) };advance(4)
            val npc=spawnNpc(CoordGrid(origin.x-3,origin.z),deps.npcs[species.npc]) { noneMode() }
            random.next=0
            player.withProtectedAccess { deps.traps.tease(this,npc,species) }
            assertEquals(NpcMode.PlayerFollow,npc.mode)
            player.clearSoftTimer(PitfallTimers.lure);npc.noneMode()
            random.then=0
            if(species.index<3) random.then=0
            if(species.index==3) random.then=2
            player.withProtectedAccess { deps.traps.jump(this,bound(deps,origin,PitfallLocs.set.id)) };advance(4)
            val xp=player.statMap.getFineXP(stats.hunter)
            player.withProtectedAccess { deps.traps.dismantle(this,bound(deps,origin,PitfallLocs.full[species.index].id)) }
            assertEquals((species.xp*10).toInt(),player.statMap.getFineXP(stats.hunter)-xp)
            assertEquals(1,player.count(objs.big_bones))
            assertEquals(1,player.count(species.meat))
            assertEquals(1,player.count(species.fur))
            if(species.index>=3) assertEquals(1,player.count(PitfallObjs.antlers[species.index-3]))
            if(species.index==3) assertEquals(2,player.count(PitfallObjs.splinters))
            assertTrue(locExists(origin,PitfallLocs.empty))
            tasks.getOrNull(species.index)?.let { assertTrue(player.hasCompletedDiaryTask(it)) }
        }
    }

    @Test
    fun GameTestState.`antelope loot survives full inventory until collection and logout restores original scene`() = runPitfallTest { deps ->
        prepare()
        val species=PitfallSpeciesList.all[3];val origin=CoordGrid(2000,2000);val pit=preparePit(species,origin)
        player.withProtectedAccess { deps.traps.setup(this,pit,species) };advance(4)
        val npc=spawnNpc(CoordGrid(1997,2000),deps.npcs[species.npc]) { noneMode() }
        random.next=0
        player.withProtectedAccess { deps.traps.tease(this,npc,species) };player.clearSoftTimer(PitfallTimers.lure);npc.noneMode()
        random.then=0;random.then=6
        player.withProtectedAccess { deps.traps.jump(this,bound(deps,origin,PitfallLocs.set.id)) };advance(4)
        player.fillInv()
        val xp=player.statMap.getFineXP(stats.hunter)
        player.withProtectedAccess { deps.traps.dismantle(this,bound(deps,origin,PitfallLocs.full[3].id)) }
        assertEquals(xp,player.statMap.getFineXP(stats.hunter))
        player.clearInv()
        player.withProtectedAccess { deps.traps.dismantle(this,bound(deps,origin,PitfallLocs.full[3].id)) }
        assertEquals(6,player.count(PitfallObjs.splinters))
        assertEquals(1,player.count(PitfallObjs.antlers[0]))
        val next=preparePit(species,origin)
        player.withProtectedAccess { deps.traps.setup(this,next,species) };advance(4)
        deps.traps.clear(player)
        assertTrue(locExists(origin,PitfallLocs.empty))
        assertEquals(0,player.count(objs.logs))
    }

    @Test
    fun GameTestState.`pursuing creature deals bounded melee damage and protection prayer prevents it`() = runPitfallTest { deps ->
        prepare()
        val species=PitfallSpeciesList.all[0]
        player.inv[0]=InvObj(PitfallObjs.stick)
        val npc=spawnNpc(player.coords.translateX(1),deps.npcs[species.npc]) { noneMode() }
        random.next=0;random.then=5
        player.opNpc1(npc)
        advance(4)
        assertEquals(94,player.stats[stats.hitpoints])
        player.setVarBit(varbits.protect_from_melee,1)
        random.then=5
        advance(4)
        assertEquals(94,player.stats[stats.hitpoints])
        deps.traps.clear(player)
        advance()
        assertEquals(NpcMode.Wander,npc.mode)
    }

    @Test
    fun GameTestState.`requirements reject unsupported logs and missing tools while high antelope catches are guaranteed`() = runPitfallTest { deps ->
        prepare()
        val species=PitfallSpeciesList.all[0]
        val pit=placeMapLoc(CoordGrid(2000,2000),PitfallLocs.empty)
        player.inv[0]=InvObj(objs.redwood_logs);player.inv[1]=InvObj(objs.knife)
        player.withProtectedAccess { deps.traps.setup(this,pit,species) }
        assertEquals(1,player.count(objs.redwood_logs))
        player.inv[0]=InvObj(objs.logs);player.inv[1]=null
        player.withProtectedAccess { deps.traps.setup(this,pit,species) }
        assertEquals(1,player.count(objs.logs))
        player.inv[1]=InvObj(objs.knife);player.stats[stats.hunter]=30
        player.withProtectedAccess { deps.traps.setup(this,pit,species) }
        assertEquals(1,player.count(objs.logs))
        assertEquals(1.0,deps.traps.approximateCatchChance(85,PitfallSpeciesList.all[1]))
        assertEquals(1.0,deps.traps.approximateCatchChance(72,PitfallSpeciesList.all[3]))
        assertEquals(1.0,deps.traps.approximateCatchChance(91,PitfallSpeciesList.all[4]))
    }
    private fun GameTestScope.prepare() { player.members=true;player.observerUUID=1L;player.stats[stats.hunter]=99;player.stats[stats.hitpoints]=99;player.telejump(CoordGrid(1999,2000));for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(2000+dx,2000+dz)) }
    private fun GameTestScope.preparePit(species:PitfallSpecies,origin:CoordGrid):BoundLocInfo { for(dx in -8..8 step 8)for(dz in -8..8 step 8)allocZoneCollision(CoordGrid(origin.x+dx,origin.z+dz));player.telejump(origin.translateX(-1));player.clearInv();player.inv[0]=InvObj(objs.knife);player.inv[1]=InvObj(objs.logs);player.inv[2]=InvObj(PitfallObjs.stick);return placeMapLoc(origin,PitfallLocs.empty) }
    private fun GameTestScope.bound(deps:Dependencies,coords:CoordGrid,id:Int):BoundLocInfo = BoundLocInfo(deps.locRepo.findAll(coords).first { it.id==id },deps.locs.getValue(id))
    private fun GameTestState.runPitfallTest(body:GameTestScope.(Dependencies)->Unit) = runInjectedGameTest(Dependencies::class,TestModule,scripts=arrayOf(PitfallScript::class,PlayerHitScript::class),testBody=body)
    private object TestModule:AbstractModule() { override fun configure() { install(BirdSnareModule()) } }
    data class Dependencies @Inject constructor(val traps:PitfallTrap,val locRepo:LocRepository,val locs:LocTypeList,val npcs:NpcTypeList,val varbits:VarBitTypeList)
}
