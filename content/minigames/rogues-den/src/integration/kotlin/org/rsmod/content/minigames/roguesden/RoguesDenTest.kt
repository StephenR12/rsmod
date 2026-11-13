package org.rsmod.content.minigames.roguesden

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RoguesDenTest {
    private fun GameTestScope.enter(g: RoguesDen) {
        player.members = true
        player.stats[stats.thieving] = 50
        player.stats[stats.agility] = 50
        player.stats[stats.prayer] = 47
        player.setVarp(DenVars.intro, 1)
        player.withProtectedAccess { assertTrue(g.enter(this)) }
    }

    @Test
    fun GameTestState
        .`Brian and doorway enforce both base levels and empty pockets then jewel restores skills`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            player.members = true
            player.stats[stats.agility] = 50
            player.stats[stats.thieving] = 49
            player.stats[stats.prayer] = 41
            val brian = spawnNpc(player.coords.translate(1, 0), npcTypes[DenNpcs.brian])
            player.withProtectedAccess {
                assertFalse(g.enter(this))
                eventBus.publish(this, NpcEvents.Op1(brian))
            }
            player.setCurrentLevel(stats.thieving, 51)
            player.withProtectedAccess { assertFalse(g.enter(this)) }
            player.stats[stats.thieving] = 50
            player.inv[0] = InvObj(objs.coins)
            player.withProtectedAccess { assertFalse(g.enter(this)) }
            player.inv[0] = null
            val outside = CoordGrid(3056, 4991, 1)
            player.placeAt(outside)
            val door = placeMapLoc(outside, DenLocs.entrance)
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op1(door, door, locTypes[DenLocs.entrance]))
            }
            assertEquals(CoordGrid(3056, 4992, 1), player.coords)
            assertEquals(0, player.stats[stats.prayer])
            assertEquals(50, player.stats[stats.thieving])
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(CoordGrid(3056, 4990, 1), player.coords)
            assertNull(player.inv[0])
            assertEquals(41, player.stats[stats.prayer])
        }

    @Test
    fun GameTestState.`disarming or running past a floor trap differs from landing on it`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            enter(g)
            val tile = CoordGrid(3054, 4993, 1)
            val trap = placeMapLoc(tile, DenLocs.traps[0])
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op5(trap, trap, locTypes[DenLocs.traps[0]]))
            }
            assertEquals(49, player.stats[stats.thieving])
            player.placeAt(tile)
            g.pulse(player)
            assertEquals(tile, player.coords)
            val second = tile.translate(-2, 0)
            placeMapLoc(second, DenLocs.traps[0])
            player.placeAt(second.translate(-1, 0))
            g.pulse(player)
            assertTrue(DenObjs.gem in player.inv)
            player.placeAt(second)
            g.pulse(player)
            assertEquals(CoordGrid(3056, 4990, 1), player.coords)
            assertEquals(50, player.stats[stats.thieving])
        }

    @Test
    fun GameTestState
        .`maze safe rolls use visible level reward weights and twelve XP per drained obstacle level`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            for ((roll, coin, drop) in
                listOf(
                    Triple(0, 7, DenObjs.crate),
                    Triple(199, 1, DenObjs.kit),
                    Triple(199, 0, null),
                )) {
                enter(g)
                val bars =
                    placeMapLoc(CoordGrid(3049, 4997, 1), DenLocs.bars, angle = LocAngle.North)
                player.placeAt(CoordGrid(3050, 4997, 1))
                player.withProtectedAccess {
                    eventBus.publish(this, LocEvents.Op1(bars, bars, locTypes[DenLocs.bars]))
                }
                assertEquals(45, player.stats[stats.agility])
                val before = player.statMap.getXP(stats.agility)
                val safe = placeMapLoc(CoordGrid(3018, 5047, 1), DenLocs.safe)
                player.placeAt(safe.coords.translate(1, 0))
                random.next = roll
                random.then = coin
                if (roll == 199 && coin == 1) random.then = 0
                player.withProtectedAccess { assertEquals(drop != null, g.safe(this, safe)) }
                if (drop != null) {
                    assertTrue(drop in player.inv)
                    assertEquals(before + 60, player.statMap.getXP(stats.agility))
                } else assertFalse(DenObjs.crate in player.inv || DenObjs.kit in player.inv)
                assertEquals(50, player.stats[stats.agility])
                assertEquals(47, player.stats[stats.prayer])
                for (i in player.inv.indices) player.inv[i] = null
            }
        }

    @Test
    fun GameTestState.`maze item removal on logout never removes other inventory`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            enter(g)
            player.inv[1] = InvObj(DenObjs.powder, 4)
            player.inv[2] = InvObj(DenObjs.tiles[3])
            player.inv[3] = InvObj(objs.rope)
            eventBus.publish(SessionStateEvent.Logout(player))
            assertEquals(CoordGrid(3056, 4990, 1), player.coords)
            assertTrue(objs.rope in player.inv)
            assertFalse(
                DenObjs.powder in player.inv ||
                    DenObjs.gem in player.inv ||
                    DenObjs.tiles[3] in player.inv
            )
        }

    @Test
    fun GameTestState
        .`mosaic accepts only the bottom-right design and rogue crate pays the selected piece`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            enter(g)
            val tile = CoordGrid(3023, 5082, 1)
            val door = placeMapLoc(tile, DenLocs.mosaic, angle = LocAngle.East)
            player.placeAt(tile)
            player.inv[1] = InvObj(DenObjs.tiles[0])
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op1(door, door, locTypes[DenLocs.mosaic]))
            }
            player.ifButton(DenComponents.tiles[2])
            advance()
            assertTrue(DenObjs.tiles[0] in player.inv)
            player.ifButton(DenComponents.tiles[0])
            advance()
            assertFalse(DenObjs.tiles[0] in player.inv)
            assertEquals(tile.translate(1, 0), player.coords)
            g.finish(player, false)
            player.inv[0] = InvObj(DenObjs.crate)
            player.withProtectedAccess { opHeld1(0) }
            player.resumeActiveCoroutine(
                org.rsmod.api.player.input.ResumePauseButtonInput(
                    org.rsmod.api.config.refs.components.menu_list,
                    4,
                )
            )
            assertTrue(DenObjs.prizes[4] in player.inv)
            assertFalse(DenObjs.crate in player.inv)
        }

    @Test
    fun GameTestState.`powder can be thrown from outside the guards ejection radius`() =
        runInjectedGameTest(RoguesDen::class, null, RoguesDen::class) { g ->
            enter(g)
            val guard = spawnNpc(CoordGrid(3018, 5072, 1), npcTypes[DenNpcs.guards[0]])
            player.placeAt(guard.coords.translate(-4, 0))
            player.inv[1] = InvObj(DenObjs.powder)
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    org.rsmod.api.player.events.interact.NpcUEvents.Ap(
                        guard,
                        1,
                        objTypes[DenObjs.powder],
                        guard.type,
                    ),
                )
            }
            assertFalse(DenObjs.powder in player.inv)
            player.placeAt(guard.coords.translate(-1, 0))
            g.pulse(player)
            assertTrue(DenObjs.gem in player.inv)
            advance(11)
            assertFalse(DenObjs.gem in player.inv)
        }
}
