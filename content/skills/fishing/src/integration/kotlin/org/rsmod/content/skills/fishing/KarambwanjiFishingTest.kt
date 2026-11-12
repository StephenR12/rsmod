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
class KarambwanjiFishingTest {
    @Test
    fun GameTestState.`cache 233 karambwanji spot objects chance and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(listOf(4710), KarambwanjiFishingNpcs.all.map { it.id })
            val spot = deps.npcs[KarambwanjiFishingNpcs.holyLake]
            assertEquals("Net", spot.op[0])
            assertEquals("There are brightly coloured fish swimming in the water.", spot.desc)
            assertTrue(
                deps.events.contains(NpcEvents.Op1::class.java, KarambwanjiFishingNpcs.holyLake.id)
            )

            assertEquals(303, objs.small_fishing_net.id)
            assertEquals(3150, objs.raw_karambwanji.id)
            assertEquals(317, objs.raw_shrimps.id)
            assertEquals(621, seqs.human_smallnet.id)
            assertTrue(deps.objs[objs.raw_karambwanji].stackable)
            assertFalse(deps.objs[objs.raw_shrimps].stackable)
            assertNotNull(deps.seqs[seqs.human_smallnet])

            assertEquals(5, KarambwanjiFishing.LEVEL_REQUIRED)
            assertEquals(5.0, KarambwanjiFishing.CATCH_XP)
            assertEquals(100, KarambwanjiFishing.KARAMBWANJI_LOW)
            assertEquals(250, KarambwanjiFishing.KARAMBWANJI_HIGH)
            assertEquals(48, KarambwanjiFishing.SHRIMPS_LOW)
            assertEquals(256, KarambwanjiFishing.SHRIMPS_HIGH)
            assertEquals(6, KarambwanjiFishing.ROLL_INTERVAL)
        }

    @Test
    fun GameTestState.`karambwanji starts only with membership level net and capacity`() =
        runFishingTest { deps ->
            val npc = spawnSpot(deps)

            prepare(level = 5)
            player.members = false
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 4)
            add(objs.small_fishing_net)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need a Fishing level of 5 to fish here.")

            prepare(level = 5)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 5)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`karambwanji rolls every six ticks and awards five xp per catch`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.small_fishing_net)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertEquals(seqs.human_smallnet.id, player.pendingSequence.id)
            advance(5)
            assertEquals(0, player.count(objs.raw_karambwanji))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
            advance()
            assertEquals(2, player.count(objs.raw_karambwanji))
            assertEquals(50, player.statMap.getFineXP(stats.fishing))
            assertMessageSent(KarambwanjiFishing.KARAMBWANJI_MESSAGE)
        }

    @Test
    fun GameTestState.`karambwanji quantity uses the boosted visible fishing level`() =
        runFishingTest { deps ->
            assertEquals(2, KarambwanjiFishing.karambwanjiQuantity(5))
            assertEquals(9, KarambwanjiFishing.karambwanjiQuantity(40))
            assertEquals(20, KarambwanjiFishing.karambwanjiQuantity(99))
            assertEquals(21, KarambwanjiFishing.karambwanjiQuantity(100))

            prepare(level = 99)
            player.stats.setCurrentLevel(stats.fishing, 100)
            add(objs.small_fishing_net)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            add(objs.spirit_flakes, 2)
            random.next = 0
            random.then = 57
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(6)

            assertEquals(42, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(50, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`karambwanji failed double roll consumes one flake and keeps base amount`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.small_fishing_net)
            add(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 58
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(6)

            assertEquals(2, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(50, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`karambwanji cascades to shrimps and can produce nothing`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.small_fishing_net)
            random.next = 99
            random.then = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(6)

            assertEquals(0, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.raw_shrimps))
            assertEquals(50, player.statMap.getFineXP(stats.fishing))
            assertMessageSent(KarambwanjiFishing.SHRIMPS_MESSAGE)
        }

    @Test
    fun GameTestState.`full inventory stack suppresses shrimp without interrupting fishing`() =
        runFishingTest { deps ->
            prepare(level = 5)
            fillInventory()
            player.inv[0] = InvObj(objs.small_fishing_net)
            player.inv[1] = InvObj(objs.raw_karambwanji, 100)
            random.next = 99
            random.then = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(6)
            assertEquals(100, player.count(objs.raw_karambwanji))
            assertEquals(0, player.count(objs.raw_shrimps))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
            advance(6)
            assertEquals(102, player.count(objs.raw_karambwanji))
            assertEquals(50, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`karambwanji revalidates distance before each roll`() =
        runFishingTest { deps ->
            prepare(level = 5)
            add(objs.small_fishing_net)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            player.teleport(player.coords.translate(10, 0))
            advance(6)

            assertEquals(0, player.count(objs.raw_karambwanji))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fishing] = level
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
        spawnNpc(player.coords.translate(1, 0), deps.npcs[KarambwanjiFishingNpcs.holyLake])

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
        val fishing: KarambwanjiFishing,
        val events: EventBus,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
    )
}
