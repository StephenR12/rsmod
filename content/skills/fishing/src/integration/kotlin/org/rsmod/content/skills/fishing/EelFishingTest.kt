package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class EelFishingTest {
    @Test
    fun GameTestState.`cache 233 eel spots methods and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(1497, 1498, 1499, 1500, 10653),
                CaveEelFishingNpcs.all.map { it.id },
            )
            for (spot in CaveEelFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertTrue(type.op[0] == "Net" || type.op[0] == "Small Net", "spot=${spot.id}")
                assertEquals(null, type.op[1], "spot=${spot.id}")
                assertEquals("Bait", type.op[2], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertEquals(0, type.wanderRange, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op3::class.java, spot.id))
            }

            assertEquals(listOf(2653, 2654, 2655), SwampEelFishingNpcs.all.map { it.id })
            for (spot in SwampEelFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertEquals("Bait", type.op[0], "spot=${spot.id}")
                assertEquals(
                    "I can see 'things' swimming in the water.",
                    type.desc,
                    "spot=${spot.id}",
                )
                assertEquals(0, type.wanderRange, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }

            assertEquals(3379, objs.raw_slimy_eel.id)
            assertEquals(5001, objs.raw_cave_eel.id)
            assertEquals(307, objs.fishing_rod.id)
            assertEquals(313, objs.fishing_bait.id)
            assertEquals(22846, objs.pearl_fishing_rod.id)
            assertEquals(622, seqs.human_fishing_casting.id)
            assertEquals(8188, seqs.human_fishing_casting_pearl.id)
            assertFalse(deps.objs[objs.raw_slimy_eel].stackable)
            assertFalse(deps.objs[objs.raw_cave_eel].stackable)
            assertNotNull(deps.seqs[seqs.human_fishing_casting])
            assertNotNull(deps.seqs[seqs.human_fishing_casting_pearl])

            assertEquals(
                listOf(
                    CatchContract(5001, 38, 80.0, 10, 80, "You catch a raw cave eel."),
                    CatchContract(3379, 28, 80.0, 10, 80, "You catch a raw slimy eel."),
                ),
                StandardFishingMethods.caveEels.catches.map(::contract),
            )
            assertEquals(
                listOf(CatchContract(3379, 28, 65.0, 10, 80, "You catch a raw slimy eel.")),
                StandardFishingMethods.swampSlimyEel.catches.map(::contract),
            )
            for (method in
                listOf(StandardFishingMethods.caveEels, StandardFishingMethods.swampSlimyEel)) {
                assertEquals(listOf(307, 22846), method.tools.map { it.type.id })
                assertEquals(listOf(false, true), method.tools.map { it.wearable })
                assertEquals(objs.fishing_bait, method.bait)
                assertTrue(method.membersOnly)
            }
        }

    @Test
    fun GameTestState.`cave eel roll precedes the slimy eel fallback`() = runFishingTest { deps ->
        prepare(level = 38)
        add(objs.fishing_rod)
        add(objs.fishing_bait, 2)
        val npc = spawnCaveSpot(deps)

        random.next = 0
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.caveEels) }
        advance(5)
        assertEquals(1, player.count(objs.raw_cave_eel))
        assertEquals(0, player.count(objs.raw_slimy_eel))
        assertEquals(800, player.statMap.getFineXP(stats.fishing))
        assertMessageSent("You catch a raw cave eel.")

        prepare(level = 38)
        add(objs.fishing_rod)
        add(objs.fishing_bait, 2)
        random.next = 100
        random.then = 0
        player.withProtectedAccess { deps.fishing.fish(this, npc, StandardFishingMethods.caveEels) }
        advance(5)
        assertEquals(0, player.count(objs.raw_cave_eel))
        assertEquals(1, player.count(objs.raw_slimy_eel))
        assertEquals(800, player.statMap.getFineXP(stats.fishing))
        assertMessageSent("You catch a raw slimy eel.")
    }

    @Test
    fun GameTestState.`level twenty eight cave fishing selects slimy eel and consumes one bait`() =
        runFishingTest { deps ->
            prepare(level = 28)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 0
            val npc = spawnCaveSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertEquals(seqs.human_fishing_casting.id, player.pendingSequence.id)
            advance(4)
            assertEquals(0, player.count(objs.raw_slimy_eel))
            advance()

            assertEquals(1, player.count(objs.raw_slimy_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(800, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Mort Myre slimy eel uses its lower xp award`() = runFishingTest { deps ->
        prepare(level = 28)
        add(objs.fishing_rod)
        add(objs.fishing_bait)
        random.next = 0
        val npc = spawnSwampSpot(deps)

        player.withProtectedAccess {
            deps.fishing.fish(this, npc, StandardFishingMethods.swampSlimyEel)
        }
        advance(5)

        assertEquals(1, player.count(objs.raw_slimy_eel))
        assertEquals(0, player.count(objs.fishing_bait))
        assertEquals(650, player.statMap.getFineXP(stats.fishing))
        assertMessageSent("You catch a raw slimy eel.")
    }

    @Test
    fun GameTestState.`eel fishing enforces membership level rod bait and capacity`() =
        runFishingTest { deps ->
            val npc = spawnCaveSpot(deps)

            prepare(level = 28)
            player.members = false
            add(objs.fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 27)
            add(objs.fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertMessageSent("You need a Fishing level of 28 to fish here.")

            prepare(level = 28)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 28)
            add(objs.fishing_rod)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertMessageSent("You do not have any fishing bait.")

            prepare(level = 28)
            fillInventory()
            player.inv[0] = InvObj(objs.fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait, 2)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.caveEels)
            }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`failed eel rolls preserve bait and repeat every five ticks`() =
        runFishingTest { deps ->
            prepare(level = 28)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 100
            random.then = 0
            val npc = spawnSwampSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.swampSlimyEel)
            }
            advance(5)
            assertEquals(2, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_slimy_eel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))

            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.raw_slimy_eel))
            assertEquals(650, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`pearl rod works and distance is revalidated before eel bait consumption`() =
        runFishingTest { deps ->
            prepare(level = 28)
            player.worn[0] = InvObj(objs.pearl_fishing_rod)
            add(objs.fishing_bait)
            random.next = 0
            val npc = spawnSwampSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.swampSlimyEel)
            }
            assertEquals(seqs.human_fishing_casting_pearl.id, player.pendingSequence.id)
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_slimy_eel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`Radas blessing and spirit flakes double eel into an open barrel`() =
        runFishingTest { deps ->
            prepare(level = 28)
            fillInventory()
            player.inv[0] = InvObj(objs.fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait, 2)
            player.inv[2] = InvObj(objs.fish_barrel_open)
            player.inv[3] = InvObj(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 0
            val npc = spawnSwampSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.swampSlimyEel)
            }
            advance(5)

            assertEquals(2, deps.barrel.count(player, objs.raw_slimy_eel))
            assertEquals(0, player.count(objs.raw_slimy_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(650, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fishing] = level
        player.stats.setCurrentLevel(stats.fishing, level)
        player.statMap.setFineXP(stats.fishing, 0)
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        val type = objTypes[obj]
        if (type.stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            player.inv[slot] = InvObj(obj, count)
        } else {
            repeat(count) {
                val slot = player.inv.indexOfFirst { it == null }
                player.inv[slot] = InvObj(obj)
            }
        }
    }

    private fun GameTestScope.fillInventory() {
        for (slot in player.inv.indices) {
            player.inv[slot] = InvObj(objs.coins)
        }
    }

    private fun GameTestScope.spawnCaveSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[CaveEelFishingNpcs.lumbridgeEast])

    private fun GameTestScope.spawnSwampSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[SwampEelFishingNpcs.mortMyreNorth])

    private fun GameTestState.runFishingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            FishingScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val fishing: StandardFishing,
        val barrel: FishBarrelStorage,
        val events: EventBus,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
    )

    private data class CatchContract(
        val output: Int,
        val level: Int,
        val xp: Double,
        val low: Int,
        val high: Int,
        val message: String,
    )

    private fun contract(catch: FishingCatch): CatchContract =
        CatchContract(catch.output.id, catch.level, catch.xp, catch.low, catch.high, catch.message)
}
