package org.rsmod.content.skills.hunter.tracking

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.util.Rotations

@Execution(ExecutionMode.SAME_THREAD)
class KebbitTrackingTest {
    @Test
    fun GameTestState.polarNativeTrailSuppliesFurMeatBonesAndThirtyXp() = catchSpecies("polar")

    @Test
    fun GameTestState.commonNativeTrailSuppliesWoodlandFurAndThirtySixXp() = catchSpecies("common")

    @Test
    fun GameTestState.feldipNativeTrailSuppliesJungleFurAndFortyEightXp() = catchSpecies("jungle")

    @Test
    fun GameTestState.desertNativeTrailSuppliesDesertFurAndSixtySixXp() = catchSpecies("desert")

    @Test
    fun GameTestState.razorNativeTrailSuppliesLongSpikeAnd348Point5Xp() = catchSpecies("razor")

    private fun GameTestState.catchSpecies(name: String) = game { d ->
        prepare()
        val network = TrackingNetworks.all.first { it.name == name }
        val before = player.statMap.getFineXP(stats.hunter)
        val start = place(network.starts.first())
        near(start)
        player.opLoc1(start)
        advance()
        assertNotNull(d.tracking.trail(player))
        finishTrail(d)
        val trail = requireNotNull(d.tracking.trail(player))
        assertTrue(trail.steps.size >= 3)
        assertEquals(trail.steps.size, trail.steps.map { it.edge }.distinct().size)
        val dest = place(trail.destination)
        near(dest)
        player.opLoc1(dest)
        advance()
        assertNotNull(d.tracking.trail(player))
        random.next = 0
        player.opLoc2(dest)
        advance(2)
        assertNull(d.tracking.trail(player))
        assertEquals(1, player.count(network.reward))
        assertEquals(1, player.count(objs.raw_beast_meat))
        assertEquals(1, player.count(objs.bones))
        assertEquals((network.xp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - before)
        assertTrue(TrackingBits.all.values.all { player.vars[it] == 0 })
    }

    @Test
    fun GameTestState.trackingRespectsMembershipAndLevelsAndIgnoresUnmappedBurrows() = game { d ->
        prepare()
        val site = TrackingNetworks.all.first { it.name == "razor" }.starts.first()
        val start = place(site)
        near(start)
        player.stats[stats.hunter] = 48
        player.withProtectedAccess { assertFalse(d.tracking.start(this, start)) }
        assertNull(d.tracking.trail(player))
        player.stats[stats.hunter] = 99
        player.members = false
        player.withProtectedAccess { assertFalse(d.tracking.start(this, start)) }
        player.members = true
        val fake =
            placeMapLoc(
                CoordGrid(3200, 3200),
                site.type,
                LocShape[site.shape],
                LocAngle[site.angle],
            )
        near(fake)
        player.withProtectedAccess { assertFalse(d.tracking.start(this, fake)) }
        assertNull(d.tracking.trail(player))
    }

    @Test
    fun GameTestState.wrongClueAndPrematureAttackCannotSkipTrailAndMissingWandScaresCreature() =
        game { d ->
            prepare()
            start(d, "common")
            val trail = requireNotNull(d.tracking.trail(player))
            val wrong = trail.network.edges.first { it !== trail.steps[1].edge }.clues.first()
            val clue = place(wrong)
            near(clue)
            player.withProtectedAccess { assertFalse(d.tracking.inspect(this, clue)) }
            assertEquals(1, trail.revealed)
            val dest = place(trail.destination)
            near(dest)
            player.withProtectedAccess { assertFalse(d.tracking.catch(this, dest)) }
            assertNotNull(d.tracking.trail(player))
            finishTrail(d)
            near(dest)
            player.righthand = null
            player.withProtectedAccess { assertFalse(d.tracking.catch(this, dest)) }
            assertNull(d.tracking.trail(player))
            assertEquals(0, player.count(trail.network.reward))
        }

    @Test
    fun GameTestState.fullInventoryPreservesTrailAndNeverCommitsConvertedBonesUntilLootDelivered() =
        game { d ->
            prepare()
            start(d, "razor")
            finishTrail(d)
            val trail = requireNotNull(d.tracking.trail(player))
            val dest = place(trail.destination)
            near(dest)
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            var commits = 0
            d.events.subscribeUnbound(RemainsDrop::class.java) { convert(1) { commits++ } }
            val before = player.statMap.getFineXP(stats.hunter)
            random.next = 0
            player.withProtectedAccess { d.tracking.catch(this, dest) }
            advance()
            assertEquals(0, commits)
            assertNotNull(d.tracking.trail(player))
            assertEquals(before, player.statMap.getFineXP(stats.hunter))
            player.inv[0] = null
            player.inv[1] = null
            random.next = 0
            player.withProtectedAccess { d.tracking.catch(this, dest) }
            advance()
            assertEquals(1, commits)
            assertNull(d.tracking.trail(player))
            assertEquals(1, player.count(trail.network.reward))
            assertEquals(0, player.count(objs.bones))
        }

    @Test
    fun GameTestState.pursuitRevealsEverySegmentAndSpendsOnlyAnEquippedChargeThenDisappears() =
        game { d ->
            prepare()
            player.inv[0] = InvObj(TrackingObjs.pursuit)
            start(d, "common")
            assertEquals(1, d.tracking.trail(player)?.revealed)
            player.ring = InvObj(TrackingObjs.pursuit)
            start(d, "common")
            val trail = requireNotNull(d.tracking.trail(player))
            assertEquals(trail.steps.size, trail.revealed)
            assertEquals(9, d.tracking.ringCharges(requireNotNull(player.ring)))
            player.ring = InvObj(TrackingObjs.pursuit, vars = 1)
            start(d, "common")
            assertNull(player.ring)
            assertEquals(TrackingObjs.pursuit.id, player.inv[0]?.id)
            assertEquals("Check", d.objects[TrackingObjs.pursuit].iop[2])
            assertEquals("Break", d.objects[TrackingObjs.pursuit].iop[3])
        }

    @Test
    fun GameTestState.separatePlayersHaveIndependentVisibleTrailsAndLogoutClearsOnlyOne() =
        game { d ->
            prepare()
            start(d, "common")
            val first = requireNotNull(d.tracking.trail(player))
            val second = registerPlayer()
            second.members = true
            second.stats[stats.hunter] = 99
            val loc = place(first.network.starts.first())
            second.telejump(player.coords)
            second.withProtectedAccess { assertTrue(d.tracking.start(this, loc)) }
            val other = requireNotNull(d.tracking.trail(second))
            d.tracking.clear(player)
            assertNull(d.tracking.trail(player))
            assertNotNull(d.tracking.trail(second))
            assertTrue(other.steps.take(other.revealed).all { second.vars[it.edge.bit] in 3..4 })
        }

    @Test
    fun GameTestState.walkingInterruptsCatchBeforeXpAndLoot() = game { d ->
        prepare()
        start(d, "common")
        finishTrail(d)
        val trail = requireNotNull(d.tracking.trail(player))
        val dest = place(trail.destination)
        near(dest)
        val before = player.statMap.getFineXP(stats.hunter)
        player.withProtectedAccess { d.tracking.catch(this, dest) }
        player.moveGameClick(player.coords.translate(1, 0))
        advance(2)
        assertNotNull(d.tracking.trail(player))
        assertEquals(0, player.count(trail.network.reward))
        assertEquals(before, player.statMap.getFineXP(stats.hunter))
    }

    @Test
    fun GameTestState.polarNativeStepsLandOnUpperHuntingFloorAndReturnToWalkableLowerApproach() =
        game { d ->
            prepare()
            val up =
                placeMapLoc(
                    CoordGrid(2715, 3799),
                    TrackingLocs.polarUp,
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.telejump(CoordGrid(2715, 3798))
            player.opLoc1(up)
            advance()
            assertEquals(CoordGrid(2715, 3802, 1), player.coords)
            val down =
                placeMapLoc(
                    CoordGrid(2715, 3800, 1),
                    TrackingLocs.polarDown,
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.opLoc1(down)
            advance()
            assertEquals(CoordGrid(2715, 3798), player.coords)
        }

    @Test
    fun GameTestState.freshLevelOnePolarTrackingCanFailWithoutLootOrXp() = game { d ->
        prepare()
        player.stats[stats.hunter] = 1
        start(d, "polar")
        finishTrail(d)
        val trail = requireNotNull(d.tracking.trail(player))
        val dest = place(trail.destination)
        near(dest)
        val before = player.statMap.getFineXP(stats.hunter)
        random.next = 99
        player.opLoc2(dest)
        advance(2)
        assertNull(d.tracking.trail(player))
        assertEquals(before, player.statMap.getFineXP(stats.hunter))
        assertEquals(0, player.count(trail.network.reward))
        assertTrue(TrackingBits.all.values.all { player.vars[it] == 0 })
    }

    @Test
    fun GameTestState.everyNativeBurrowCanBeginANewConnectedTrail() = game { d ->
        prepare()
        for (network in TrackingNetworks.all) for (site in network.starts) {
            val loc = place(site)
            near(loc)
            random.next = 0
            player.opLoc1(loc)
            advance()
            val trail = requireNotNull(d.tracking.trail(player))
            assertSame(network, trail.network)
            assertEquals(site.node, trail.steps.first().from)
            assertEquals(1, trail.revealed)
            assertTrue(trail.steps.size >= 3)
        }
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.hunter] = 99
        player.righthand = InvObj(TrackingObjs.wand)
        random.next = 0
    }

    private fun GameTestScope.start(d: Deps, name: String) {
        val site = TrackingNetworks.all.first { it.name == name }.starts.first()
        val loc = place(site)
        near(loc)
        random.next = 0
        player.withProtectedAccess { assertTrue(d.tracking.start(this, loc)) }
    }

    private fun GameTestScope.finishTrail(d: Deps) {
        val trail = requireNotNull(d.tracking.trail(player))
        while (trail.revealed < trail.steps.size) {
            val before = trail.revealed
            val loc = place(trail.steps[trail.revealed].edge.clues.first())
            near(loc)
            player.opLoc1(loc)
            advance()
            assertTrue(trail.revealed > before, "clue did not reveal next segment")
        }
    }

    private fun GameTestScope.place(site: TrackingSite): BoundLocInfo =
        placeMapLoc(site.coords, site.type, LocShape[site.shape], LocAngle[site.angle])

    private fun GameTestScope.near(loc: BoundLocInfo) {
        val flags = Rotations.rotate(loc.angleId, loc.forceApproachFlags)
        player.telejump(
            when {
                flags and 1 == 0 -> loc.coords.translate(0, loc.adjustedLength)
                flags and 2 == 0 -> loc.coords.translate(loc.adjustedWidth, 0)
                flags and 4 == 0 -> loc.coords.translate(0, -1)
                else -> loc.coords.translate(-1, 0)
            }
        )
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, KebbitTracking::class) { d ->
            for (network in TrackingNetworks.all) for (site in
                network.starts + network.ends + network.edges.flatMap { it.clues }) for (dx in
                -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(
                site.coords.translate(dx, dz)
            )
            for (level in 0..1) for (x in 2704..2736 step 8) for (z in
                3792..3832 step 8) allocZoneCollision(CoordGrid(x, z, level))
            body(d)
        }

    data class Deps
    @Inject
    constructor(val tracking: KebbitTracking, val events: EventBus, val objects: ObjTypeList)
}
