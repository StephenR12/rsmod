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
class SpecialEelFishingTest {
    @Test
    fun GameTestState.`cache 233 special eel spots methods and dispatch are pinned`() =
        runFishingTest { deps ->
            assertEquals(7676, SpecialEelFishingNpcs.infernal.id)
            assertEquals(6488, SpecialEelFishingNpcs.sacred.id)
            val infernalSpot = deps.npcs[SpecialEelFishingNpcs.infernal]
            val sacredSpot = deps.npcs[SpecialEelFishingNpcs.sacred]
            assertEquals("Bait", infernalSpot.op[0])
            assertEquals("It looks like there are some large eels in the lava.", infernalSpot.desc)
            assertEquals("Bait", sacredSpot.op[0])
            assertEquals("It looks like something's moving under here.", sacredSpot.desc)
            for (spot in SpecialEelFishingNpcs.all) {
                assertEquals(0, deps.npcs[spot].wanderRange)
                assertTrue(deps.events.contains(NpcEvents.Op1::class.java, spot.id))
            }

            assertEquals(21293, objs.infernal_eel.id)
            assertEquals(13339, objs.sacred_eel.id)
            assertEquals(1585, objs.oily_fishing_rod.id)
            assertEquals(23122, objs.oily_pearl_fishing_rod.id)
            assertEquals(1580, objs.ice_gloves.id)
            assertEquals(27031, objs.smiths_gloves_i.id)
            assertEquals(4391, varbits.snakeboss_info.id)
            assertFalse(deps.objs[objs.infernal_eel].stackable)
            assertFalse(deps.objs[objs.sacred_eel].stackable)
            assertEquals(
                listOf(null, null, null, "Eat", null),
                deps.objs[objs.infernal_eel].iop.toList(),
            )
            assertEquals(
                listOf(null, null, null, "Eat", null),
                deps.objs[objs.sacred_eel].iop.toList(),
            )
            assertNotNull(deps.seqs[seqs.human_fishing_casting])
            assertNotNull(deps.seqs[seqs.human_fishing_casting_pearl])
            assertTrue(objs.infernal_eel in FishBarrelStorage.accepted)
            assertTrue(objs.sacred_eel in FishBarrelStorage.accepted)

            assertEquals(
                CatchContract(21293, 80, 95.0, 30, 93, "You catch an infernal eel."),
                contract(StandardFishingMethods.infernalEel.catches.single()),
            )
            assertEquals(
                CatchContract(13339, 87, 105.0, 0, 60, "You catch a sacred eel."),
                contract(StandardFishingMethods.sacredEel.catches.single()),
            )
            assertEquals(
                listOf(1585, 23122),
                StandardFishingMethods.infernalEel.tools.map { it.type.id },
            )
            assertEquals(
                listOf(1580, 27031),
                StandardFishingMethods.infernalEel.requiredWorn.map { it.id },
            )
            assertEquals(
                listOf(307, 22846),
                StandardFishingMethods.sacredEel.tools.map { it.type.id },
            )
            assertEquals(
                varbits.snakeboss_info,
                StandardFishingMethods.sacredEel.varbitRequirement?.type,
            )
            assertEquals(1, StandardFishingMethods.sacredEel.varbitRequirement?.minimumValue)
        }

    @Test
    fun GameTestState.`infernal eel uses oily rod ice gloves bait and exact reward`() =
        runFishingTest { deps ->
            prepare(level = 80)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait, 2)
            player.worn[0] = InvObj(objs.ice_gloves)
            random.next = 0
            val npc = spawnInfernalSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            advance(5)

