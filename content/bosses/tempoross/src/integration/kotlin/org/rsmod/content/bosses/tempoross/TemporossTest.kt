package org.rsmod.content.bosses.tempoross

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
class TemporossTest {
    private fun GameTestScope.ropeLadder(solo: Boolean) {
        val pos = CoordGrid(3135, 2840)
        player.teleport(pos.translate(2, 0))
        val ladder = placeMapLoc(pos, locTypes.getValue(41305))
        if (solo) player.opLoc3(ladder) else player.opLoc2(ladder)
        advance(3)
    }

    private fun GameTestState.test(body: GameTestScope.(NpcRepository) -> Unit) =
        runInjectedGameTest(
            NpcRepository::class,
            null,
            TemporossScript::class,
            TemporossRewards::class,
        ) {
            player.members = true
            player.stats[stats.fishing] = 99
            player.observerUUID = 123L
            body(it)
        }

    @Test
    fun GameTestState.boardingRequiresBaseFishingAndTheStormReturnsAnIdleCrew() = test {
        player.stats[stats.fishing] = 34
        ropeLadder(true)
        assertEquals(0, player.vars[cacheTypes.varps.getValue(65741)])
        player.stats[stats.fishing] = 35
        ropeLadder(false)
        assertEquals(CoordGrid(3134, 2840), player.coords)
        advance(101)
        assertEquals(2, player.vars[cacheTypes.varps.getValue(65741)])
        assertTrue(
            player.ui.containsOverlay(
                cacheTypes.interfaces.values.single { it.internalName == "tempoross_hud" }
            )
        )
        advance(385)
        assertEquals(CoordGrid(3137, 2840), player.coords)
        assertEquals(0, player.vars[cacheTypes.varps.getValue(65741)])
        assertEquals(0, player.vars[cacheTypes.varbits.getValue(11936)])
    }

