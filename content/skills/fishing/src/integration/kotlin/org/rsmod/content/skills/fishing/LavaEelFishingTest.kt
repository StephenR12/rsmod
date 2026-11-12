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
import org.rsmod.api.config.refs.varps
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
class LavaEelFishingTest {
    @Test
    fun GameTestState.`cache 233 lava eel spots method and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(listOf(4928, 6784), LavaEelFishingNpcs.all.map { it.id })
            for (spot in LavaEelFishingNpcs.all) {
                val type = deps.npcs[spot]
                assertEquals("Bait", type.op[0])
                assertEquals("I can see eels swimming in the lava.", type.desc)
                assertEquals(0, type.wanderRange)
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }

            assertEquals(313, objs.fishing_bait.id)
            assertEquals(1585, objs.oily_fishing_rod.id)
            assertEquals(2148, objs.raw_lava_eel.id)
            assertEquals(23122, objs.oily_pearl_fishing_rod.id)
            assertNotNull(deps.seqs[seqs.human_fishing_casting])
            assertNotNull(deps.seqs[seqs.human_fishing_casting_pearl])
            assertTrue(objs.raw_lava_eel in FishBarrelStorage.accepted)

            val method = StandardFishingMethods.lavaEel
            val catch = method.catches.single()
            assertEquals(listOf(1585, 23122), method.tools.map { it.type.id })
            assertEquals(listOf(false, true), method.tools.map { it.wearable })
            assertEquals(objs.fishing_bait, method.bait)
            assertFalse(method.membersOnly)
            assertEquals(2148, catch.output.id)
            assertEquals(53, catch.level)
            assertEquals(60.0, catch.xp)
            assertEquals(16, catch.low)
            assertEquals(96, catch.high)
            assertEquals("You catch a lava eel.", catch.message)
        }

    @Test
    fun GameTestState.`lava eel is deliberately available in a free world without quest state`() =
        runFishingTest { deps ->
            prepare(level = 53, members = false)
            player.setVarp(varps.heroes_quest, 0)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 0
            val npc = spawnSpot(deps, LavaEelFishingNpcs.taverleyDungeon)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            advance(5)

            assertEquals(1, player.count(objs.raw_lava_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(600, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a lava eel.")
        }

    @Test
    fun GameTestState.`lava eel enforces level oily rod bait and capacity`() =
        runFishingTest { deps ->
            val npc = spawnSpot(deps, LavaEelFishingNpcs.lavaMaze)

            prepare(level = 52)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            assertMessageSent("You need a Fishing level of 53 to fish here.")

            prepare(level = 53)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 53)
            add(objs.oily_fishing_rod)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            assertMessageSent("You do not have any fishing bait.")

            prepare(level = 53)
            fillInventory()
            player.inv[0] = InvObj(objs.oily_fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait, 2)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`failed lava eel roll preserves bait and retries every five ticks`() =
        runFishingTest { deps ->
            prepare(level = 53)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait, 2)
            random.next = 200
            random.then = 0
            val npc = spawnSpot(deps, LavaEelFishingNpcs.taverleyDungeon)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            advance(5)
            assertEquals(2, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_lava_eel))
            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.raw_lava_eel))
            assertEquals(600, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`worn oily pearl rod fishes lava eels and removal interrupts atomically`() =
        runFishingTest { deps ->
            prepare(level = 53)
            player.worn[0] = InvObj(objs.oily_pearl_fishing_rod)
            add(objs.fishing_bait)
            random.next = 0
            val npc = spawnSpot(deps, LavaEelFishingNpcs.lavaMaze)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            assertEquals(seqs.human_fishing_casting_pearl.id, player.pendingSequence.id)
            player.worn[0] = null
            advance(5)

            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_lava_eel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`moving away interrupts lava eel fishing before reward`() =
        runFishingTest { deps ->
            prepare(level = 53)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            random.next = 0
            val npc = spawnSpot(deps, LavaEelFishingNpcs.taverleyDungeon)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.lavaEel)
            }
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.raw_lava_eel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.stats[stats.fishing] = level
        player.stats.setCurrentLevel(stats.fishing, level)
        player.statMap.setFineXP(stats.fishing, 0)
        player.setVarp(varps.heroes_quest, 0)
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

    private fun GameTestScope.spawnSpot(
        deps: Dependencies,
        type: org.rsmod.game.type.npc.NpcType,
    ): Npc = spawnNpc(player.coords.translate(1, 0), deps.npcs[type])

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
}
