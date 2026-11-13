package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.feet
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MasterFarmerTest {
    @Test
    fun GameTestState.`native Martin and north Ardougne farmers supply seeds and award distinct diaries`() = runMasterTest { deps ->
        prepare()
        for ((type, coords, task) in listOf(Triple(MasterFarmerNpcs.martin, CoordGrid(3078,3258), DiaryTask.PickpocketMartin), Triple(MasterFarmerNpcs.ordinary[1], CoordGrid(2636,3364), DiaryTask.PickpocketArdougneMasterFarmer))) {
            player.placeAt(coords.translateX(-1)); allocZoneCollision(coords)
            val npc = spawnNpc(coords, deps.npcs[type]) { noneMode() }
            assertEquals("Pickpocket", deps.npcs[type].op[2])
            random.next = 0; random.then = 0; random.then = 0; random.then = 4
            player.actionDelay = -1
            val before = player.statMap.getFineXP(stats.thieving)
            player.opNpc3(npc); advance()
            assertEquals(430, player.statMap.getFineXP(stats.thieving) - before)
            assertTrue(player.hasCompletedDiaryTask(task))
        }
        assertEquals(8, player.count(MasterFarmerObjs.potato_seed))
    }

    @Test
    fun GameTestState.`Farming herb redistribution uses base level caps at eighty five and preserves all rare seeds`() = runMasterTest { _ ->
        prepare()
        val rolls = random
        assertEquals(45, MasterFarmerSeeds.all.map { it.obj.id }.toSet().size)
        for ((level, special) in listOf(1 to 7, 75 to 81, 85 to 91, 99 to 91)) {
            player.stats[stats.farming] = level
            player.setCurrentLevel(stats.farming, 120)
            assertEquals(special, MasterFarmerSeeds.valuableSlots(level))
            player.withProtectedAccess {
                rolls.next = special - 1; rolls.then = 0
                assertEquals(MasterFarmerObjs.ranarr_seed, MasterFarmerSeeds.herb(this).obj)
                rolls.next = special
                assertEquals(MasterFarmerObjs.guam_seed, MasterFarmerSeeds.herb(this).obj)
                rolls.next = 400
                assertEquals(MasterFarmerObjs.guam_seed, MasterFarmerSeeds.herb(this).obj)
                rolls.next = 401
                assertEquals(MasterFarmerObjs.marrentill_seed, MasterFarmerSeeds.herb(this).obj)
                rolls.next = 999
                assertEquals(MasterFarmerObjs.dwarf_weed_seed, MasterFarmerSeeds.herb(this).obj)
            }
        }
        assertEquals(listOf(7,8,5,6,5), listOf(MasterFarmerSeeds.hops, MasterFarmerSeeds.allotments, MasterFarmerSeeds.flowers, MasterFarmerSeeds.bushes, MasterFarmerSeeds.special).map { it.size })
        assertTrue(MasterFarmerSeeds.special.any { it.obj == MasterFarmerObjs.seaweed_seed })
    }

    @Test
    fun GameTestState.`seed pickpocket requires a free slot ignores coin pouch limit and doubles with rogue outfit`() = runMasterTest { deps ->
        prepare()
        val npc = spawnNpc(player.coords.translateX(1), deps.npcs[MasterFarmerNpcs.martin]) { noneMode() }
        player.fillInv(); player.inv[0] = InvObj(MasterFarmerObjs.potato_seed, 100)
        player.withProtectedAccess { deps.pickpocketing.attempt(this, npc, MasterFarmerSeeds.definition) }
        assertEquals(100, player.count(MasterFarmerObjs.potato_seed))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.PickpocketMartin))
        player.clearInv()
        player.inv[0] = InvObj(ThievingObjs.citizenPouch, 28)
        player.hat = InvObj(ThievingObjs.rogueMask); player.torso = InvObj(ThievingObjs.rogueTop); player.legs = InvObj(ThievingObjs.rogueTrousers); player.hands = InvObj(ThievingObjs.rogueGloves); player.feet = InvObj(ThievingObjs.rogueBoots)
        random.next = 0; random.then = 0; random.then = 0; random.then = 4
        player.withProtectedAccess { deps.pickpocketing.attempt(this, npc, MasterFarmerSeeds.definition) }
        assertEquals(8, player.count(MasterFarmerObjs.potato_seed))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.PickpocketMartin))
    }

    @Test
    fun GameTestState.`failure applies three damage and level gate gives no seeds or diary credit`() = runMasterTest { deps ->
        prepare()
        val npc = spawnNpc(player.coords.translateX(1), deps.npcs[MasterFarmerNpcs.martin]) { noneMode() }
        player.stats[stats.thieving] = 37
        player.withProtectedAccess { deps.pickpocketing.attempt(this, npc, MasterFarmerSeeds.definition) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.PickpocketMartin))
        player.stats[stats.thieving] = 38
        random.next = 255
        player.withProtectedAccess { deps.pickpocketing.attempt(this, npc, MasterFarmerSeeds.definition) }
        advance(3)
        assertEquals(7, player.stats[stats.hitpoints])
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.PickpocketMartin))
    }

    @Test
    fun GameTestState.`existing hero guard gnome and elf rewards complete their eligible diary rows`() = runMasterTest { deps ->
        prepare()
        val rows = listOf(Triple(StandardPickpockets.hero, CoordGrid(2655,3300), DiaryTask.PickpocketArdougneHero), Triple(StandardPickpockets.guard, CoordGrid(2965,3380), DiaryTask.PickpocketFaladorGuard), Triple(StandardPickpockets.gnome, CoordGrid(2465,3480), DiaryTask.PickpocketGnome), Triple(StandardPickpockets.lletyaElf, CoordGrid(2340,3170), DiaryTask.PickpocketElf))
        for ((definition, coords, task) in rows) {
            player.clearInv(); player.actionDelay = -1
            player.placeAt(coords); allocZoneCollision(coords)
            val npc = spawnNpc(coords.translateX(1), deps.npcs[definition.npcs.first()]) { noneMode() }
            random.next = 0; random.then = 0; random.then = 2; random.then = 2
            player.withProtectedAccess { deps.pickpocketing.attempt(this, npc, definition) }
            assertTrue(player.hasCompletedDiaryTask(task), task.name)
        }
    }
    private fun GameTestScope.prepare() { player.members = true; player.stats[stats.thieving] = 99; player.stats[stats.hitpoints] = 10; player.placeAt(CoordGrid(3077,3258)); allocZoneCollision(player.coords) }
    private fun GameTestState.runMasterTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(PickpocketScript::class, MasterFarmerScript::class, PlayerHitScript::class), testBody = body)
    data class Dependencies @Inject constructor(val npcs: NpcTypeList, val pickpocketing: Pickpocketing)
}
