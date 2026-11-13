package org.rsmod.content.skills.hunter.moths

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class LegacyButterflyTest {
    @Test
    fun GameTestState.`all four native catches consume one optional jar and award dated exact xp`() = runButterflyTest { deps ->
        prepare()
        assertEquals(listOf(5556,5555,5554,5553), LegacyButterflyTable.all.map { it.npc.id })
        for (species in LegacyButterflyTable.all) {
            player.clearInv()
            player.inv[0] = InvObj(MothObjs.emptyJar)
            player.righthand = InvObj(MothObjs.butterflyNet)
            val npc = spawnNpc(player.coords.translateX(1), deps.npcs[species.npc]) { noneMode() }
            assertEquals("Catch", deps.npcs[species.npc].op[0])
            val xp = player.statMap.getFineXP(stats.hunter)
            random.next = 0
            player.opNpc1(npc)
            advance(2)
            assertFalse(npc.isValidTarget())
            assertEquals(1, player.count(species.jar))
            assertEquals(0, player.count(MothObjs.emptyJar))
            assertEquals((species.xp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - xp)
        }
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchSnowyKnight))
    }

    @Test
    fun GameTestState.`barehanded requires ten extra Hunter levels and releases without consuming a jar`() = runButterflyTest { deps ->
        prepare()
        val species = LegacyButterflyTable.all.first()
        player.stats[stats.hunter] = 24
        player.stats[stats.attack] = 50
        player.inv[0] = InvObj(MothObjs.emptyJar)
        val npc = spawnNpc(player.coords.translateX(1), deps.npcs[species.npc]) { noneMode() }
        player.withProtectedAccess { deps.hunting.catch(this, npc, species) }
        assertTrue(npc.isValidTarget())
        player.stats[stats.hunter] = 25
        random.next = 0
        player.withProtectedAccess { deps.hunting.catch(this, npc, species) }
        advance()
        assertFalse(npc.isValidTarget())
        assertEquals(1, player.count(MothObjs.emptyJar))
        assertEquals(0, player.count(species.jar))
        assertEquals(61, player.stats[stats.attack])
    }

    @Test
    fun GameTestState.`jar effects restore exact stats and share the normal potion delay`() = runButterflyTest { deps ->
        prepare()
        for ((species, stat) in listOf(LegacyButterflyTable.all[0] to stats.attack, LegacyButterflyTable.all[1] to stats.defence, LegacyButterflyTable.all[3] to stats.strength)) {
            deps.potions.clear(player)
            player.clearInv()
            player.stats[stat] = 50
            player.inv[0] = InvObj(species.jar)
            player.withProtectedAccess { deps.hunting.release(this, 0, species) }
            assertEquals(61, player.stats[stat])
            assertEquals(1, player.count(MothObjs.emptyJar))
            assertEquals(3, deps.potions.remainingCycles(player))
        }
        val snowy = LegacyButterflyTable.all[2]
        player.stats[stats.hitpoints] = 50
        player.setCurrentLevel(stats.hitpoints, 20)
        player.inv[0] = InvObj(snowy.jar)
        player.withProtectedAccess { deps.hunting.release(this, 0, snowy) }
        assertEquals(1, player.count(snowy.jar))
        advance(3)
        player.withProtectedAccess { deps.hunting.release(this, 0, snowy) }
        assertEquals(35, player.stats[stats.hitpoints])
        assertEquals(1, player.count(MothObjs.emptyJar))
    }

    @Test
    fun GameTestState.`level membership and failed approximate rolls preserve the creature and jar`() = runButterflyTest { deps ->
        prepare()
        val species = LegacyButterflyTable.all[2]
        player.righthand = InvObj(MothObjs.magicButterflyNet)
        player.inv[0] = InvObj(MothObjs.emptyJar)
        val npc = spawnNpc(player.coords.translateX(1), deps.npcs[species.npc]) { noneMode() }
        player.stats[stats.hunter] = 34
        player.withProtectedAccess { deps.hunting.catch(this, npc, species) }
        assertTrue(npc.isValidTarget())
        player.stats[stats.hunter] = 35
        player.members = false
        player.withProtectedAccess { deps.hunting.catch(this, npc, species) }
        assertTrue(npc.isValidTarget())
        player.members = true
        random.next = 99
        player.withProtectedAccess { deps.hunting.catch(this, npc, species) }
        advance()
        assertTrue(npc.isValidTarget())
        assertEquals(1, player.count(MothObjs.emptyJar))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchSnowyKnight))
    }
    private fun GameTestScope.prepare() { player.members = true; player.stats[stats.hunter] = 99; player.placeAt(CoordGrid(2700,3800)); allocZoneCollision(player.coords) }
    private fun GameTestState.runButterflyTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(LegacyButterflyScript::class), testBody = body)
    data class Dependencies @Inject constructor(val hunting: LegacyButterflyHunting, val npcs: NpcTypeList, val potions: PotionConsumptionRepository)
}
