package org.rsmod.content.skills.smithing.smelting

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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BluriteLimbSmithingTest {
    @Test
    fun GameTestState.`ordinary full-inventory smithing grants seventeen point five xp after five ticks`() = runBluriteTest { deps ->
        prepare()
        val anvil = anvil()
        player.fillInv()
        player.inv[0] = InvObj(objs.hammer)
        player.inv[1] = InvObj(objs.blurite_bar)
        val xp = player.statMap.getFineXP(stats.smithing)
        player.withProtectedAccess { deps.blurite.smith(this, anvil, 1) }
        advance(4)
        assertEquals(0, player.count(objs.blurite_limbs))
        advance()
        assertEquals(1, player.count(objs.blurite_limbs))
        assertEquals(175, player.statMap.getFineXP(stats.smithing) - xp)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.SmithDoricBluriteLimbs))
    }

    @Test
    fun GameTestState.`native Doric Smith accepts a two-bar batch and credits the easy Falador task`() = runBluriteTest { _ ->
        prepare()
        val anvil = placeMapLoc(CoordGrid(2000, 2000), AnvilLocs.doric)
        player.placeAt(anvil.coords.translateZ(1))
        player.inv[0] = InvObj(objs.hammer)
        player.inv[1] = InvObj(objs.blurite_bar)
        player.inv[2] = InvObj(objs.blurite_bar)
        assertEquals("Smith", locTypes[AnvilLocs.doric].op[0])
        player.opLoc1(anvil)
        advance()
        assertTrue(player.isAccessProtected, "Native Smith should open the blurite production menu.")
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a, 2))
        advance(10)
        assertEquals(2, player.count(objs.blurite_limbs))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.SmithDoricBluriteLimbs))
    }

    @Test
    fun GameTestState.`quest level tool membership and movement gates preserve blurite bars`() = runBluriteTest { deps ->
        prepare()
        val anvil = anvil()
        player.inv[0] = InvObj(objs.hammer)
        player.inv[1] = InvObj(objs.blurite_bar)
        player.setVarp(varps.squire, 6)
        player.withProtectedAccess { deps.blurite.smith(this, anvil, 1) }
        advance(5)
        assertEquals(1, player.count(objs.blurite_bar))
        player.setVarp(varps.squire, 7)
        player.setBaseLevel(stats.smithing, 12)
        player.setCurrentLevel(stats.smithing, 13)
        player.withProtectedAccess { deps.blurite.smith(this, anvil, 1) }
        player.inv[0] = null
        advance(5)
        assertEquals(1, player.count(objs.blurite_bar))
        player.inv[0] = InvObj(objs.hammer)
        player.members = false
        player.withProtectedAccess { deps.blurite.smith(this, anvil, 1) }
        advance(5)
        assertEquals(1, player.count(objs.blurite_bar))
        player.members = true
        player.withProtectedAccess { deps.blurite.smith(this, anvil, 1) }
        player.placeAt(player.coords.translateX(10))
        advance(5)
        assertEquals(1, player.count(objs.blurite_bar))
    }

    private fun GameTestScope.prepare() { player.members = true; player.stats[stats.smithing] = 13; player.setVarp(varps.squire, 7); player.setVarp(AnvilQuestVars.doric, 100) }
    private fun GameTestScope.anvil() = placeMapLoc(CoordGrid(3200, 3200), AnvilLocs.ordinary.first()).also { player.placeAt(it.coords.translateX(-1)) }
    private fun GameTestState.runBluriteTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(AnvilSmithingScript::class), testBody = body)
    data class Dependencies @Inject constructor(val blurite: BluriteLimbSmithing)
}
