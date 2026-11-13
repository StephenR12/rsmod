package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.legs
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class AerialFishingTest {
    @Test
    fun GameTestState.`dated species rolls thresholds requirements and uncapped boost formula are pinned`() = runAerialTest { _ ->
        assertEquals(listOf(22826, 22829, 22832, 22835), AerialFishTable.all.map { it.obj.id })
        assertEquals(listOf(52, 15, 15, 17), AerialFishTable.all.map { fish -> (0 until 99).count { AerialFishTable.select(99, 99, it) == fish } })
        assertEquals(110, AerialFishTable.bound(114, 102))
        assertEquals(71, AerialFishTable.pearlDenominator(114, 102))
        assertEquals(100, AerialFishTable.pearlDenominator(43, 35))
        assertEquals(75, AerialFishTable.pearlDenominator(99, 99))
        assertEquals(AerialFishTable.all[2], AerialFishTable.select(91, 86, 88))
        assertEquals(AerialFishTable.all[0], AerialFishTable.select(1, 1, 0))
    }

    @Test
    fun GameTestState.`native Get bird loan supports boosted access and bluegill after levels drain`() = runAerialTest { deps ->
        prepare()
        player.setBaseLevel(stats.fishing, 42)
        player.setCurrentLevel(stats.fishing, 43)
        player.setBaseLevel(stats.hunter, 34)
        player.setCurrentLevel(stats.hunter, 35)
        assertEquals("Get bird", deps.npcs[AerialNpcs.alry].op[3])
        val alry = spawnNpc(player.coords.translateX(1), deps.npcs[AerialNpcs.alry]) { noneMode() }
        player.opNpc4(alry)
        advance(2)
        assertEquals(AerialObjs.bird.id, player.righthand?.id)
        player.setCurrentLevel(stats.fishing, 1)
        player.setCurrentLevel(stats.hunter, 1)
        player.inv[0] = InvObj(AerialObjs.chunks, 1)
        val pool = pool(deps)
        rollCatch(0)
        player.withProtectedAccess { deps.fishing.launch(this, pool) }
        advance(2)
        assertEquals(1, player.count(objs.raw_bluegill))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchLakeMolchBluegill))
    }

    @Test
    fun GameTestState.`all four catches award exact Fishing and Hunter xp with adjacent one tick returns`() = runAerialTest { deps ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        val pool = pool(deps)
        for (fish in AerialFishTable.all) {
            player.clearInv()
            player.inv[0] = InvObj(AerialObjs.worm)
            val fishingXp = player.statMap.getFineXP(stats.fishing)
            val hunterXp = player.statMap.getFineXP(stats.hunter)
            rollCatch(fish.threshold)
            player.withProtectedAccess { deps.fishing.launch(this, pool) }
            assertEquals(AerialObjs.glove.id, player.righthand?.id)
            advance(2)
            assertEquals(1, player.count(fish.obj))
            assertEquals(0, player.count(AerialObjs.worm))
            assertEquals((fish.fishingXp * 10).toInt(), player.statMap.getFineXP(stats.fishing) - fishingXp)
            assertEquals((fish.hunterXp * 10).toInt(), player.statMap.getFineXP(stats.hunter) - hunterXp)
        }
    }

    @Test
    fun GameTestState.`fish chunk is preserved twice then can last until guaranteed fourth consumption`() = runAerialTest { deps ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        player.inv[0] = InvObj(AerialObjs.chunks, 2)
        val pool = pool(deps)
        repeat(4) { index ->
            rollCatch(0)
            if (index == 2) random.then = 1
            player.withProtectedAccess { deps.fishing.launch(this, pool) }
            advance(2)
            assertEquals(if (index < 3) 2 else 1, player.count(AerialObjs.chunks))
        }
        assertEquals(0, player.vars[AerialVars.chunkCatches])
        assertEquals(4, player.count(objs.raw_bluegill))
    }

    @Test
    fun GameTestState.`pool relocation during flight preserves catch while full inventory on return discards it`() = runAerialTest { deps ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        player.inv[0] = InvObj(AerialObjs.chunks, 2)
        val pool = pool(deps)
        rollCatch(0)
        player.withProtectedAccess { deps.fishing.launch(this, pool) }
        pool.coords = pool.coords.translateX(5)
        advance(2)
        assertEquals(1, player.count(objs.raw_bluegill))
        pool.coords = player.coords.translateX(1)
        player.withProtectedAccess { deps.fishing.launch(this, pool) }
        player.fillInv()
        player.inv[0] = InvObj(AerialObjs.chunks, 2)
        val xp = player.statMap.getFineXP(stats.fishing)
        advance(2)
        assertEquals(xp, player.statMap.getFineXP(stats.fishing))
        assertEquals(2, player.count(AerialObjs.chunks))
        assertEquals(AerialObjs.bird.id, player.righthand?.id)
    }

    @Test
    fun GameTestState.`knife processing is instantaneous for every fish and rewards exact Cooking xp`() = runAerialTest { deps ->
        prepare()
        player.inv[0] = InvObj(objs.knife)
        val start = mapClock.cycle
        for (fish in AerialFishTable.all) {
            player.inv[2] = InvObj(fish.obj)
            val xp = player.statMap.getFineXP(stats.cooking)
            player.withProtectedAccess { deps.heldU.interact(this, player.inv, deps.objs[fish.obj], 2, deps.objs[objs.knife], 0) }
            assertEquals((fish.cookingXp * 10).toInt(), player.statMap.getFineXP(stats.cooking) - xp)
        }
        assertEquals(4, player.count(AerialObjs.chunks))
        assertEquals(start, mapClock.cycle)
    }

    @Test
    fun GameTestState.`golden tench and pearl rolls feed exchange and fixed-price rewards with training gates`() = runAerialTest { deps ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        player.inv[0] = InvObj(AerialObjs.chunks, 10)
        val pool = pool(deps)
        random.next = 0
        random.then = 0
        random.then = 0
        player.withProtectedAccess { deps.fishing.launch(this, pool) }
        advance(2)
        assertEquals(1, player.count(AerialObjs.goldenTench))
        assertEquals(1, player.count(AerialObjs.pearl))
        player.withProtectedAccess { assertTrue(deps.fishing.exchangeTench(this)); assertTrue(deps.fishing.buy(this, AerialRewards.outfit.first())) }
        assertEquals(1, player.count(AerialObjs.pearl))
        assertEquals(1, player.count(FishingOutfits.hat))
        player.clearInv()
        player.inv[0] = InvObj(AerialObjs.pearl, 1000)
        player.withProtectedAccess { assertFalse(deps.fishing.buy(this, AerialRewards.rods.last())) }
        player.setVarBit(varbits.brut_fishing_r, 1)
        player.withProtectedAccess { assertTrue(deps.fishing.buy(this, AerialRewards.rods.last())) }
        assertEquals(850, player.count(AerialObjs.pearl))
        assertEquals(1, player.count(objs.pearl_barbarian_rod))
    }

    @Test
    fun GameTestState.`mixed Angler and Spirit pieces grant exact full-set Fishing xp without Hunter bonus`() = runAerialTest { deps ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        player.hat = InvObj(FishingOutfits.spiritHat)
        player.torso = InvObj(FishingOutfits.top)
        player.legs = InvObj(FishingOutfits.spiritLegs)
        player.feet = InvObj(FishingOutfits.boots)
        assertEquals(1.025, FishingXp.multiplier(player), 0.000001)
        player.inv[0] = InvObj(AerialObjs.worm)
        val fishingXp = player.statMap.getFineXP(stats.fishing)
        val hunterXp = player.statMap.getFineXP(stats.hunter)
        val pool = pool(deps)
        rollCatch(0)
        player.withProtectedAccess { deps.fishing.launch(this, pool) }
        advance(2)
        assertEquals((11.5 * 1.025 * 10).toInt(), player.statMap.getFineXP(stats.fishing) - fishingXp)
        assertEquals(165, player.statMap.getFineXP(stats.hunter) - hunterXp)
    }

    @Test
    fun GameTestState.`Boaty provides natural island exit and removes borrowed gear`() = runAerialTest { _ ->
        prepare()
        player.righthand = InvObj(AerialObjs.bird)
        val boat = placeMapLoc(CoordGrid(1368, 3640), AerialLocs.boat)
        player.placeAt(CoordGrid(1368, 3639))
        allocZoneCollision(CoordGrid(1342, 3645))
        player.opLoc1(boat)
        advance(4)
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 2))
        assertEquals(CoordGrid(1342, 3645), player.coords)
        assertEquals(null, player.righthand)
    }

    private fun GameTestScope.prepare() { player.members = true; player.stats[stats.fishing] = 99; player.stats[stats.hunter] = 99; player.placeAt(CoordGrid(1368, 3634)); allocZoneCollision(player.coords); allocZoneCollision(player.coords.translateX(8)) }
    private fun GameTestScope.pool(deps: Dependencies): Npc = spawnNpc(player.coords.translateX(1), deps.npcs[AerialNpcs.pool]) { noneMode(); aiTimer = Int.MAX_VALUE }
    private fun GameTestScope.rollCatch(roll: Int) { random.next = roll; random.then = 1; random.then = 1 }
    private fun GameTestState.runAerialTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(AerialFishingScript::class), testBody = body)
    data class Dependencies @Inject constructor(val fishing: AerialFishing, val npcs: NpcTypeList, val objs: ObjTypeList, val heldU: HeldUInteractions)
}
