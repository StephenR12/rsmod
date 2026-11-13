package org.rsmod.content.skills.hunter.birdsnares

import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.interact.AiLocInteractions
import org.rsmod.api.npc.opLoc1
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class NetTrapTest {
    @Test
    fun GameTestState.`five species pin native tree operations levels products and exact published success curves`() = runNetTest { deps ->
        assertEquals(listOf(2906,2903,2904,2905,12769), NetTrapSpeciesList.all.map { it.npc.id })
        assertEquals(listOf(29,47,59,67,79), NetTrapSpeciesList.all.map { it.level })
        assertEquals(listOf(152.0,224.0,272.0,319.2,344.0), NetTrapSpeciesList.all.map { it.xp })
        assertEquals(listOf(10149,10146,10147,10148,28831), NetTrapSpeciesList.all.map { it.product.id })
        for (species in NetTrapSpeciesList.all) {
            assertEquals("Set-trap", deps.locs[species.tree].op[0])
            assertEquals(listOf("Dismantle","Investigate",null,null,null), deps.locs[species.open].op.toList())
            assertEquals(listOf("Check","Reset",null,null,null), deps.locs[species.full].op.toList())
            assertEquals(listOf("Dismantle","Reset",null,null,null), deps.locs[species.broken].op.toList())
        }
        prepare()
        assertEquals(1.0, deps.traps.catchRate(player, NetTrapSpeciesList.all[0], false, false))
        val red = NetTrapSpeciesList.all[2]
        val normal = deps.traps.catchRate(player, red, false, false)
        assertEquals(0.05, deps.traps.catchRate(player, red, true, true) - normal, 0.000001)
        val tecu = NetTrapSpeciesList.all[4]
        assertEquals(deps.traps.catchRate(player, tecu, false, false), deps.traps.catchRate(player, tecu, true, true))
    }

    @Test
    fun GameTestState.`all native net-trap tiers award exact products xp tools and four diary rows after Check`() = runNetTest { deps ->
        prepare()
        val tasks = listOf(DiaryTask.CatchSwampLizard,DiaryTask.CatchOrangeSalamander,DiaryTask.CatchRedSalamander,DiaryTask.CatchBlackSalamander)
        for (species in NetTrapSpeciesList.all) {
            player.stats[stats.hunter] = 99
            val origin = CoordGrid(2000 + species.index * 8,2000)
            val tree = prepareTree(species, origin)
            player.opLoc1(tree); advance()
            val nativeMessages = client.filterIsInstance<MessageGame>().map { it.message }
            advance(11)
            assertEquals(1, deps.traps.activeTrapCount(player), "species=${species.index}, player=$player, inv=${player.inv.toList()}, messages=$nativeMessages, protected=${player.isAccessProtected}")
            assertEquals(0, player.count(NetTrapObjs.rope))
            random.next = 0
            if (species.index == 4) random.then = 1
            val npc = spawnNpc(origin.translateX(1), deps.npcs[species.npc]) { noneMode() }
            npc.opLoc1(bound(deps, origin, species.open), deps.aiLoc)
            advance(3)
            assertTrue(locExists(origin, species.full))
            val xp = player.statMap.getFineXP(stats.hunter)
            player.withProtectedAccess { deps.traps.check(this, bound(deps, origin, species.full)) }
            advance(4)
            assertEquals(1, player.count(species.product))
            assertEquals(1, player.count(NetTrapObjs.rope))
            assertEquals(1, player.count(NetTrapObjs.net))
            assertEquals((species.xp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - xp)
            assertEquals(0, deps.traps.activeTrapCount(player))
            assertTrue(locExists(origin, species.tree))
            tasks.getOrNull(species.index)?.let { assertTrue(player.hasCompletedDiaryTask(it)) }
        }
    }

    @Test
    fun GameTestState.`failed catch drops ordinary tools and native Reset reclaims them without duplicating inventory`() = runNetTest { deps ->
        prepare()
        val species = NetTrapSpeciesList.all[2]
        val origin = CoordGrid(2000,2000)
        val tree = prepareTree(species, origin)
        player.withProtectedAccess { deps.traps.lay(this, tree, species) }; advance(4)
        random.next = 99
        val npc = spawnNpc(origin.translateX(1), deps.npcs[species.npc]) { noneMode() }
        npc.opLoc1(bound(deps, origin, species.open), deps.aiLoc); advance(3)
        assertTrue(locExists(origin,species.broken))
        assertEquals(2, deps.objRepo.findAll(origin).count())
        deps.npcRepo.despawn(npc,Int.MAX_VALUE)
        player.telejump(CoordGrid(origin.x,origin.z+2))
        player.opLoc2(bound(deps,origin,species.broken)); advance(12)
        assertTrue(locExists(origin,species.open), "player=$player, inv=${player.inv.toList()}, ground=${deps.objRepo.findAll(origin).toList()}")
        assertEquals(0, deps.objRepo.findAll(origin).count())
        assertEquals(0, player.count(NetTrapObjs.rope))
        assertEquals(0, player.count(NetTrapObjs.net))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchRedSalamander))
        player.withProtectedAccess { deps.traps.dismantle(this,bound(deps,origin,species.open)) }
        assertEquals(1, player.count(NetTrapObjs.rope))
        assertEquals(1, player.count(NetTrapObjs.net))
    }

    @Test
    fun GameTestState.`full inventory preserves the caught Tecu rarity roll and Reset keeps both tools in the trap`() = runNetTest { deps ->
        prepare()
        val species = NetTrapSpeciesList.all[4]
        val origin = CoordGrid(2000,2000)
        val tree = prepareTree(species,origin)
        player.withProtectedAccess { deps.traps.lay(this,tree,species) }; advance(4)
        random.next = 0; random.then = 0
        val npc = spawnNpc(origin.translateX(1),deps.npcs[species.npc]) { noneMode() }
        npc.opLoc1(bound(deps,origin,species.open),deps.aiLoc); advance(3)
        player.fillInv()
        player.withProtectedAccess { deps.traps.check(this,bound(deps,origin,species.full),reset=true) }; advance(8)
        assertTrue(locExists(origin,species.full))
        player.clearInv()
        player.withProtectedAccess { deps.traps.check(this,bound(deps,origin,species.full),reset=true) }; advance(8)
        assertEquals(1,player.count(NetTrapObjs.grownTecu))
        assertEquals(0,player.count(NetTrapObjs.rope))
        assertEquals(0,player.count(NetTrapObjs.net))
        assertTrue(locExists(origin,species.open))
        deps.traps.clear(player)
        assertEquals(1,player.count(NetTrapObjs.rope))
        assertEquals(1,player.count(NetTrapObjs.net))
        assertTrue(locExists(origin,species.tree))
    }

    @Test
    fun GameTestState.`requirements limit active traps and expiry returns both tools to ground`() = runNetTest { deps ->
        prepare()
        val species = NetTrapSpeciesList.all[0]
        val origin = CoordGrid(2000,2000)
        val tree = prepareTree(species,origin)
        player.stats[stats.hunter] = 28
        player.withProtectedAccess { deps.traps.lay(this,tree,species) }
        assertEquals(1,player.count(NetTrapObjs.rope))
        assertEquals(0,deps.traps.activeTrapCount(player))
        player.stats[stats.hunter] = 29
        assertEquals(2,deps.traps.maximumTraps(player))
        player.setVarBit(varbits.inside_wilderness,1)
        assertEquals(3,deps.traps.maximumTraps(player))
        player.withProtectedAccess { deps.traps.lay(this,tree,species) }; advance(4)
        advance(NetTrap.IDLE_TICKS+1)
        assertEquals(0,deps.traps.activeTrapCount(player))
        assertEquals(setOf(NetTrapObjs.net.id,NetTrapObjs.rope.id),deps.objRepo.findAll(origin).map { it.type }.toSet())
        assertTrue(locExists(origin,species.tree))
    }

    private fun GameTestScope.prepare() { player.members=true; player.observerUUID=1L; player.stats[stats.hunter]=99; player.placeAt(CoordGrid(1999,2000)); allocZoneCollision(player.coords) }
    private fun GameTestScope.prepareTree(species: NetTrapSpecies,origin: CoordGrid): BoundLocInfo { for (dx in listOf(-8,0,8)) for (dz in listOf(-8,0,8)) allocZoneCollision(CoordGrid(origin.x+dx,origin.z+dz)); player.telejump(CoordGrid(origin.x,origin.z+2)); player.clearInv(); player.inv[0]=InvObj(NetTrapObjs.rope);player.inv[1]=InvObj(NetTrapObjs.net); return placeMapLoc(origin,species.tree) }
    private fun GameTestScope.bound(deps: Dependencies,coords: CoordGrid,type: LocType): BoundLocInfo { val loc=deps.locRepo.findAll(coords).firstOrNull { it.id==type.id };assertNotNull(loc);return BoundLocInfo(requireNotNull(loc),deps.locs[type]) }
    private fun GameTestState.runNetTest(body: GameTestScope.(Dependencies)->Unit) = runInjectedGameTest(Dependencies::class,TestModule,NetTrapScript::class,testBody=body)
    private object TestModule: AbstractModule() { override fun configure() { install(BirdSnareModule()) } }
    data class Dependencies @Inject constructor(val traps:NetTrap,val birds:BirdSnareTrap,val boxes:BoxTrap,val locs:LocTypeList,val npcs:NpcTypeList,val locRepo:LocRepository,val npcRepo:NpcRepository,val objRepo:ObjRepository,val aiLoc:AiLocInteractions)
}
