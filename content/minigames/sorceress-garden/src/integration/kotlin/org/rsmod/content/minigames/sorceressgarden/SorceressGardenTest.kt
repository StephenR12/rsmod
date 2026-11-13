package org.rsmod.content.minigames.sorceressgarden

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
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SorceressGardenTest {
    private fun GameTestScope.click(id: Int) {
        val type = locTypes.getValue(id)
        val loc = placeMapLoc(player.coords.translate(1, 0), type)
        player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
    }

    @Test
    fun GameTestState
        .`spymaster introduction and apprentice dialogue unlock the native teleport and fountain exit`() =
        runGameTest(SorceressGarden::class) {
            player.members = true
            val street = CoordGrid(3321, 3139)
            player.placeAt(street)
            val apprentice = spawnNpc(street.translate(1, 0), npcTypes.getValue(1808))
            val osman = spawnNpc(street.translate(-1, 0), npcTypes.getValue(4286))
            fun talk() {
                player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(osman)) }
            }
            talk()
            assertEquals(0, player.vars[GardenBits.introduction])
            player.setVarp(GardenVars.prince, 110)
            talk()
            assertEquals(1, player.vars[GardenBits.introduction])
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op4(apprentice)) }
            assertEquals(street, player.coords)
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(apprentice)) }
            assertEquals(CoordGrid(2912, 5474), player.coords)
            click(GardenLocs.fountain.id)
            advance(3)
            assertEquals(CoordGrid(3321, 3141), player.coords)
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op4(apprentice)) }
            assertEquals(CoordGrid(2912, 5474), player.coords)
        }

    @Test
    fun GameTestState
        .`four native gates lead to fruit and juice rewards with distinct skill requirements and exchange values`() =
        runInjectedGameTest(SorceressGarden::class, null, SorceressGarden::class) { game ->
            player.members = true
            player.setVarp(GardenVars.prince, 110)
            player.setVarBit(GardenBits.introduction, 1)
            player.setVarBit(GardenBits.apprentice, 1)
            for (g in Garden.entries) {
                val gate = GardenLocs.gates[g.ordinal].id
                player.placeAt(g.gate)
                player.stats[stats.thieving] = (g.level - 1).coerceAtLeast(0)
                click(gate)
                assertEquals(g.gate, player.coords)
                player.stats[stats.thieving] = g.level
                val farm = player.statMap.getXP(stats.farming)
                repeat(g.fruitCount) {
                    player.placeAt(g.gate)
                    click(gate)
                    assertEquals(g.inside, player.coords)
                    click(GardenLocs.trees[g.ordinal].id)
                    assertEquals(g.eject, player.coords)
                }
                assertEquals(
                    farm + (30 + 10 * g.ordinal) * g.fruitCount,
                    player.statMap.getXP(stats.farming),
                )
                player.inv[25] = InvObj(objs.pestle_and_mortar)
                player.withProtectedAccess { assertFalse(game.juice(this, g)) }
                player.inv[26] = InvObj(objs.beer_glass)
                val cooking = player.statMap.getXP(stats.cooking)
                player.withProtectedAccess { assertTrue(game.juice(this, g)) }
                assertEquals(cooking + 5, player.statMap.getXP(stats.cooking))
                val xp = player.statMap.getXP(stats.thieving)
                player.withProtectedAccess {
                    game.contact(this, true)
                    assertEquals(xp, player.statMap.getXP(stats.thieving))
                    game.contact(this, false)
                }
                assertEquals(xp + g.juiceXp, player.statMap.getXP(stats.thieving))
            }
        }

    class World
    @jakarta.inject.Inject
    constructor(
        val garden: SorceressGarden,
        val registry: org.rsmod.api.registry.player.PlayerRegistry,
    )

    @Test
    fun GameTestState
        .`herbs require two inventory spaces and elemental sight respects direction and distance`() =
        runInjectedGameTest(World::class) { d ->
            val game = d.garden
            player.members = true
            player.setVarBit(GardenBits.apprentice, 1)
            val g = Garden.Winter
            player.placeAt(g.inside)
            for (i in 1..27) player.inv[i] = InvObj(objs.hammer)
            player.withProtectedAccess { assertFalse(game.harvest(this, g, true)) }
            assertEquals(g.inside, player.coords)
            assertEquals(0, player.statMap.getXP(stats.farming))
            player.inv[1] = null
            player.withProtectedAccess { assertTrue(game.harvest(this, g, true)) }
            assertEquals(50, player.statMap.getXP(stats.farming))
            assertTrue(
                (0..1).all { i -> GardenObjs.herbs.take(4).any { it.id == player.inv[i]?.id } }
            )
            val guard = spawnNpc(CoordGrid(2899, 5466), npcTypes.getValue(5796))
            player.placeAt(guard.coords.translate(0, 2))
            assertTrue(game.sees(guard, player, 0, 1))
            assertFalse(game.sees(guard, player, 0, -1))
            player.placeAt(guard.coords.translate(0, 3))
            assertFalse(game.sees(guard, player, 0, 1))
            player.placeAt(guard.coords.translate(0, 1))
            d.registry.change(
                player,
                org.rsmod.map.zone.ZoneKey.NULL,
                org.rsmod.map.zone.ZoneKey.from(player.coords),
            )
            game.patrol(guard, 0)
            assertEquals(g.eject, player.coords)
        }
}