            assertEquals(1, player.count(objs.infernal_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(950, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch an infernal eel.")
        }

    @Test
    fun GameTestState.`sacred eel requires priestess education and awards its exact reward`() =
        runFishingTest { deps ->
            prepare(level = 87)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            val npc = spawnSacredSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.sacredEel)
            }
            assertMessageSent(
                "You know nothing of the sacred contract between our tribe and the mighty Zulrah! " +
                    "Ask the High Priestess to educate you about Zulrah before you meddle further."
            )

            prepare(level = 87)
            add(objs.fishing_rod)
            add(objs.fishing_bait, 2)
            player.withProtectedAccess { vars[varbits.snakeboss_info] = 1 }
            random.next = 0
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.sacredEel)
            }
            advance(5)

            assertEquals(1, player.count(objs.sacred_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1050, player.statMap.getFineXP(stats.fishing))
            assertMessageSent("You catch a sacred eel.")
        }

    @Test
    fun GameTestState
        .`special eel fishing enforces membership level tools bait gloves and capacity`() =
        runFishingTest { deps ->
            val npc = spawnInfernalSpot(deps)

            prepare(level = 80)
            player.members = false
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            player.worn[0] = InvObj(objs.ice_gloves)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You need to be on a members' world to fish here.")

            prepare(level = 79)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            player.worn[0] = InvObj(objs.ice_gloves)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You need a Fishing level of 80 to fish here.")

            prepare(level = 80)
            add(objs.fishing_bait)
            player.worn[0] = InvObj(objs.ice_gloves)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You do not have the required equipment to fish here.")

            prepare(level = 80)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You need to be wearing ice gloves to fish here.")

            prepare(level = 80)
            add(objs.oily_fishing_rod)
            player.worn[0] = InvObj(objs.ice_gloves)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You do not have any fishing bait.")

            prepare(level = 80)
            fillInventory()
            player.inv[0] = InvObj(objs.oily_fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait, 2)
            player.worn[0] = InvObj(objs.ice_gloves)
            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            assertMessageSent("You do not have enough inventory space.")
        }

    @Test
    fun GameTestState.`failed special eel roll preserves bait and repeats every five ticks`() =
        runFishingTest { deps ->
            prepare(level = 80)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait, 2)
            player.worn[0] = InvObj(objs.ice_gloves)
            random.next = 200
            random.then = 0
            val npc = spawnInfernalSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.infernalEel)
            }
            advance(5)
            assertEquals(2, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.infernal_eel))
            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.infernal_eel))
            assertEquals(950, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`special eel pearl rods and smiths ice gloves work from worn slots`() =
        runFishingTest { deps ->
            prepare(level = 80)
            player.worn[0] = InvObj(objs.oily_pearl_fishing_rod)
            player.worn[1] = InvObj(objs.smiths_gloves_i)
            add(objs.fishing_bait)
            random.next = 0
            val infernalNpc = spawnInfernalSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, infernalNpc, StandardFishingMethods.infernalEel)
            }
            assertEquals(seqs.human_fishing_casting_pearl.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.infernal_eel))
            player.teleport(player.coords.translate(10, 0))
            advance(5)

            prepare(level = 87)
            player.worn[0] = InvObj(objs.pearl_fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess { vars[varbits.snakeboss_info] = 1 }
            random.next = 0
            val sacredNpc = spawnSacredSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, sacredNpc, StandardFishingMethods.sacredEel)
            }
            assertEquals(seqs.human_fishing_casting_pearl.id, player.pendingSequence.id)
            advance(5)
            assertEquals(1, player.count(objs.sacred_eel))
        }

    @Test
    fun GameTestState.`special access requirements are revalidated before consuming bait`() =
        runFishingTest { deps ->
            prepare(level = 80)
            add(objs.oily_fishing_rod)
            add(objs.fishing_bait)
            player.worn[0] = InvObj(objs.ice_gloves)
            random.next = 0
            val infernalNpc = spawnInfernalSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, infernalNpc, StandardFishingMethods.infernalEel)
            }
            player.worn[0] = null
            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.infernal_eel))

            prepare(level = 87)
            add(objs.fishing_rod)
            add(objs.fishing_bait)
            player.withProtectedAccess { vars[varbits.snakeboss_info] = 1 }
            random.next = 0
            val sacredNpc = spawnSacredSpot(deps)
            player.withProtectedAccess {
                deps.fishing.fish(this, sacredNpc, StandardFishingMethods.sacredEel)
            }
            player.setVarBit(varbits.snakeboss_info, 0)
            advance(5)
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(0, player.count(objs.sacred_eel))
        }

    @Test
    fun GameTestState.`Radas blessing and spirit flakes double sacred eel into an open barrel`() =
        runFishingTest { deps ->
            prepare(level = 87)
            fillInventory()
            player.inv[0] = InvObj(objs.fishing_rod)
            player.inv[1] = InvObj(objs.fishing_bait, 2)
            player.inv[2] = InvObj(objs.fish_barrel_open)
            player.inv[3] = InvObj(objs.spirit_flakes, 2)
            player.worn[0] = InvObj(objs.diary_radas_blessing_4)
            player.withProtectedAccess { vars[varbits.snakeboss_info] = 1 }
            random.next = 0
            random.then = 0
            val npc = spawnSacredSpot(deps)

            player.withProtectedAccess {
                deps.fishing.fish(this, npc, StandardFishingMethods.sacredEel)
            }
            advance(5)

            assertEquals(2, deps.barrel.count(player, objs.sacred_eel))
            assertEquals(0, player.count(objs.sacred_eel))
            assertEquals(1, player.count(objs.fishing_bait))
            assertEquals(1, player.count(objs.spirit_flakes))
            assertEquals(1050, player.statMap.getFineXP(stats.fishing))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fishing] = level
        player.stats.setCurrentLevel(stats.fishing, level)
        player.statMap.setFineXP(stats.fishing, 0)
        player.withProtectedAccess { vars[varbits.snakeboss_info] = 0 }
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

    private fun GameTestScope.spawnInfernalSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[SpecialEelFishingNpcs.infernal])

    private fun GameTestScope.spawnSacredSpot(deps: Dependencies): Npc =
        spawnNpc(player.coords.translate(1, 0), deps.npcs[SpecialEelFishingNpcs.sacred])

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
