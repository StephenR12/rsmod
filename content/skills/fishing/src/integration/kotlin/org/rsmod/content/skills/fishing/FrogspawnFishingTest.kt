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
class FrogspawnFishingTest {
    @Test
    fun GameTestState.`cache 233 frogspawn spot net contract and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(
                listOf(1497, 1498, 1499, 1500, 10653),
                CaveEelFishingNpcs.all.map { it.id },
            )
            for (spot in CaveEelFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertTrue(type.op[0] == "Net" || type.op[0] == "Small Net", "spot=${spot.id}")
                assertEquals("Bait", type.op[2], "spot=${spot.id}")
                assertEquals("I can see fish swimming in the water.", type.desc, "spot=${spot.id}")
                assertEquals(0, type.wanderRange, "spot=${spot.id}")
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }

            assertEquals(5004, objs.frog_spawn.id)
            assertEquals(10978, objs.swamp_weed.id)
            assertEquals(303, objs.small_fishing_net.id)
            assertEquals(621, seqs.human_smallnet.id)
            assertFalse(deps.objs[objs.frog_spawn].stackable)
            assertFalse(deps.objs[objs.swamp_weed].stackable)
            assertNotNull(deps.seqs[seqs.human_smallnet])
            assertEquals(
                listOf(
                    CatchContract(5004, 33, 75.0, 16, 96, "You catch some frog spawn."),
                    CatchContract(10978, 33, 1.0, 10, 10, "You catch some swamp weed."),
                ),
                StandardFishingMethods.frogspawnNet.catches.map(::contract),
            )
            assertEquals(listOf(303), StandardFishingMethods.frogspawnNet.tools.map { it.type.id })
            assertTrue(StandardFishingMethods.frogspawnNet.membersOnly)
            assertFalse(objs.frog_spawn in FishBarrelStorage.accepted)
            assertFalse(objs.swamp_weed in FishBarrelStorage.accepted)
        }

    @Test
    fun GameTestState.`frog spawn roll precedes the swamp weed fallback`() =
        runFishingTest { deps ->
            prepare(level = 33)
            add(objs.small_fishing_net)
            val npc = spawnSpot(deps)

            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            advance(5)
            assertEquals(1, player.count(objs.frog_spawn))
            assertEquals(0, player.count(objs.swamp_weed))
            assertEquals(750, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch some frog spawn.")

            prepare(level = 33)
            add(objs.small_fishing_net)
            random.next = 100
            random.then = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            advance(5)
            assertEquals(0, player.count(objs.frog_spawn))
            assertEquals(1, player.count(objs.swamp_weed))
            assertEquals(10, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch some swamp weed.")
        }

    @Test
    fun GameTestState.`frogspawn net enforces membership level tool and capacity`() =
        runFishingTest { deps ->
            val npc = spawnSpot(deps)

            prepare(level = 33)
            player.members = false
            add(objs.small_fishing_net)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 32)
            add(objs.small_fishing_net)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            assertMessageSent("You need a Fishing level of 33 to fish here.")

            prepare(level = 33)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 33)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`failed frogspawn rolls repeat every five ticks`() = runFishingTest { deps ->
        prepare(level = 33)
        add(objs.small_fishing_net)
        random.next = 100
        random.then = 100
        random.then = 0
        val npc = spawnSpot(deps)

        player.withProtectedAccess {
            deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
        }
        advance(5)
        assertEquals(0, player.count(objs.frog_spawn))
        assertEquals(0, player.count(objs.swamp_weed))
        assertEquals(0, player.statMap.getFineXP(stats.fishing))

        advance(5)
        assertEquals(1, player.count(objs.frog_spawn))
        assertEquals(0, player.count(objs.swamp_weed))
        assertEquals(750, player.statMap.getFineXP(stats.fishing))
    }

    @Test
    fun GameTestState.`distance and small net are revalidated before the roll`() =
        runFishingTest { deps ->
            prepare(level = 33)
            add(objs.small_fishing_net)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            assertEquals(seqs.human_smallnet.id, player.pendingSequence.id)
            player.teleport(player.coords.translate(10, 0))
            advance(5)
            assertEquals(0, player.count(objs.frog_spawn))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))

            prepare(level = 33)
            add(objs.small_fishing_net)
            val secondNpc = spawnSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, secondNpc, StandardFishingMethods.frogspawnNet)
            }
            player.inv[player.inv.indexOfFirst { it?.id == objs.small_fishing_net.id }] = null
            advance(5)
            assertEquals(0, player.count(objs.frog_spawn))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`spirit flakes and Radas blessing can double frog spawn without extra xp`() =
        runFishingTest { deps ->
            prepare(level = 33)
            add(objs.small_fishing_net)
            add(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 57
            val npc = spawnSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.frogspawnNet)
            }
            advance(5)

            assertEquals(2, player.count(objs.frog_spawn))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(750, player.statMap.getFineXP(stats.fishing))
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

    private fun GameTestScope.spawnSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[CaveEelFishingNpcs.lumbridgeEast])

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
