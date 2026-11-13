package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.hat
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class KourendMineralsTest {
    @Test
    fun GameTestState.`sulphur requires worn filter and gives dated XP and native diary`() =
        runGameTest(Mining::class) {
            prepare(42)
            val rock = placeMapLoc(CoordGrid(1432, 3866), KourendMineralLocs.sulphur[2])
            player.telejump(rock.coords.translateZ(-1))
            player.opLoc1(rock)
            advance()
            assertMessageSent("You'll choke on the fumes if you try to mine without protection.")
            assertEquals(0, player.count(KourendMineralObjs.sulphur))
            player.hat = InvObj(KourendMineralObjs.faceMask)
            random.next = 128
            random.then = 0
            val xp = player.statMap.getFineXP(stats.mining)
            player.opLoc1(rock)
            advance(9)
            assertEquals(1, player.count(KourendMineralObjs.sulphur))
            assertEquals(350, player.statMap.getFineXP(stats.mining) - xp)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineVolcanicSulphur))
        }

    @Test
    fun GameTestState.`removing the filter while mining prevents sulphur output`() =
        runGameTest(Mining::class) {
            prepare(99)
            player.hat = InvObj(KourendMineralObjs.gasMask)
            val rock = placeMapLoc(CoordGrid(1439, 3869), KourendMineralLocs.sulphur[2])
            player.telejump(rock.coords.translateZ(-1))
            player.opLoc1(rock)
            advance()
            player.hat = null
            advance(9)
            assertEquals(0, player.count(KourendMineralObjs.sulphur))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineVolcanicSulphur))
        }

    @Test
    fun GameTestState.`lovakite normal ore extraction supplies smelting and diary`() =
        runGameTest(Mining::class) {
            prepare(65)
            val rock = placeMapLoc(CoordGrid(1434, 3840), KourendMineralLocs.lovakite[0])
            player.telejump(rock.coords.translateZ(-1))
            random.next = 128
            random.then = 0
            val xp = player.statMap.getFineXP(stats.mining)
            player.opLoc1(rock)
            advance(9)
            assertEquals(1, player.count(KourendMineralObjs.lovakite))
            assertEquals(600, player.statMap.getFineXP(stats.mining) - xp)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineLovakiteOre))
        }

    @Test
    fun GameTestState.`saltpetre requires spade and delivers double with no mining XP`() =
        runSaltTest { deps ->
            prepare(99)
            val rock = placeMapLoc(CoordGrid(1708, 3545), KourendMineralLocs.saltpetre[0])
            player.telejump(rock.coords.translateZ(-1))
            player.withProtectedAccess { deps.salt.dig(this, rock) }
            assertMessageSent("You need a spade to dig for saltpetre.")
            player.inv[0] = InvObj(objs.spade)
            random.next = 0
            val xp = player.statMap.getFineXP(stats.mining)
            player.withProtectedAccess { deps.salt.dig(this, rock) }
            advance(3)
            assertEquals(2, player.count(KourendMineralObjs.saltpetre))
            assertEquals(xp, player.statMap.getFineXP(stats.mining))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DigHosidiusSaltpetre))
        }

    @Test
    fun GameTestState.`saltpetre single remaining slot never overflows or loses carried goods`() =
        runSaltTest { deps ->
            prepare(99)
            val rock = placeMapLoc(CoordGrid(1716, 3518), KourendMineralLocs.saltpetre[1])
            player.telejump(rock.coords.translateZ(-1))
            player.inv[0] = InvObj(objs.spade)
            for (slot in 1..26) player.inv[slot] = InvObj(objs.small_fishing_net)
            random.next = 0
            player.withProtectedAccess { deps.salt.dig(this, rock) }
            advance(3)
            assertEquals(1, player.count(KourendMineralObjs.saltpetre))
            assertEquals(26, player.count(objs.small_fishing_net))
            assertTrue(player.inv.isFull())
        }

    @Test
    fun GameTestState.`Konoo gives a normal spade and removed spade cancels pending gathering`() =
        runSaltTest { deps ->
            prepare(99)
            player.withProtectedAccess { deps.salt.spade(this) }
            assertEquals(1, player.count(objs.spade))
            player.withProtectedAccess { deps.salt.spade(this) }
            assertEquals(1, player.count(objs.spade))
            val rock = placeMapLoc(CoordGrid(1687, 3512), KourendMineralLocs.saltpetre[2])
            player.telejump(rock.coords.translateZ(-1))
            player.withProtectedAccess { deps.salt.dig(this, rock) }
            player.clearAllInvs()
            advance(3)
            assertEquals(0, player.count(KourendMineralObjs.saltpetre))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DigHosidiusSaltpetre))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.stats[stats.mining] = level
        player.righthand = InvObj(MiningObjs.runePickaxe)
        for (dx in -8..8 step 8) for (dz in -8..8 step 8) allocZoneCollision(
            player.coords.translate(dx, dz)
        )
    }

    private fun GameTestState.runSaltTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            SaltpetreScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(val salt: SaltpetreGathering, val locs: LocRepository)
}
