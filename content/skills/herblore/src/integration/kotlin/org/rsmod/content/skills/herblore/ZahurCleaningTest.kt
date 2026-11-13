package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ZahurCleaningTest {
    @Test
    fun GameTestState.`Zahur cleans all fifteen ordinary herbs for two hundred coins without level quest or xp gates`() = runZahurTest { deps ->
        player.members = true
        for (recipe in HerbCleaningRecipes.all) {
            player.clearInv()
            player.inv[0] = InvObj(recipe.grimy)
            player.inv[1] = InvObj(objs.coins, 200)
            val xp = player.statMap.getFineXP(stats.herblore)
            player.withProtectedAccess { assertEquals(1, deps.cleaning.clean(this)) }
            assertEquals(1, player.count(recipe.clean))
            assertEquals(0, player.count(recipe.grimy))
            assertEquals(0, player.count(objs.coins))
            assertEquals(xp, player.statMap.getFineXP(stats.herblore))
        }
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CleanWithZahur))
    }

    @Test
    fun GameTestState.`noted cleaning requires hard diary and only consumes affordable units`() = runZahurTest { deps ->
        player.members = true
        val raw = deps.objs.getValue(deps.objs[objs.grimy_torstol].certlink)
        val clean = deps.objs.getValue(deps.objs[objs.torstol].certlink)
        player.inv[0] = InvObj(raw, 10)
        player.inv[1] = InvObj(objs.coins, 700)
        player.withProtectedAccess { assertEquals(0, deps.cleaning.clean(this)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CleanWithZahur))
        player.setVarBit(DiaryRegion.Desert.tier(DiaryDifficulty.Hard).complete, 1)
        player.withProtectedAccess { assertEquals(3, deps.cleaning.clean(this)) }
        assertEquals(7, player.count(raw))
        assertEquals(3, player.count(clean))
        assertEquals(100, player.count(objs.coins))
    }

    @Test
    fun GameTestState.`native Clean operation awards Desert task only after a paid result`() = runZahurTest { deps ->
        player.members = true
        player.placeAt(CoordGrid(3200, 3200))
        allocZoneCollision(player.coords)
        assertEquals("Clean", deps.npcs[ZahurNpcs.zahur].op[3])
        val zahur = spawnNpc(player.coords.translateX(1), deps.npcs[ZahurNpcs.zahur]) { noneMode() }
        player.inv[0] = InvObj(objs.grimy_guam_leaf)
        player.opNpc4(zahur)
        advance(2)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.CleanWithZahur))
        player.inv[1] = InvObj(objs.coins, 200)
        player.opNpc4(zahur)
        advance(2)
        assertEquals(1, player.count(objs.guam_leaf))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.CleanWithZahur))
    }

    private fun GameTestState.runZahurTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(ZahurScript::class), testBody = body)
    data class Dependencies @Inject constructor(val cleaning: ZahurCleaning, val objs: ObjTypeList, val npcs: NpcTypeList)
}
