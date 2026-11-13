package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.interact.AiLocInteractions
import org.rsmod.api.npc.opLoc1
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchFamily
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.events.EventBus
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.hunt.HuntVis
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.util.Rotations

@Execution(ExecutionMode.SAME_THREAD)
class DeadfallTest {
    @Test fun GameTestState.wildKebbitNativeCatchSuppliesClawsMeatAndBones() = catchTest(0)

    @Test fun GameTestState.barbTailNativeCatchSuppliesHarpoonMeatAndBones() = catchTest(1)

    @Test fun GameTestState.pricklyNativeCatchSuppliesSpikeAndBones() = catchTest(2)

    @Test fun GameTestState.sabreNativeCatchSuppliesTeethAndBones() = catchTest(3)

    @Test fun GameTestState.pyreFoxNativeCatchSuppliesFurMeatAndBones() = catchTest(4)

    private fun GameTestState.catchTest(index: Int) = game { d ->
        prepare()
        val before = player.statMap.getFineXP(stats.hunter)
        val source = source(d, index)
        player.opLoc1(source)
        advance(4)
        assertEquals(0, player.count(objs.logs))
        assertTrue(locExists(source.coords, DeadfallLocs.open))
        catch(d, index, source.coords)
        val full = bound(d, source.coords, DeadfallLocs.full[index])
        near(full)
        player.opLoc1(full)
        advance()
        for (item in DeadfallSpeciesList.all[index].loot) assertEquals(1, player.count(item))
        assertEquals(1, player.count(objs.bones))
        assertEquals(
            (DeadfallSpeciesList.all[index].xp * 10).toInt(),
            player.statMap.getFineXP(stats.hunter) - before,
        )
        assertTrue(locExists(source.coords, DeadfallLocs.boulder))
        assertEquals(0, d.limits.count(player))
    }

    @Test
    fun GameTestState.visibleLevelToolsAndEligibleLogsAreCheckedBeforeConsumption() = game { d ->
        prepare(22)
        val source = source(d, 0)
        player.withProtectedAccess { assertFalse(d.traps.setup(this, source)) }
        assertEquals(1, player.count(objs.logs))
        player.setCurrentLevel(stats.hunter, 23)
        player.inv[0] = null
        player.withProtectedAccess { assertFalse(d.traps.setup(this, source)) }
        player.inv[0] = InvObj(objs.knife)
        player.inv[1] = InvObj(objs.arctic_pine_logs)
        player.withProtectedAccess { assertFalse(d.traps.setup(this, source)) }
        player.inv[1] = InvObj(objs.logs)
        player.members = false
        player.withProtectedAccess { assertFalse(d.traps.setup(this, source)) }
        assertEquals(1, player.count(objs.logs))
    }

    @Test
    fun GameTestState.walkingOrToolLossDuringSetupPreservesLogsAndPreventsTrap() = game { d ->
        prepare()
        val source = source(d, 0)
        player.opLoc1(source)
        advance()
        player.moveGameClick(player.coords.translate(2, 0))
        advance(5)
        assertEquals(1, player.count(objs.logs))
        assertFalse(locExists(source.coords, DeadfallLocs.open))
        near(source)
        player.withProtectedAccess { d.traps.setup(this, source) }
        player.inv[0] = null
        advance(4)
        assertEquals(1, player.count(objs.logs))
        assertEquals(0, d.limits.count(player))
    }