    @Test
    fun GameTestState.harpoonShrineCannonSpiritAndPermitPoolWorkInSequence() = test { npcs ->
        val shrineType = locTypes.getValue(41236)
        placeMapLoc(
            CoordGrid(3039, 3001),
            shrineType,
            shape = org.rsmod.game.loc.LocShape[11],
            angle = org.rsmod.game.loc.LocAngle.South,
        )
        ropeLadder(true)
        val deck = player.coords
        val residents = npcs.findAll(ZoneKey.from(deck), 8).toList()
        val fish = residents.first { it.id == 10565 }
        player.teleport(fish.coords.translate(0, -1))
        player.opNpc1(fish)
        advance(6)
        assertEquals(0, player.count(objTypes.getValue(25564)))
        player.inv[0] = InvObj(objs.harpoon)
        player.opNpc1(fish)
        advance(16)
        assertTrue(player.count(objTypes.getValue(25564)) > 0, "harpoon a native fishing spot")
        player.moveGameClick(player.coords)
        advance()
        val shrine = deck.translate(4, 24)
        player.teleport(shrine.translate(2, 0))
        player.opLoc1(
            org.rsmod.game.loc.BoundLocInfo(requireNotNull(findLoc(shrine, shrineType)), shrineType)
        )
        advance(10)
        assertTrue(player.count(objTypes.getValue(25565)) > 0, "cook the caught fish")
        player.moveGameClick(player.coords)
        advance()
        // Supply the rest of a crew's catch, then exercise loading, firing and both native pool
        // states.
        val pool = residents.first { it.id == 10570 }
        var rounds = 0
        while (player.vars[cacheTypes.varps.getValue(2934)] == 0 && rounds++ < 4) {
            val cannon = residents.first { it.id == 10576 + (rounds - 1) % 4 }
            for (slot in 1..24) player.inv[slot] = InvObj(objTypes.getValue(25565))
            player.teleport(cannon.coords.translate(-1, 0))
            player.opNpc1(cannon)
            repeat(120) {
                if (pool.transmog?.id != 10571) {
                    // Waves interrupt loading. Return to the crate and resume with the surviving
                    // fish.
                    if (it % 12 == 0) {
                        if (player.count(objTypes.getValue(25565)) == 0)
                            for (slot in 1..24) player.inv[slot] = InvObj(objTypes.getValue(25565))
                        player.teleport(cannon.coords.translate(if (rounds <= 2) -1 else 1, 0))
                        player.opNpc1(cannon)
                    }
                    advance()
                }
            }
            assertEquals(
                10571,
                pool.transmog?.id,
                "cannon should have broken the shield at round $rounds; points=${player.vars[cacheTypes.varbits.getValue(11897)]} cooked=${player.count(objTypes.getValue(25565))}",
            )
            player.moveGameClick(player.coords)
            advance(3)
            player.teleport(pool.coords.translate(0, 3))
            player.opNpc1(pool)
            val before = player.vars[cacheTypes.varbits.getValue(11897)]
            repeat(75) {
                if (pool.transmog?.id == 10571 && player.vars[cacheTypes.varps.getValue(2934)] == 0)
                    advance()
            }
            assertTrue(
                player.vars[cacheTypes.varbits.getValue(11897)] > before,
                "harpooning the vulnerable pool should score points",
            )
        }
        assertEquals(1, player.vars[cacheTypes.varps.getValue(2934)])
        assertTrue(player.vars[cacheTypes.varbits.getValue(11936)] > 0)
        advance(34)
        assertEquals(CoordGrid(3137, 2840), player.coords)
        assertEquals(0, player.count(objTypes.getValue(25565)))
        val permits = player.vars[cacheTypes.varbits.getValue(11936)]
        val pos = CoordGrid(3150, 2832)
        val reward = placeMapLoc(pos, locTypes.getValue(41356))
        player.inv[1] = InvObj(objs.small_fishing_net)
        player.teleport(pos.translate(-1, 0))
        player.opLoc3(reward)
        advance(7)
        assertEquals(minOf(5, permits), player.vars[cacheTypes.varps.getValue(65743)])
        assertEquals(
            (permits - 5).coerceAtLeast(0),
            player.vars[cacheTypes.varbits.getValue(11936)],
        )
    }

    @Test
    fun GameTestState.bigSearchUsesBaseFishingAndReplacesOwnedBarrels() = test {
        val pos = CoordGrid(3150, 2832)
        player.teleport(pos.translate(-1, 0))
        val pool = placeMapLoc(pos, locTypes.getValue(41356))
        player.stats[stats.fishing] = 50
        player.setCurrentLevel(stats.fishing, 99)
        player.setVarBit(cacheTypes.varbits.getValue(11936), 3)
        player.opLoc3(pool)
        advance(5)
        assertEquals(3, player.vars[cacheTypes.varbits.getValue(11936)])
        player.inv[0] = InvObj(objs.small_fishing_net)
        player.inv[1] = InvObj(objs.fish_barrel_open)
        random.next = 40
        random.then = 6000
        random.then = 39
        random.then = 8
        random.then = 2300
        player.opLoc3(pool)
        advance(7)
        assertEquals(25, player.count(objTypes.getValue(25578)))
        assertEquals(8, player.count(objTypes.cert(objTypes[objs.raw_swordfish])))
        assertEquals(1, player.count(objTypes.getValue(25590)))
        assertEquals(0, player.vars[cacheTypes.varbits.getValue(11936)])
        assertEquals(3, player.vars[cacheTypes.varps.getValue(65743)])
        random.next = 0
        random.then = 1
        random.then = 99
        val casket = player.inv.indexOfFirst { it?.id == 25590 }
        player.withProtectedAccess { opHeld1(casket) }
        assertEquals(0, player.count(objTypes.getValue(25590)))
        assertEquals(1, player.count(objTypes.cert(objTypes.getValue(1147))))
    }
}
