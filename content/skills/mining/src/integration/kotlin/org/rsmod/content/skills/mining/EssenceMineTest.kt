package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class EssenceMineTest {
    @Test
    fun GameTestState
        .`an unbounded essence rock gives pure essence to eligible members and ordinary essence in free worlds`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(player.coords.translate(1, 0), MiningLocs.essenceRock)
            player.members = false
            player.stats[stats.mining] = 41
            player.inv[0] = InvObj(MiningObjs.runePickaxe)
            val xp = player.statMap.getFineXP(stats.mining)
            player.opLoc1(rock)
            advance(8)
            assertTrue(player.count(MiningObjs.runeEssence) >= 2)
            assertEquals(
                xp + 50 * player.count(MiningObjs.runeEssence),
                player.statMap.getFineXP(stats.mining),
            )
            player.members = true
            advance(3)
            assertTrue(player.count(MiningObjs.pureEssence) > 0)
        }

    @Test
    fun GameTestState
        .`Sedridor uses Rune Mysteries state and returns the miner from an instanced portal`() =
        runGameTest(EssenceMineTravel::class) {
            player.members = true
            val portal = locTypes.getValue(34825)
            val normalPortal = CoordGrid(2885, 4850)
            placeMapLoc(normalPortal, portal)
            player.teleport(CoordGrid(3104, 9571))
            val wizard = spawnNpc(CoordGrid(3103, 9571), npcTypes.getValue(5034))
            player.opNpc3(wizard)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SedridorEssenceMine))
            player.withProtectedAccess { vars[varps.runemysteries] = 6 }
            player.opNpc3(wizard)
            advance(2)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SedridorEssenceMine))
            assertNotEquals(CoordGrid(2911, 4832), player.coords)
            val copied = player.coords.translate(normalPortal.x - 2911, normalPortal.z - 4832)
            val exit = requireNotNull(findLoc(copied, portal))
            player.teleport(copied.translate(1, 0))
            player.opLoc1(BoundLocInfo(exit, portal))
            advance()
            assertEquals(CoordGrid(3104, 9571), player.coords)
        }
}
