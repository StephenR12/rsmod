package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ArtefactTheftTest {
    @Test
    fun GameTestState
        .`assignments require membership and visible forty nine with no favour gate`() = test { d ->
        prepare(48)
        player.withProtectedAccess { assertFalse(d.script.assign(this)) }
        player.stats[stats.thieving] = 49
        player.members = false
        player.withProtectedAccess { assertFalse(d.script.assign(this)) }
        player.members = true
        player.statMap.setFineXP(stats.thieving, 0)
        random.next = 5
        player.withProtectedAccess { assertTrue(d.script.assign(this)) }
        assertEquals(6, player.vars[ArtefactBits.target])
        player.withProtectedAccess { assertFalse(d.script.assign(this)) }
        assertEquals(0, player.statMap.getFineXP(stats.thieving))
    }

    @Test
    fun GameTestState.`native drawers require the assigned house a tool and free inventory`() =
        test { d ->
            prepare()
            random.next = 0
            player.withProtectedAccess { assertTrue(d.script.assign(this)) }
            val drawer = drawer(1)
            player.opLoc1(drawer)
            advance(6)
            assertEquals(0, stolen())
            d.script.invalidate(player, false)
            random.next = 1
            player.withProtectedAccess { d.script.assign(this) }
            player.inv[0] = null
            player.opLoc1(drawer)
            advance(6)
            assertEquals(0, stolen())
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.knife)
            player.inv[0] = InvObj(ArtefactObjs.lockpick)
            player.opLoc1(drawer)
            advance(6)
            assertEquals(0, stolen())
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`each of six native drawer actions steals one random artefact only once`() =
        test { d ->
            prepare()
            for (house in 0..5) {
                d.script.invalidate(player, false)
                random.next = house
                player.withProtectedAccess { assertTrue(d.script.assign(this)) }
                val drawer = drawer(house)
                random.next = house % 5
                player.opLoc1(drawer)
                advance(6)
                assertEquals(1, stolen())
                assertEquals(1, player.vars[ArtefactBits.completed])
                player.opLoc1(drawer)
                advance(6)
                assertEquals(1, stolen())
            }
            assertEquals(45000, player.statMap.getFineXP(stats.thieving))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverPiscariliusArtefact))
        }

    @Test
    fun GameTestState
        .`hairclip substitutes for lockpick and delivery pays exact XP coins and diary`() =
        test { d ->
            prepare()
            player.inv[0] = InvObj(ArtefactObjs.hairclip)
            steal(d)
            for (x in listOf(1787, 1807, 1827, 1847)) {
                player.moveGameClick(CoordGrid(x, 3754))
                advance(24)
            }
            assertTrue(player.isWithinDistance(CoordGrid(1847, 3754), 1))
            assertEquals(1, stolen())
            val captain = spawnNpc(CoordGrid(1845, 3752), d.npcs[ArtefactNpcs.captain])
            random.next = 750
            player.opNpc1(captain)
            advance(12)
            assertEquals(750, player.count(objs.coins))
            assertEquals(0, stolen())
            assertEquals(27100, player.statMap.getFineXP(stats.thieving))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DeliverPiscariliusArtefact))
            assertEquals(0, player.vars[ArtefactBits.target])
            player.withProtectedAccess { assertFalse(d.script.deliver(this)) }
        }

    @Test
    fun GameTestState.`inventory filling during lock animation gives no artefact or XP`() =
        test { d ->
            prepare()
            random.next = 0
            player.withProtectedAccess { d.script.assign(this) }
            val drawer = drawer(0)
            player.opLoc1(drawer)
            advance()
            for (slot in 1..27) player.inv[slot] = InvObj(objs.knife)
            advance(6)
            assertEquals(0, stolen())
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
            assertEquals(0, player.vars[ArtefactBits.completed])
        }

    @Test
    fun GameTestState
        .`failed payment retains stolen item and gives no completion or delivery XP`() = test { d ->
        prepare()
        steal(d)
        player.inv[27] = InvObj(objs.coins, Int.MAX_VALUE)
        random.next = 500
        player.withProtectedAccess { assertFalse(d.script.deliver(this)) }
        assertEquals(1, stolen())
        assertEquals(7500, player.statMap.getFineXP(stats.thieving))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverPiscariliusArtefact))
    }

    @Test
    fun GameTestState.`accepted teleport and logout confiscate artefacts without diary credit`() =
        test { d ->
            prepare()
            steal(d)
            d.events.publish(PlayerTeleportEvent.Start(player, CoordGrid(3200, 3200)))
            assertEquals(0, stolen())
            assertEquals(0, player.vars[ArtefactBits.target])
            steal(d)
            d.events.publish(SessionStateEvent.Logout(player))
            assertEquals(0, stolen())
            assertEquals(0, player.vars[ArtefactBits.completed])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverPiscariliusArtefact))
            assertEquals(15000, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState
        .`nonstandard long travel loses artefact but ordinary two tile running retains it`() =
        test { d ->
            prepare()
            steal(d)
            player.telejump(player.coords.translate(2, 0))
            d.script.watch(player)
            assertEquals(1, stolen())
            player.telejump(player.coords.translate(10, 0))
            d.script.watch(player)
            assertEquals(0, stolen())
            player.withProtectedAccess { assertFalse(d.script.deliver(this)) }
        }

    @Test
    fun GameTestState
        .`moving patrol sees only forward cone and confiscates after reaction interval`() =
        test { d ->
            prepare()
            steal(d)
            // Update the expected last tile via the native nearby ladder path; the guard test below
            // uses adjacent coordinates without triggering the independent long-travel detector.
            player.telejump(CoordGrid(1764, 3755))
            // Re-create this heist at the guard location for a deterministic detection fixture.
            d.script.invalidate(player, false)
            random.next = 0
            player.withProtectedAccess { d.script.assign(this) }
            val loc =
                placeMapLoc(
                    CoordGrid(1764, 3754),
                    ArtefactLocs.drawers[0],
                    LocShape.CentrepieceStraight,
                    LocAngle.South,
                )
            random.next = 0
            player.opLoc1(loc)
            advance(6)
            val guard = spawnNpc(CoordGrid(1762, 3755), d.npcs[ArtefactNpcs.guards[0]])
            assertTrue(d.script.sees(guard, player))
            d.script.watch(player)
            advance(3)
            assertEquals(0, stolen())
            assertEquals(ArtefactTheft.CAUGHT, player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverPiscariliusArtefact))
        }

    @Test
    fun GameTestState.`native linked house ladders change plane and permit a foot journey`() =
        test { d ->
            prepare()
            player.telejump(CoordGrid(1775, 3730))
            val bottom =
                placeMapLoc(
                    CoordGrid(1776, 3730),
                    ArtefactLocs.ladder,
                    LocShape.CentrepieceStraight,
                    LocAngle.West,
                )
            player.opLoc1(bottom)
            advance()
            assertEquals(1, player.coords.level)
            val top =
                placeMapLoc(
                    CoordGrid(1776, 3730, 1),
                    ArtefactLocs.upstairs[0],
                    LocShape.CentrepieceStraight,
                    LocAngle.West,
                )
            player.opLoc1(top)
            advance()
            assertEquals(0, player.coords.level)
        }

    @Test
    fun GameTestState.`patrols walk their bridge route and cannot see someone behind them`() =
        test { d ->
            prepare()
            val guard = spawnNpc(CoordGrid(1762, 3755), d.npcs[ArtefactNpcs.guards[0]])
            player.telejump(CoordGrid(1760, 3755))
            assertFalse(d.script.sees(guard, player))
            player.telejump(CoordGrid(1764, 3755))
            assertTrue(d.script.sees(guard, player))
            val initial = guard.coords
            advance(4)
            assertNotEquals(initial, guard.coords)
        }

    private fun GameTestScope.prepare(level: Int = 49) {
        player.members = true
        player.stats[stats.thieving] = level
        player.statMap.setFineXP(stats.thieving, 0)
        player.inv[0] = InvObj(ArtefactObjs.lockpick)
        player.telejump(CoordGrid(1768, 3750))
    }

    private fun GameTestScope.stolen(): Int = ArtefactObjs.loot.sumOf { player.count(it) }

    private fun GameTestScope.drawer(index: Int): org.rsmod.game.loc.BoundLocInfo {
        val c =
            listOf(
                CoordGrid(1767, 3750),
                CoordGrid(1773, 3730, 1),
                CoordGrid(1764, 3735, 1),
                CoordGrid(1749, 3735, 1),
                CoordGrid(1747, 3749, 1),
                CoordGrid(1750, 3763, 1),
            )[index]
        player.telejump(c.translate(0, 1))
        return placeMapLoc(
            c,
            ArtefactLocs.drawers[index],
            LocShape.CentrepieceStraight,
            LocAngle.South,
        )
    }

    private fun GameTestScope.steal(d: Dependencies) {
        random.next = 0
        player.withProtectedAccess { assertTrue(d.script.assign(this)) }
        val loc = drawer(0)
        random.next = 0
        player.opLoc1(loc)
        advance(6)
        assertEquals(1, stolen())
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, ArtefactTheft::class) { d ->
            for (c in
                listOf(
                    CoordGrid(1767, 3750),
                    CoordGrid(1773, 3730, 1),
                    CoordGrid(1764, 3735, 1),
                    CoordGrid(1749, 3735, 1),
                    CoordGrid(1747, 3749, 1),
                    CoordGrid(1750, 3763, 1),
                    CoordGrid(1847, 3755),
                    ArtefactTheft.CAUGHT,
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            for (x in 1760..1856 step 8) for (z in 3744..3760 step 8) allocZoneCollision(
                CoordGrid(x, z)
            )
            body(d)
        }

    data class Dependencies
    @Inject
    constructor(val script: ArtefactTheft, val npcs: NpcTypeList, val events: EventBus)
}
