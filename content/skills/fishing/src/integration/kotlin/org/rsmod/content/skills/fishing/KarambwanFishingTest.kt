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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.interact.HeldUInteractions
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
class KarambwanFishingTest {
    @Test
    fun GameTestState.`cache 233 karambwan contracts and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(listOf(4712, 4713), KarambwanFishingNpcs.postQuest.map { it.id })
            assertEquals(4714, KarambwanFishingNpcs.lubufuQuest.id)
            for (spot in KarambwanFishingNpcs.postQuest) {
                val type = deps.npcs[spot]
                assertEquals("Fish", type.op[0])
                assertEquals("Something dark green is lurking in these waters.", type.desc)
                assertEquals(0, type.wanderRange)
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }
            assertFalse(
                deps.events.contains(NpcEvents.Op1::class.java, KarambwanFishingNpcs.lubufuQuest.id)
            )

            assertEquals(3142, objs.raw_karambwan.id)
            assertEquals(3150, objs.raw_karambwanji.id)
            assertEquals(3157, objs.karambwan_vessel.id)
            assertEquals(3159, objs.baited_karambwan_vessel.id)
            assertEquals(1193, seqs.human_karambwan.id)
            assertEquals(320, varps.tbwt_main.id)
            assertEquals(3588, varbits.karamja_diary_medium_karambwan.id)
            assertFalse(deps.objs[objs.raw_karambwan].stackable)
            assertTrue(deps.objs[objs.raw_karambwanji].stackable)
            assertEquals(
                "A wide bodied and thin necked vessel, encrusted with sea salt.",
                deps.objs[objs.karambwan_vessel].desc,
            )
            assertEquals(
                "This Karambwan Vessel is loaded with Karambwanji.",
                deps.objs[objs.baited_karambwan_vessel].desc,
            )
            assertNotNull(deps.seqs[seqs.human_karambwan])
            assertTrue(
                deps.events.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.karambwan_vessel.id, objs.raw_karambwanji.id),
                )
            )

            assertEquals(65, KarambwanFishing.LEVEL_REQUIRED)
            assertEquals(6, KarambwanFishing.QUEST_COMPLETE)
            assertEquals(50.0, KarambwanFishing.CATCH_XP)
            assertEquals(5, KarambwanFishing.CATCH_LOW)
            assertEquals(160, KarambwanFishing.CATCH_HIGH)
            assertEquals(4, KarambwanFishing.ROLL_INTERVAL)
        }

    @Test
    fun GameTestState.`empty vessel loads atomically in either held item order`() =
        runFishingTest { deps ->
            repeat(2) { reverse ->
                prepare()
                fillInventory()
                player.inv[3] = InvObj(objs.karambwan_vessel)
                player.inv[8] = InvObj(objs.raw_karambwanji, 2)

                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.karambwan_vessel],
                            3,
                            deps.objs[objs.raw_karambwanji],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.raw_karambwanji],
                            8,
                            deps.objs[objs.karambwan_vessel],
                            3,
                        )
                    }
                }

                assertEquals(1, player.count(objs.baited_karambwan_vessel), "reverse=$reverse")
                assertEquals(0, player.count(objs.karambwan_vessel), "reverse=$reverse")
                assertEquals(1, player.count(objs.raw_karambwanji), "reverse=$reverse")
            }

            prepare()
            player.members = false
            add(objs.karambwan_vessel)
            add(objs.raw_karambwanji)
            val vesselSlot = player.inv.indexOfFirst { it?.id == objs.karambwan_vessel.id }
            val baitSlot = player.inv.indexOfFirst { it?.id == objs.raw_karambwanji.id }
            player.withProtectedAccess { deps.fishing.loadVessel(this, vesselSlot, baitSlot) }
            assertEquals(1, player.count(objs.karambwan_vessel))
            assertEquals(1, player.count(objs.raw_karambwanji))
        }

    @Test
    fun GameTestState.`post quest fishing enforces membership quest level vessel and capacity`() =
        runFishingTest { deps ->
            val npc = spawnSpot(deps)

            prepare()
            player.members = false
            add(objs.baited_karambwan_vessel)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(quest = 5)
            add(objs.baited_karambwan_vessel)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need to complete Tai Bwo Wannai Trio before fishing here.")

            prepare(level = 64)
            add(objs.baited_karambwan_vessel)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need a Fishing level of 65 to fish here.")

            prepare()
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You need a baited karambwan vessel to fish here.")

            prepare()
            fillInventory()
            player.inv[0] = InvObj(objs.baited_karambwan_vessel)
            player.inv[1] = InvObj(objs.raw_karambwanji, 2)
            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`karambwan rolls every four ticks reloads and awards exact xp`() =
        runFishingTest { deps ->
            prepare()
            add(objs.baited_karambwan_vessel)
            add(objs.raw_karambwanji, 2)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            assertEquals(seqs.human_karambwan.id, player.pendingSequence.id)
            assertMessageSent(KarambwanFishing.LOWER_VESSEL_MESSAGE)
            advance(3)
            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(2, player.count(objs.raw_karambwanji))
            advance()

            assertEquals(1, player.count(objs.raw_karambwan))
            assertEquals(1, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.baited_karambwan_vessel))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
            assertEquals(1, player.vars[varbits.karamja_diary_medium_karambwan])
            assertMessageSent(KarambwanFishing.CATCH_MESSAGE)
        }

    @Test
    fun GameTestState.`failed roll consumes bait reloads and sends the sourced message`() =
        runFishingTest { deps ->
            prepare()
            add(objs.baited_karambwan_vessel)
            add(objs.raw_karambwanji)
            random.next = 255
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(4)

            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(0, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.baited_karambwan_vessel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
            assertMessageSent(KarambwanFishing.BAIT_STOLEN_MESSAGE)
        }

    @Test
    fun GameTestState.`last loaded vessel becomes empty after its final attempt`() =
        runFishingTest { deps ->
            prepare()
            add(objs.baited_karambwan_vessel)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(4)

            assertEquals(1, player.count(objs.raw_karambwan))
            assertEquals(1, player.count(objs.karambwan_vessel))
            assertEquals(0, player.count(objs.baited_karambwan_vessel))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`rada and spirit flakes double into an open barrel from a full inventory`() =
        runFishingTest { deps ->
            prepare()
            fillInventory()
            player.inv[0] = InvObj(objs.baited_karambwan_vessel)
            player.inv[1] = InvObj(objs.raw_karambwanji, 2)
            player.inv[2] = InvObj(objs.fish_barrel_open)
            player.inv[3] = InvObj(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            random.next = 0
            random.then = 57
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            advance(4)

            assertEquals(2, deps.barrel.count(player, objs.raw_karambwan))
            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(1, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(500, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`karambwan revalidates distance before consuming the vessel bait`() =
        runFishingTest { deps ->
            prepare()
            add(objs.baited_karambwan_vessel)
            add(objs.raw_karambwanji)
            random.next = 0
            val npc = spawnSpot(deps)

            player.withProtectedAccess { deps.fishing.fish(this, npc) }
            player.teleport(player.coords.translate(10, 0))
            advance(4)

            assertEquals(0, player.count(objs.raw_karambwan))
            assertEquals(1, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.baited_karambwan_vessel))
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int = 65, quest: Int = 6) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fishing] = level
        player.stats.setCurrentLevel(stats.fishing, level)
        player.statMap.setFineXP(stats.fishing, 0)
        player.withProtectedAccess {
            vars[varps.tbwt_main] = quest
            vars[varbits.karamja_diary_medium_karambwan] = 0
        }
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
        spawnNpc(player.coords.translate(1, 0), deps.npcs[KarambwanFishingNpcs.fixed])

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
        val fishing: KarambwanFishing,
        val barrel: FishBarrelStorage,
        val events: EventBus,
        val heldU: HeldUInteractions,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
    )
}
