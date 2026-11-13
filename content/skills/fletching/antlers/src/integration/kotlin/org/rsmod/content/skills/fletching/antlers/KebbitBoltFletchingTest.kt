package org.rsmod.content.skills.fletching.antlers

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class KebbitBoltFletchingTest {
    @Test
    fun GameTestState.nativeChiselUseMakesTwelveKebbitBoltsAfterThreeTicks() = nativeRecipeTest(0)

    @Test fun GameTestState.nativeChiselUseMakesTwelveLongBoltsAfterTwoTicks() = nativeRecipeTest(1)

    private fun GameTestState.nativeRecipeTest(index: Int) = game { d ->
        prepare()
        val recipe = KebbitBoltRecipes.all[index]
        player.inv[0] = InvObj(objs.chisel)
        player.inv[1] = InvObj(recipe.antler)
        val before = player.statMap.getFineXP(stats.fletching)
        player.withProtectedAccess {
            d.use.interact(this, inv, d.types[recipe.antler], 1, d.types[objs.chisel], 0)
        }
        player.ui.queueClose(requireNotNull(player.ui.getModalOrNull(components.chatbox_chatmodal)))
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a, 1))
        advance(recipe.ticks - 1)
        assertEquals(0, player.count(recipe.output))
        advance()
        assertEquals(12, player.count(recipe.output))
        assertEquals(0, player.count(recipe.antler))
        assertEquals(if (index == 0) 58 else 79, player.statMap.getFineXP(stats.fletching) - before)
    }

    @Test
    fun GameTestState.spikeBatchStopsAtTheRequestedQuantity() = game { d ->
        prepare()
        val recipe = KebbitBoltRecipes.all[0]
        player.inv[0] = InvObj(objs.chisel)
        repeat(3) { player.inv[it + 1] = InvObj(recipe.antler) }
        player.withProtectedAccess { d.cut.cut(this, recipe, 2) }
        advance(6)
        assertEquals(24, player.count(recipe.output))
        assertEquals(1, player.count(recipe.antler))
    }

    @Test
    fun GameTestState.boostedRequirementsAndToolLossAreRecheckedBeforeConsumption() = game { d ->
        prepare()
        val recipe = KebbitBoltRecipes.all[1]
        player.stats[stats.fletching] = 41
        player.inv[0] = InvObj(objs.chisel)
        player.inv[1] = InvObj(recipe.antler)
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        advance(3)
        assertEquals(1, player.count(recipe.antler))
        player.setCurrentLevel(stats.fletching, 42)
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        player.inv[0] = null
        advance(2)
        assertEquals(1, player.count(recipe.antler))
        player.inv[0] = InvObj(objs.chisel)
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        advance(2)
        assertEquals(12, player.count(recipe.output))
    }

    @Test
    fun GameTestState.fullInventoryUsesTheConsumedSpikeSlotAndNonmembersPreserveIt() = game { d ->
        prepare()
        val recipe = KebbitBoltRecipes.all[0]
        player.fillInv()
        player.inv[0] = InvObj(objs.chisel)
        player.inv[1] = InvObj(recipe.antler)
        player.members = false
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        advance(3)
        assertEquals(1, player.count(recipe.antler))
        player.members = true
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        advance(3)
        assertEquals(12, player.count(recipe.output))
        assertEquals(0, player.inv.freeSpace())
    }

    @Test
    fun GameTestState.walkingCancelsTheProductionWaitWithoutConsumingASpike() = game { d ->
        prepare()
        val recipe = KebbitBoltRecipes.all[0]
        player.inv[0] = InvObj(objs.chisel)
        player.inv[1] = InvObj(recipe.antler)
        val before = player.statMap.getFineXP(stats.fletching)
        player.withProtectedAccess { d.cut.cut(this, recipe, 1) }
        player.moveGameClick(CoordGrid(2002, 2000))
        advance(5)
        assertEquals(CoordGrid(2002, 2000), player.coords)
        assertEquals(1, player.count(recipe.antler))
        assertEquals(0, player.count(recipe.output))
        assertEquals(before, player.statMap.getFineXP(stats.fletching))
    }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.fletching] = 99
        player.telejump(CoordGrid(2000, 2000))
        allocZoneCollision(player.coords)
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, AntlerFletchingScript::class, testBody = body)

    data class Deps
    @Inject
    constructor(val cut: AntlerFletching, val use: HeldUInteractions, val types: ObjTypeList)
}
