package org.rsmod.content.skills.hunter.falconry

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.hands
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchFamily
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.events.EventBus
import org.rsmod.game.entity.NpcList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FalconryTest {
    @Test
    fun GameTestState.`cache identities and exact dated catch curves are pinned`() =
        runFalconryTest { _ ->
            assertEquals(listOf(5531, 5532, 5533), FalconrySpeciesList.all.map { it.npc.id })
            assertEquals(listOf(1342, 1344, 1343), FalconrySpeciesList.all.map { it.caught.id })
            assertEquals(10023, FalconryObjs.glove.id)
            assertEquals(10024, FalconryObjs.falcon.id)
            assertEquals(listOf(43, 57, 69), FalconrySpeciesList.all.map { it.level })
            assertEquals(1.0, FalconrySpeciesList.all[0].chance(99))
            assertEquals(254.0 / 256.0, FalconrySpeciesList.all[1].chance(99))
            assertEquals(206.0 / 256.0, FalconrySpeciesList.all[2].chance(99))
        }

    @Test
    fun GameTestState
        .`rental equips directly with a full inventory and permanent investment removes future fees`() =
        runFalconryTest { deps ->
            prepare()
            player.fillInv()
            player.inv[0] = InvObj(objs.coins, 1000)
            player.withProtectedAccess { assertTrue(deps.falconry.rent(this)) }
            assertEquals(500, player.count(objs.coins))
            assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
            deps.falconry.leave(player)
            player.clearInv()
            player.inv[0] = InvObj(objs.coins, 500_000)
            player.withProtectedAccess { assertTrue(deps.falconry.invest(this)) }
            assertEquals(1, player.vars[FalconryVars.invested])
            assertEquals(0, player.count(objs.coins))
            player.withProtectedAccess { assertTrue(deps.falconry.rent(this)) }
            assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
        }

    @Test
    fun GameTestState.`each successful catch requires retrieval for exact fur meat bones and xp`() =
        runFalconryTest { deps ->
            prepare()
            player.righthand = InvObj(FalconryObjs.falcon)
            for (species in FalconrySpeciesList.all) {
                player.clearInv()
                val xp = player.statMap.getFineXP(stats.hunter)
                val prey =
                    spawnNpc(player.coords.translateX(1), deps.npcTypes[species.npc]) { noneMode() }
                random.next = 0
                player.withProtectedAccess { deps.falconry.launch(this, prey) }
                assertEquals(FalconryObjs.glove.id, player.righthand?.id)
                advance(6)
                assertEquals(xp, player.statMap.getFineXP(stats.hunter))
                val caught = deps.npcs.first { it.id == species.caught.id && it.isValidTarget() }
                player.withProtectedAccess { deps.falconry.retrieve(this, caught) }
                assertEquals(1, player.count(species.fur))
                assertEquals(1, player.count(objs.bones))
                species.meat?.let { assertEquals(1, player.count(it)) }
                assertEquals((species.xp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - xp)
                assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
            }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchDashingKebbit))
        }

    @Test
    fun GameTestState
        .`failed flight returns falcon without xp and equipment or low level blocks rental`() =
        runFalconryTest { deps ->
            prepare()
            player.inv[0] = InvObj(objs.coins, 500)
            player.hands = InvObj(objs.hammer)
            player.withProtectedAccess { assertFalse(deps.falconry.rent(this)) }
            player.hands = null
            player.stats[stats.hunter] = 42
            player.withProtectedAccess { assertFalse(deps.falconry.rent(this)) }
            assertEquals(500, player.count(objs.coins))
            player.setBaseLevel(stats.hunter, 68)
            player.setCurrentLevel(stats.hunter, 69)
            player.withProtectedAccess { assertTrue(deps.falconry.rent(this)) }
            val prey =
                spawnNpc(player.coords.translateX(1), deps.npcTypes[FalconryNpcs.dashing]) {
                    noneMode()
                }
            val xp = player.statMap.getFineXP(stats.hunter)
            random.next = 255
            player.withProtectedAccess { deps.falconry.launch(this, prey) }
            advance(6)
            assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
            assertEquals(xp, player.statMap.getFineXP(stats.hunter))
            assertTrue(prey.isValidTarget())
        }

    @Test
    fun GameTestState.`prey ownership and full-inventory retry prevent theft and reward loss`() =
        runFalconryTest { deps ->
            prepare()
            player.righthand = InvObj(FalconryObjs.falcon)
            val species = FalconrySpeciesList.all[0]
            val prey =
                spawnNpc(player.coords.translateX(1), deps.npcTypes[species.npc]) { noneMode() }
            random.next = 0
            player.withProtectedAccess { deps.falconry.launch(this, prey) }
            advance(6)
            val caught = deps.npcs.first { it.id == species.caught.id && it.isValidTarget() }
            val other = registerPlayer(player.coords)
            other.members = true
            other.withProtectedAccess { deps.falconry.retrieve(this, caught) }
            assertEquals(0, other.count(species.fur))
            player.fillInv()
            player.withProtectedAccess { deps.falconry.retrieve(this, caught) }
            assertTrue(caught.isValidTarget())
            player.inv[0] = null
            player.inv[1] = null
            player.withProtectedAccess { deps.falconry.retrieve(this, caught) }
            assertEquals(1, player.count(species.fur))
            assertFalse(caught.isValidTarget())
        }

    @Test
    fun GameTestState
        .`timed out prey yields no xp and Matthias recovers the falcon without a second payment`() =
        runFalconryTest { deps ->
            prepare()
            player.inv[0] = InvObj(objs.coins, 500)
            player.withProtectedAccess { assertTrue(deps.falconry.rent(this)) }
            val prey =
                spawnNpc(player.coords.translateX(1), deps.npcTypes[FalconryNpcs.spotted]) {
                    noneMode()
                }
            random.next = 0
            val xp = player.statMap.getFineXP(stats.hunter)
            player.withProtectedAccess { deps.falconry.launch(this, prey) }
            advance(Falconry.RETRIEVE_TIMEOUT_TICKS + 6)
            assertEquals(xp, player.statMap.getFineXP(stats.hunter))
            assertEquals(FalconryObjs.glove.id, player.righthand?.id)
            player.withProtectedAccess { assertTrue(deps.falconry.rent(this)) }
            assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
            player.placeAt(CoordGrid(3200, 3200))
            advance(2)
            assertEquals(null, player.righthand)
        }

    @Test
    fun GameTestState
        .`native Quick-falcon supports ranged catches without walking into melee distance`() =
        runFalconryTest { deps ->
            prepare()
            player.inv[0] = InvObj(objs.coins, 500)
            val matthias =
                spawnNpc(player.coords.translateX(1), deps.npcTypes[FalconryNpcs.matthias]) {
                    noneMode()
                }
            player.opNpc3(matthias)
            advance()
            assertEquals(FalconryObjs.falcon.id, player.righthand?.id)
            val origin = player.coords
            val prey =
                spawnNpc(player.coords.translateX(5), deps.npcTypes[FalconryNpcs.spotted]) {
                    noneMode()
                }
            random.next = 0
            player.opNpc1(prey)
            advance(6)
            assertTrue(kotlin.math.abs(player.coords.x - origin.x) <= 1)
            assertTrue(kotlin.math.abs(player.coords.x - prey.coords.x) > 1)
            val caught =
                deps.npcs.first { it.id == FalconryNpcs.onSpotted.id && it.isValidTarget() }
            player.placeAt(caught.coords.translateX(-1))
            player.opNpc1(caught)
            advance(2)
            assertEquals(1, player.count(objs.spotted_kebbit_fur))
        }

    @Test
    fun GameTestState.`remains conversion commits only after the rest of a Hunter catch fits`() =
        runFalconryTest { deps ->
            prepare()
            player.righthand = InvObj(FalconryObjs.falcon)
            val species = FalconrySpeciesList.all.first()
            val prey =
                spawnNpc(player.coords.translateX(1), deps.npcTypes[species.npc]) { noneMode() }
            random.next = 0
            player.withProtectedAccess { deps.falconry.launch(this, prey) }
            advance(6)
            val caught = deps.npcs.first { it.id == species.caught.id && it.isValidTarget() }
            var commits = 0
            deps.events.subscribeUnbound(RemainsDrop::class.java) { convert(1) { commits++ } }
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            player.withProtectedAccess { deps.falconry.retrieve(this, caught) }
            assertEquals(0, commits)
            assertTrue(caught.isValidTarget())
            player.inv[27] = null
            player.withProtectedAccess { deps.falconry.retrieve(this, caught) }
            assertEquals(1, commits)
            assertEquals(0, player.count(objs.bones))
            assertEquals(1, player.count(species.fur))
            assertFalse(caught.isValidTarget())
        }

    @Test
    fun GameTestState.openPouchesTakeTheDashingCatchAndBoneConversionWithNoInventorySlots() =
        runFalconryTest { d ->
            prepare()
            player.righthand = InvObj(FalconryObjs.falcon)
            val species = FalconrySpeciesList.all.last()
            val prey = spawnNpc(player.coords.translateX(1), d.npcTypes[species.npc]) { noneMode() }
            random.next = 0
            player.withProtectedAccess { d.falconry.launch(this, prey) }
            advance(6)
            val caught = d.npcs.first { it.id == species.caught.id && it.isValidTarget() }
            for (i in 0..27) player.inv[i] = InvObj(objs.knife)
            player.inv[0] = InvObj(objs.small_fur_pouch_open)
            player.inv[1] = InvObj(objs.small_meat_pouch_open)
            var commits = 0
            d.events.subscribeUnbound(RemainsDrop::class.java) { convert(1) { commits++ } }
            player.withProtectedAccess { d.falconry.retrieve(this, caught) }
            assertEquals(1, d.pouches.count(player, HunterPouchFamily.Fur))
            assertEquals(1, d.pouches.count(player, HunterPouchFamily.Meat))
            assertEquals(1, commits)
            assertFalse(caught.isValidTarget())
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchDashingKebbit))
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.hunter] = 99
        player.placeAt(CoordGrid(2376, 3584))
        allocZoneCollision(player.coords)
        allocZoneCollision(player.coords.translateX(8))
    }

    private fun GameTestState.runFalconryTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(FalconryScript::class),
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val falconry: Falconry,
        val pouches: HunterPouchStorage,
        val npcTypes: NpcTypeList,
        val npcs: NpcList,
        val events: EventBus,
    )
}
