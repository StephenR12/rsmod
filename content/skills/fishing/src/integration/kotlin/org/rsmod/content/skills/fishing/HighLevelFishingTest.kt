package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class HighLevelFishingTest {
    @Test
    fun GameTestState.`native high level spots expose supported operations and stay fixed`() =
        runFishingTest { deps ->
            for ((type, operation) in
                listOf(
                    HighLevelFishingNpcs.monkfish to "Net",
                    HighLevelFishingNpcs.angler to "Bait",
                    HighLevelFishingNpcs.darkCoast to "Cage",
                    HighLevelFishingNpcs.darkResource to "Cage",
                )) {
                assertEquals(operation, deps.npcs[type].op[0])
                assertEquals(0, deps.npcs[type].wanderRange)
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, type.id))
            }
            assertEquals("Harpoon", deps.npcs[HighLevelFishingNpcs.monkfish].op[2])
            assertTrue(
                deps.events.contains(NpcEvents.Op3::class.java, HighLevelFishingNpcs.monkfish.id)
            )
            assertEquals(2098, HighLevelFishingBits.swan.id)
            assertEquals(13431, HighLevelFishingObjs.sandworms.id)
            assertEquals(30773, HighLevelFishingObjs.diabolic.id)
            assertEquals(11940, HighLevelFishingObjs.darkBait.id)
        }

    @Test
    fun GameTestState.`monkfish requires completed Swan Song then awards exact catch XP`() =
        runFishingTest { deps ->
            prepare(62)
            add(objs.small_fishing_net)
            player.withProtectedAccess { vars[HighLevelFishingBits.swan] = 199 }
            val npc = spawn(HighLevelFishingNpcs.monkfish, deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.monkfish)
            }
            assertMessageSent("You need to complete Swan Song before fishing for monkfish.")
            assertEquals(0, player.count(objs.raw_monkfish))
            player.withProtectedAccess { vars[HighLevelFishingBits.swan] = 200 }
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.monkfish)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_monkfish))
            assertEquals(1200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`anglerfish native interaction chooses diabolic worms and reduced XP`() =
        runFishingTest { deps ->
            prepare(82)
            add(objs.fishing_rod)
            add(HighLevelFishingObjs.diabolic, 2)
            random.next = 0
            deps.interactions.interact(
                player,
                spawn(HighLevelFishingNpcs.angler, deps),
                InteractionOp.Op1,
            )
            advance(6)
            assertEquals(1, player.count(objs.raw_anglerfish))
            assertEquals(1, player.count(HighLevelFishingObjs.diabolic))
            assertEquals(792, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`anglerfish requires correct bait and sandworms retain normal XP`() =
        runFishingTest { deps ->
            prepare(82)
            add(objs.fishing_rod)
            add(objs.fishing_bait)
            val npc = spawn(HighLevelFishingNpcs.angler, deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.angler)
            }
            assertMessageSent("You need sandworms or diabolic worms to fish for anglerfish.")
            add(HighLevelFishingObjs.sandworms)
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.angler)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_anglerfish))
            assertEquals(0, player.count(HighLevelFishingObjs.sandworms))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1200, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState
        .`dark crab requires dark bait and failed elite bonus retains ordinary roll`() =
        runFishingTest { deps ->
            prepare(85)
            add(objs.lobster_pot)
            val npc = spawn(HighLevelFishingNpcs.darkResource, deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            assertMessageSent("You need dark fishing bait to catch dark crabs.")
            add(HighLevelFishingObjs.darkBait, 2)
            player.withProtectedAccess {
                vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete] = 1
            }
            random.then = 100
            random.then = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_dark_crab))
            assertEquals(1, player.count(HighLevelFishingObjs.darkBait))
            assertEquals(1300, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState
        .`elite dark crab bonus succeeds above ordinary rate without consuming second roll`() =
        runFishingTest { deps ->
            prepare(99)
            add(objs.lobster_pot)
            add(HighLevelFishingObjs.darkBait)
            player.withProtectedAccess {
                vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete] = 1
            }
            random.then = 18
            random.next = 100
            val npc = spawn(HighLevelFishingNpcs.darkCoast, deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            advance(5)
            assertEquals(1, player.count(objs.raw_dark_crab))
            assertEquals(1300, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState
        .`diabolic bait has priority then switches to sandworms with the next roll speed bonus`() =
        runFishingTest { deps ->
            prepare(82)
            add(objs.fishing_rod)
            add(HighLevelFishingObjs.diabolic)
            add(HighLevelFishingObjs.sandworms, 2)
            random.next = 0
            deps.interactions.interact(
                player,
                spawn(HighLevelFishingNpcs.angler, deps),
                InteractionOp.Op1,
            )
            advance(6)
            assertEquals(1, player.count(objs.raw_anglerfish))
            assertEquals(0, player.count(HighLevelFishingObjs.diabolic))
            assertEquals(2, player.count(HighLevelFishingObjs.sandworms))
            assertEquals(792, player.statMap.getFineXP(stats.fishing))
            advance(4)
            assertEquals(2, player.count(objs.raw_anglerfish))
            assertEquals(1, player.count(HighLevelFishingObjs.sandworms))
            assertEquals(1992, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`members level and full inventory gates preserve dark bait`() =
        runFishingTest { deps ->
            prepare(84)
            add(objs.lobster_pot)
            add(HighLevelFishingObjs.darkBait, 2)
            val npc = spawn(HighLevelFishingNpcs.darkCoast, deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            assertMessageSent("You need a Fishing level of 85 to fish here.")
            player.stats[stats.fishing] = 85
            player.statMap.setFineXP(stats.fishing, 0)
            player.members = false
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            assertMessageSent("You need to be on a members' world to fish here.")
            player.members = true
            while (!player.inv.isFull()) add(objs.coins)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, HighLevelFishingMethods.darkCrab)
            }
            assertMessageSent("You do not have enough inventory space.")
            assertEquals(2, player.count(HighLevelFishingObjs.darkBait))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.clearAllInvs()
        player.stats[stats.fishing] = level
        player.statMap.setFineXP(stats.fishing, 0)
    }

    private fun GameTestScope.add(type: ObjType, count: Int = 1) {
        player.inv[player.inv.indexOfFirst { it == null }] = InvObj(type, count)
    }

    private fun GameTestScope.spawn(type: NpcType, deps: Dependencies) =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[type])

    private fun GameTestState.runFishingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            HighLevelFishingScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val fishing: StandardFishing,
        val npcs: NpcTypeList,
        val interactions: NpcInteractions,
        val events: EventBus,
    )
}
