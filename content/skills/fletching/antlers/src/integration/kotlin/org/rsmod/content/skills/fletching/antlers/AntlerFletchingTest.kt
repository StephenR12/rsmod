package org.rsmod.content.skills.fletching.antlers

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
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

@Execution(ExecutionMode.SAME_THREAD)
class AntlerFletchingTest {
    @Test
    fun GameTestState.`cache pins both antlers ammunition and hunters crossbow identities`() =
        runAntlerTest { _ ->
            assertEquals(28884, AntlerObjs.sunlight.id)
            assertEquals(28887, AntlerObjs.moonlight.id)
            assertEquals(28872, AntlerObjs.sunlightBolts.id)
            assertEquals(28878, AntlerObjs.moonlightBolts.id)
            assertEquals(10156, AntlerObjs.crossbow.id)
            assertEquals(28869, AntlerObjs.sunlightCrossbow.id)
        }

    @Test
    fun GameTestState.`each antler produces twelve bolts after three ticks with exact xp in a full inventory`() =
        runAntlerTest { deps ->
            prepare()
            for (recipe in AntlerBoltRecipes.all) {
                player.fillInv()
                player.inv[0] = InvObj(objs.chisel)
                player.inv[1] = InvObj(recipe.antler)
                val xp = player.statMap.getFineXP(stats.fletching)
                player.withProtectedAccess { deps.fletching.cut(this, recipe, 1) }
                advance(2)
                assertEquals(0, player.count(recipe.output))
                advance()
                assertEquals(12, player.count(recipe.output))
                assertEquals(0, player.count(recipe.antler))
                assertEquals((recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.fletching) - xp)
            }
        }

    @Test
    fun GameTestState.`sunlight crossbow upgrade is synchronous zero xp and does not require wield levels`() =
        runAntlerTest { deps ->
            prepare()
            player.stats[stats.hunter] = 1
            player.stats[stats.ranged] = 1
            player.fillInv()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(AntlerObjs.crossbow)
            player.inv[2] = InvObj(AntlerObjs.sunlight)
            val xp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess {
                deps.heldU.interact(this, player.inv, deps.objs[AntlerObjs.sunlight], 2, deps.objs[AntlerObjs.crossbow], 1)
            }
            assertEquals(1, player.count(AntlerObjs.sunlightCrossbow))
            assertEquals(0, player.count(AntlerObjs.crossbow))
            assertEquals(0, player.count(AntlerObjs.sunlight))
            assertEquals(xp, player.statMap.getFineXP(stats.fletching))
        }

    @Test
    fun GameTestState.`native reverse chisel use accepts a two-antler batch`() =
        runAntlerTest { deps ->
            prepare()
            val recipe = AntlerBoltRecipes.all.first()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(recipe.antler)
            player.inv[2] = InvObj(recipe.antler)
            player.withProtectedAccess {
                deps.heldU.interact(this, player.inv, deps.objs[recipe.antler], 1, deps.objs[objs.chisel], 0)
            }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a, 2))
            advance(6)
            assertEquals(24, player.count(recipe.output))
            assertEquals(0, player.count(recipe.antler))
        }

    @Test
    fun GameTestState.`boosted levels are accepted but tool loss and membership reject without consuming antlers`() =
        runAntlerTest { deps ->
            prepare()
            val recipe = AntlerBoltRecipes.all.last()
            player.setBaseLevel(stats.fletching, 71)
            player.setCurrentLevel(stats.fletching, 72)
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(recipe.antler)
            player.withProtectedAccess { deps.fletching.cut(this, recipe, 1) }
            player.inv[0] = null
            advance(3)
            assertEquals(1, player.count(recipe.antler))
            player.inv[0] = InvObj(objs.chisel)
            player.members = false
            player.withProtectedAccess { deps.fletching.cut(this, recipe, 1) }
            assertEquals(1, player.count(recipe.antler))
            player.members = true
            player.withProtectedAccess { deps.fletching.cut(this, recipe, 1) }
            advance(3)
            assertEquals(12, player.count(recipe.output))
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.fletching] = 74
    }

    private fun GameTestState.runAntlerTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(AntlerFletchingScript::class), testBody = body)

    data class Dependencies @Inject constructor(val fletching: AntlerFletching, val heldU: HeldUInteractions, val objs: ObjTypeList)
}
