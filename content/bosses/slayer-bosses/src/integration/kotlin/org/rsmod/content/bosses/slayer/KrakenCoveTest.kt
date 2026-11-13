package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.npc.slayerFamily
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class KrakenCoveTest {
    private class Deps
    @Inject
    constructor(val registry: NpcDropTableRegistry, val random: GameRandom)

    @Test
    fun GameTestState
        .`Kraken Cove entrance and assignment gated whirlpool supply an uncharged trident`() =
        runInjectedGameTest(Deps::class, null, KrakenCove::class) { deps ->
            player.placeAt(CoordGrid(2278, 3610))
            player.members = true
            player.stats[stats.slayer] = 87
            fun loc(id: Int) {
                val t = locTypes.getValue(id)
                val l = placeMapLoc(player.coords.translate(0, 1), t)
                player.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(l, l, t)) }
            }
            loc(30177)
            assertEquals(CoordGrid(2276, 9988), player.coords)
            val whirlpool = spawnNpc(player.coords.translate(2, 0), npcTypes.getValue(493))
            fun disturb() {
                player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op2(whirlpool)) }
            }
            disturb()
            assertEquals(493, whirlpool.visType.id)
            val family = npcTypes.getValue(492).slayerFamily
            assertTrue(family > 0)
            player.setVarp(varps.slayer_target, family)
            player.setVarp(varps.slayer_count, 12)
            player.stats[stats.slayer] = 86
            disturb()
            assertEquals(493, whirlpool.visType.id)
            player.stats[stats.slayer] = 87
            disturb()
            assertEquals(492, whirlpool.visType.id)
            assertEquals(125, whirlpool.hitpoints)
            val table = checkNotNull(deps.registry[whirlpool.type])
            random.next = 0
            random.then = 0
            val drops = table.roll(NpcDropContext(whirlpool, player, whirlpool.coords, deps.random))
            assertTrue(drops.any { it.obj.id == 11908 })
            assertTrue(drops.any { it.obj.id == 12004 })
            loc(537)
            assertEquals(CoordGrid(2280, 10022), player.coords)
            loc(538)
            loc(30178)
            assertEquals(CoordGrid(2278, 3610), player.coords)
        }
}
