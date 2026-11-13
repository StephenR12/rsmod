package org.rsmod.content.skills.hunter.birdsnares

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
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.translate
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BoxTrapTest {
    @Test
    fun GameTestState.`cache pins all chinchompas and the embertailed jerboa to native box trap operations`() =
        runBoxTest { deps ->
            assertEquals(10008, BoxTrapObjs.snare.id)
            assertEquals(listOf(2910, 2911, 2912, 13139), BoxTrapSpeciesList.all.map { it.npc.id })
            assertEquals(listOf(53, 63, 73, 39), BoxTrapSpeciesList.all.map { it.level })
            assertEquals(listOf(198.4, 265.0, 315.0, 137.0), BoxTrapSpeciesList.all.map { it.xp })
            assertEquals(listOf(10033, 10034, 11959, 29166), BoxTrapSpeciesList.all.map { it.product.id })
            assertEquals(listOf(40), deps.rows.types.getValue(37).data[19])
            assertEquals(
                listOf("Dismantle", "Investigate", null, null, null),
                deps.locs[BoxTrapLocs.open].op.toList(),
            )
            assertEquals(
                listOf("Dismantle", "Reset", null, null, null),
                deps.locs[BoxTrapLocs.broken].op.toList(),
            )
            for (species in BoxTrapSpeciesList.all) {
                assertEquals(
                    listOf("Check", "Reset", null, null, null),
                    deps.locs[species.full].op.toList(),
                )
            }
        }

    @Test
    fun GameTestState
        .`four tick placement consumes one stacked trap and shares bird snare limits`() =
        runBoxTest { deps ->
            prepare(level = 27)
            player.setVarBit(BoxTrapVars.eaglesPeak, 0)
            player.inv[0] = InvObj(BoxTrapObjs.snare, 3)
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            assertEquals(3, player.count(BoxTrapObjs.snare))
            player.setVarBit(BoxTrapVars.eaglesPeak, 40)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(3)
            assertFalse(locExists(origin, BoxTrapLocs.open))
            advance()
            assertTrue(locExists(origin, BoxTrapLocs.open))
            assertEquals(2, player.count(BoxTrapObjs.snare))
            assertEquals(1, deps.traps.activeTrapCount(player))
            assertEquals(1, deps.birds.activeTrapCount(player))
            player.setVarBit(varbits.inside_wilderness, 1)
            assertEquals(3, deps.traps.maximumTraps(player))
        }

    @Test
    fun GameTestState
        .`each trapped creature awards its native product and xp after a successful catch`() =
        runBoxTest { deps ->
            prepare()
            for ((index, species) in BoxTrapSpeciesList.all.withIndex()) {
                val origin = CoordGrid(3200 + index * 8, 3200)
                player.teleport(origin)
                player.clearInv()
                player.inv[0] = InvObj(BoxTrapObjs.snare)
                player.withProtectedAccess { deps.traps.lay(this, 0) }
                advance(4)
                player.teleport(origin.translate(-1, 0))
                random.next = 0
                val npc = spawnNpc(origin.translate(1, 0), deps.npcs[species.npc])
                npc.opLoc1(bound(deps, origin, BoxTrapLocs.open), deps.aiLoc)
                advance(1 + BoxTrap.TRANSITION_TICKS)
                assertTrue(locExists(origin, species.full))
                val xp = player.statMap.getFineXP(stats.hunter)
                player.withProtectedAccess {
                    deps.traps.check(this, bound(deps, origin, species.full))
                }
                advance(BoxTrap.CHECK_TICKS)
                assertEquals(1, player.count(species.product))
                assertEquals(1, player.count(BoxTrapObjs.snare))
                assertEquals((species.xp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - xp)
                assertEquals(0, deps.traps.activeTrapCount(player))
            }
        }

    @Test
    fun GameTestState
        .`failed trap Reset keeps the same owned trap and creates no inventory copy`() =
        runBoxTest { deps ->
            prepare()
            player.inv[0] = InvObj(BoxTrapObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(4)
            player.teleport(origin.translate(-1, 0))
            random.next = 99
            val npc = spawnNpc(origin.translate(1, 0), deps.npcs[BoxTrapNpcs.red])
            npc.opLoc1(bound(deps, origin, BoxTrapLocs.open), deps.aiLoc)
            advance(1 + BoxTrap.TRANSITION_TICKS)
            assertTrue(locExists(origin, BoxTrapLocs.broken))
            deps.npcRepo.despawn(npc, Int.MAX_VALUE)
            player.withProtectedAccess {
                deps.traps.reset(this, bound(deps, origin, BoxTrapLocs.broken))
            }
            advance(4)
            assertTrue(locExists(origin, BoxTrapLocs.open))
            assertEquals(0, player.count(BoxTrapObjs.snare))
            assertEquals(1, deps.traps.activeTrapCount(player))
        }

    @Test
    fun GameTestState.`full inventory leaves a caught chinchompa intact for retry`() =
        runBoxTest { deps ->
            prepare()
            player.inv[0] = InvObj(BoxTrapObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(4)
            player.teleport(origin.translate(-1, 0))
            random.next = 0
            val npc = spawnNpc(origin.translate(1, 0), deps.npcs[BoxTrapNpcs.grey])
            npc.opLoc1(bound(deps, origin, BoxTrapLocs.open), deps.aiLoc)
            advance(1 + BoxTrap.TRANSITION_TICKS)
            player.fillInv()
            val xp = player.statMap.getFineXP(stats.hunter)
            player.withProtectedAccess {
                deps.traps.check(this, bound(deps, origin, BoxTrapLocs.fullGrey))
            }
            advance(4)
            assertTrue(locExists(origin, BoxTrapLocs.fullGrey))
            assertEquals(xp, player.statMap.getFineXP(stats.hunter))
            player.clearInv()
            player.withProtectedAccess {
                deps.traps.check(this, bound(deps, origin, BoxTrapLocs.fullGrey))
            }
            advance(4)
            assertEquals(1, player.count(BoxTrapObjs.grey))
        }

    @Test
    fun GameTestState.`abandoned boxes collapse after one minute into private owner loot`() =
        runBoxTest { deps ->
            prepare()
            player.inv[0] = InvObj(BoxTrapObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(4)
            advance(BoxTrap.IDLE_TICKS)
            assertFalse(locExists(origin, BoxTrapLocs.open))
            val fallen =
                deps.objRepo.findAll(origin).singleOrNull { it.type == BoxTrapObjs.snare.id }
            assertNotNull(fallen)
            assertTrue(fallen!!.isPrivate)
            assertEquals(0, deps.traps.activeTrapCount(player))
        }

    private fun GameTestScope.prepare(level: Int = 99) {
        val origin = CoordGrid(3200, 3200)
        allocZoneCollision(origin)
        player.placeAt(origin)
        player.observerUUID = 1L
        player.members = true
        player.stats[stats.hunter] = level
        player.setVarBit(BoxTrapVars.eaglesPeak, 40)
    }

    private fun bound(deps: Dependencies, coords: CoordGrid, type: LocType): BoundLocInfo =
        BoundLocInfo(requireNotNull(deps.locRepo.findExact(coords, type)), deps.locs[type])

    private fun GameTestState.runBoxTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, BoxTrapScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(BirdSnareModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val traps: BoxTrap,
        val birds: BirdSnareTrap,
        val locs: LocTypeList,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val rows: DbRowTypeList,
        val locRepo: LocRepository,
        val npcRepo: NpcRepository,
        val objRepo: ObjRepository,
        val aiLoc: AiLocInteractions,
    )
}