    @Test
    fun GameTestState.onlyOneDeadfallPerHunterAndOtherHuntersCannotDismantleOrCollectIt() =
        game { d ->
            prepare()
            val first = source(d, 0)
            player.withProtectedAccess { assertTrue(d.traps.setup(this, first)) }
            advance(4)
            player.inv[1] = InvObj(objs.logs)
            val second =
                placeMapLoc(
                    CoordGrid(2363, 3538),
                    DeadfallLocs.boulder,
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            near(second)
            player.withProtectedAccess { assertFalse(d.traps.setup(this, second)) }
            assertEquals(1, player.count(objs.logs))
            val other = registerPlayer(first.coords.translate(2, 0))
            other.members = true
            other.stats[stats.hunter] = 99
            val open = bound(d, first.coords, DeadfallLocs.open)
            other.withProtectedAccess { assertFalse(d.traps.dismantle(this, open)) }
            catch(d, 0, first.coords)
            val full = bound(d, first.coords, DeadfallLocs.full[0])
            other.withProtectedAccess { assertFalse(d.traps.check(this, full)) }
            assertTrue(locExists(first.coords, DeadfallLocs.full[0]))
            near(full)
            player.withProtectedAccess { assertTrue(d.traps.check(this, full)) }
        }

    @Test
    fun GameTestState.fullBagRetainsCatchAndConversionCommitsOnlyAfterSuccessfulDelivery() =
        game { d ->
            prepare()
            val source = source(d, 0)
            player.withProtectedAccess { d.traps.setup(this, source) }
            advance(4)
            catch(d, 0, source.coords)
            val full = bound(d, source.coords, DeadfallLocs.full[0])
            near(full)
            for (i in 0..27) player.inv[i] = InvObj(objs.knife)
            val before = player.statMap.getFineXP(stats.hunter)
            var commits = 0
            d.events.subscribeUnbound(RemainsDrop::class.java) { convert(1) { commits++ } }
            player.withProtectedAccess { assertFalse(d.traps.check(this, full)) }
            assertEquals(0, commits)
            assertEquals(before, player.statMap.getFineXP(stats.hunter))
            assertTrue(locExists(source.coords, DeadfallLocs.full[0]))
            player.inv[26] = null
            player.inv[27] = null
            player.withProtectedAccess { assertTrue(d.traps.check(this, full)) }
            assertEquals(1, commits)
            assertEquals(0, player.count(objs.bones))
            assertEquals(1, player.count(DeadfallObjs.claws))
            player.withProtectedAccess { assertFalse(d.traps.check(this, full)) }
            assertEquals(1, commits)
            assertEquals(1280, player.statMap.getFineXP(stats.hunter) - before)
        }

    @Test
    fun GameTestState.baitAndSmokeProvideTheirPublishedThreeAndTwoPointBonuses() = game { d ->
        prepare()
        val source = source(d, 0)
        player.withProtectedAccess { d.traps.setup(this, source) }
        advance(4)
        val open = bound(d, source.coords, DeadfallLocs.open)
        near(open)
        player.inv[1] = InvObj(objs.raw_beef)
        player.inv[2] = InvObj(BirdSnareObjs.litTorch)
        player.withProtectedAccess {
            assertFalse(d.traps.bait(this, open, DeadfallObjs.barley))
            assertTrue(d.traps.bait(this, open, objs.raw_beef))
            assertFalse(d.traps.bait(this, open, objs.raw_beef))
            assertTrue(d.traps.smoke(this, open, BirdSnareObjs.litTorch))
        }
        assertEquals(0, player.count(objs.raw_beef))
        assertEquals(1, player.count(BirdSnareObjs.litTorch))
        val npc = spawnNpc(player.coords, d.npcs[DeadfallNpcs.all[0]])
        random.next = 94
        npc.opLoc1(open, d.ai)
        advance(4)
        assertTrue(locExists(source.coords, DeadfallLocs.full[0]))
    }

    @Test
    fun GameTestState.unsmokedFailedCatchRestoresBoulderAndLeavesCreatureAlive() = game { d ->
        prepare()
        val source = source(d, 0)
        player.withProtectedAccess { d.traps.setup(this, source) }
        advance(4)
        val open = bound(d, source.coords, DeadfallLocs.open)
        val npc = spawnNpc(player.coords, d.npcs[DeadfallNpcs.all[0]])
        val before = player.statMap.getFineXP(stats.hunter)
        random.next = 99
        npc.opLoc1(open, d.ai)
        advance(4)
        assertTrue(locExists(source.coords, DeadfallLocs.boulder))
        assertTrue(npc.isVisible)
        assertEquals(before, player.statMap.getFineXP(stats.hunter))
        assertEquals(0, d.limits.count(player))
    }

    @Test
    fun GameTestState.abandonmentAndLogoutRestoreBouldersAndReleaseOwnership() = game { d ->
        prepare()
        val source = source(d, 0)
        player.withProtectedAccess { d.traps.setup(this, source) }
        advance(104)
        assertTrue(locExists(source.coords, DeadfallLocs.boulder))
        assertEquals(0, d.limits.count(player))
        player.inv[1] = InvObj(objs.logs)
        near(source)
        player.withProtectedAccess { d.traps.setup(this, source) }
        advance(4)
        assertEquals(1, d.limits.count(player))
        d.traps.clear(player)
        assertEquals(0, d.limits.count(player))
        assertTrue(locExists(source.coords, DeadfallLocs.boulder))
    }

    @Test
    fun GameTestState.wieldedFletchingKnifeAndAuburnvaleNativeBouldersAreSupported() = game { d ->
        prepare()
        player.inv[0] = null
        player.righthand = InvObj(objs.fletching_knife)
        val source =
            placeMapLoc(
                CoordGrid(1378, 3386),
                DeadfallLocs.boulder,
                LocShape.CentrepieceStraight,
                LocAngle.East,
            )
        near(source)
        player.withProtectedAccess { assertTrue(d.traps.setup(this, source)) }
        advance(4)
        catch(d, 0, source.coords)
        val full = bound(d, source.coords, DeadfallLocs.full[0])
        near(full)
        player.withProtectedAccess { assertTrue(d.traps.check(this, full)) }
        assertEquals(1, player.count(DeadfallObjs.claws))
        assertNotNull(player.righthand)
    }

    @Test
    fun GameTestState.nativeHuntingAiFindsAndRoutesToAnOpenTrapWithoutAnExplicitNpcClick() =
        game { d ->
            prepare()
            val source = source(d, 0)
            player.withProtectedAccess { d.traps.setup(this, source) }
            advance(4)
            random.next = 0
            repeat(100) { random.then = 0 }
            val npc = spawnNpc(source.coords.translate(3, 0), d.npcs[DeadfallNpcs.all[0]])
            npc.infoProtocol =
                object : NpcInfoProtocol by NoopNpcInfo {
                    override fun isActive() = true
                }
            assertFalse(
                d.hunt.findLocs(npc.coords, 4, HuntVis.LineOfSight).any {
                    it.id == DeadfallLocs.open.id
                }
            )
            assertTrue(
                d.hunt.findLocs(npc.coords, 4, HuntVis.Off).any { it.id == DeadfallLocs.open.id }
            )
            advance(30)
            assertTrue(
                locExists(source.coords, DeadfallLocs.full[0]),
                "npc=${npc.coords},mode=${npc.mode},hunt=${npc.huntMode},range=${npc.huntRange},target=${npc.huntLoc},clock=${npc.huntClock},interaction=${npc.interaction}",
            )
        }

    @Test
    fun GameTestState.pyreFoxCheckFillsBothOpenPouchesWithAFullBagAndConvertedBones() = game { d ->
        prepare()
        val source = source(d, 4)
        player.withProtectedAccess { d.traps.setup(this, source) }
        advance(4)
        catch(d, 4, source.coords)
        val full = bound(d, source.coords, DeadfallLocs.full[4])
        near(full)
        for (i in 0..27) player.inv[i] = InvObj(objs.knife)
        player.inv[0] = InvObj(objs.small_fur_pouch_open)
        player.inv[1] = InvObj(objs.small_meat_pouch_open)
        val before = player.statMap.getFineXP(stats.hunter)
        var commits = 0
        d.events.subscribeUnbound(RemainsDrop::class.java) { convert(1) { commits++ } }
        player.withProtectedAccess { assertTrue(d.traps.check(this, full)) }
        assertEquals(1, d.pouches.count(player, HunterPouchFamily.Fur))
        assertEquals(1, d.pouches.count(player, HunterPouchFamily.Meat))
        assertEquals(0, player.inv.freeSpace())
        assertEquals(1, commits)
        assertEquals(2220, player.statMap.getFineXP(stats.hunter) - before)
    }

    private fun GameTestScope.prepare(level: Int = 99) {
        player.members = true
        player.stats[stats.hunter] = level
        player.inv[0] = InvObj(objs.knife)
        player.inv[1] = InvObj(objs.logs)
    }

    private fun GameTestScope.source(d: Deps, index: Int): BoundLocInfo {
        val c =
            when (index) {
                0 -> CoordGrid(2348, 3524)
                1 -> CoordGrid(2574, 2910)
                2 -> CoordGrid(2308, 3605)
                3 -> CoordGrid(2715, 3769)
                else -> CoordGrid(1612, 2997)
            }
        val angle =
            when (index) {
                0 -> LocAngle.East
                1 -> LocAngle.East
                2 -> LocAngle.South
                3 -> LocAngle.South
                4 -> LocAngle.West
                else -> LocAngle.West
            }
        val source = placeMapLoc(c, DeadfallLocs.boulder, LocShape.CentrepieceStraight, angle)
        near(source)
        return source
    }

    private fun GameTestScope.catch(d: Deps, index: Int, c: CoordGrid) {
        val open = bound(d, c, DeadfallLocs.open)
        near(open)
        val npc = spawnNpc(player.coords, d.npcs[DeadfallNpcs.all[index]])
        random.next = 0
        npc.opLoc1(open, d.ai)
        advance(4)
        assertTrue(locExists(c, DeadfallLocs.full[index]))
    }

    private fun GameTestScope.bound(
        d: Deps,
        c: CoordGrid,
        t: org.rsmod.game.type.loc.LocType,
    ): BoundLocInfo = BoundLocInfo(requireNotNull(d.locs.findExact(c, t)), d.types[t])

    private fun GameTestScope.near(loc: BoundLocInfo) {
        val flags = Rotations.rotate(loc.angleId, loc.forceApproachFlags)
        val c = loc.coords
        player.telejump(
            when {
                flags and 1 == 0 -> c.translate(0, loc.adjustedLength)
                flags and 2 == 0 -> c.translate(loc.adjustedWidth, 0)
                flags and 4 == 0 -> c.translate(0, -1)
                else -> c.translate(-1, 0)
            }
        )
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, DeadfallTrap::class) { d ->
            for (c in DeadfallSpeciesList.sites.keys) for (dx in -8..8 step 8) for (dz in
                -8..8 step 8) allocZoneCollision(c.translate(dx, dz))
            body(d)
        }

    data class Deps
    @Inject
    constructor(
        val traps: DeadfallTrap,
        val pouches: HunterPouchStorage,
        val hunt: Hunt,
        val locs: LocRepository,
        val types: LocTypeList,
        val npcs: NpcTypeList,
        val ai: AiLocInteractions,
        val limits: PortableHunterTraps,
        val events: EventBus,
    )
}
