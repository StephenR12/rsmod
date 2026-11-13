package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CamdozaalGolemsTest {
    private class Deps
    @Inject
    constructor(
        val ground: ObjRepository,
        val tables: NpcDropTableRegistry,
        val rng: GameRandom,
        @org.rsmod.api.random.CoreRandom val deathRandom: GameRandom,
    )

    @Test
    fun GameTestState
        .`level thirteen flawed rubble awakens after Below Ice Mountain and only its owner can damage it`() =
        runInjectedGameTest(
            Deps::class,
            null,
            CamdozaalGolems::class,
            NpcHitScript::class,
            NpcDeathScript::class,
        ) { d ->
            val tile = CoordGrid(2976, 5775)
            val first = spawnNpc(tile, npcTypes.getValue(10696)) { respawns = true }
            val second = spawnNpc(tile.translate(4, 0), npcTypes.getValue(10694))
            player.placeAt(tile.translate(-1, 0))
            player.members = false
            player.observerUUID = player.uuid
            player.stats[stats.hitpoints] = 99
            fun wake(n: org.rsmod.game.entity.Npc) {
                player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(n)) }
            }
            player.setVarBit(cacheTypes.varbits.getValue(12063), 119)
            wake(first)
            assertEquals(10696, first.visType.id)
            player.setVarBit(cacheTypes.varbits.getValue(12063), 120)
            wake(first)
            assertEquals(10695, first.visType.id)
            assertEquals(25, first.hitpoints)
            wake(second)
            assertEquals(10694, second.visType.id)
            val other = registerPlayer(tile.translate(-2, 0))
            first.queueHit(other, 1, HitType.Melee, 12)
            advance(2)
            assertEquals(25, first.hitpoints)
            (d.deathRandom as org.rsmod.api.testing.random.FixedRandom).set(1)
            first.queueHit(player, 1, HitType.Melee, 50)
            advance(8)
            assertTrue(
                d.ground.findAll(tile).any { it.type in setOf(434, 1437, 438, 436, 1623, 1621) }
            )
            wake(second)
            assertEquals(10693, second.visType.id)
            player.placeAt(tile.translate(-20, 0))
            advance(4)
            assertFalse(second.isVisible)
            assertTrue(d.ground.findAll(second.coords).none())
            advance(26)
            assertEquals(10694, second.visType.id)
        }

    @Test
    fun GameTestState
        .`mind body and chaos golems roll distinct rune cores and both charms improve rare chances`() =
        runInjectedGameTest(Deps::class, null, CamdozaalGolems::class) { d ->
            player.members = true
            player.setVarBit(cacheTypes.varbits.getValue(12063), 120)
            for ((i, id) in listOf(10693, 10691, 10689).withIndex()) {
                val n = spawnNpc(CoordGrid(3000 + i * 3, 5774), npcTypes.getValue(id))
                val table = d.tables[n.type]!!
                val ctx = NpcDropContext(n, player, n.coords, d.rng)
                fun rolls(luck: Int, timed: Int): Set<Int> {
                    player.setVarBit(cacheTypes.varbits.getValue(12103), luck)
                    player.setVarBit(cacheTypes.varbits.getValue(12104), timed)
                    random.next = 51
                    random.then = 12
                    random.then = 205
                    random.then = 108
                    random.then = 3000
                    return table.roll(ctx).map { it.obj.id }.toSet()
                }
                val ordinary = rolls(0, 0)
                assertFalse(25696 + 2 * i in ordinary)
                assertFalse(25639 in ordinary)
                val permanent = rolls(1, 0)
                assertTrue(25696 + 2 * i in permanent)
                assertFalse(25639 in permanent)
                assertTrue(25639 in rolls(1, 1))
            }
            val chaos = spawnNpc(CoordGrid(3014, 5777), npcTypes.getValue(10690))
            player.members = false
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(chaos)) }
            assertEquals(10690, chaos.visType.id)
            player.members = true
            player.withProtectedAccess { eventBus.publish(this, NpcEvents.Op1(chaos)) }
            assertEquals(10689, chaos.visType.id)
        }
}
