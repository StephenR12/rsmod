package org.rsmod.content.skills.crafting.snelms

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
class SnelmCraftingTest {
    @Test
    fun GameTestState.`cache pins all nine shell and snelm identities and cutting animation`() =
        runSnelmTest { _ ->
            assertEquals(listOf(3345, 3347, 3349, 3351, 3353, 3355, 3357, 3359, 3361), SnelmRecipes.all.map { it.shell.id })
            assertEquals(listOf(3327, 3329, 3331, 3333, 3335, 3337, 3339, 3341, 3343), SnelmRecipes.all.map { it.output.id })
            assertEquals(1280, SnelmSequences.cut.id)
        }

    @Test
    fun GameTestState.`every shell becomes its own snelm in two ticks and awards thirty two point five xp`() =
        runSnelmTest { deps ->
            prepare()
            for (recipe in SnelmRecipes.all) {
                player.fillInv()
                player.inv[0] = InvObj(objs.chisel)
                player.inv[1] = InvObj(recipe.shell)
                val xp = player.statMap.getFineXP(stats.crafting)
                player.withProtectedAccess { deps.crafting.craft(this, recipe, 1) }
                advance()
                assertEquals(1, player.count(recipe.shell))
                advance()
                assertEquals(0, player.count(recipe.shell))
                assertEquals(1, player.count(recipe.output))
                assertEquals(325, player.statMap.getFineXP(stats.crafting) - xp)
            }
        }

    @Test
    fun GameTestState.`boosted level is accepted and chisel loss interrupts before consumption`() =
        runSnelmTest { deps ->
            prepare()
            player.setBaseLevel(stats.crafting, 14)
            player.setCurrentLevel(stats.crafting, 15)
            val recipe = SnelmRecipes.all.first()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(recipe.shell)
            player.withProtectedAccess { deps.crafting.craft(this, recipe, 1) }
            player.inv[0] = null
            advance(2)
            assertEquals(1, player.count(recipe.shell))
            assertEquals(0, player.count(recipe.output))
            player.inv[0] = InvObj(objs.chisel)
            player.withProtectedAccess { deps.crafting.craft(this, recipe, 1) }
            advance(2)
            assertEquals(1, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`native reverse chisel use opens the quantity menu and makes the selected batch`() =
        runSnelmTest { deps ->
            prepare()
            val recipe = SnelmRecipes.all.last()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(recipe.shell)
            player.inv[2] = InvObj(recipe.shell)
            player.withProtectedAccess {
                deps.heldU.interact(this, player.inv, deps.objs[recipe.shell], 1, deps.objs[objs.chisel], 0)
            }
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.skillmulti_a, 2))
            advance(4)
            assertEquals(2, player.count(recipe.output))
            assertEquals(0, player.count(recipe.shell))
        }

    @Test
    fun GameTestState.`members and level gates leave shells intact`() =
        runSnelmTest { deps ->
            val recipe = SnelmRecipes.all.first()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(recipe.shell)
            player.members = false
            player.stats[stats.crafting] = 15
            player.withProtectedAccess { deps.crafting.craft(this, recipe, 1) }
            assertEquals(1, player.count(recipe.shell))
            player.members = true
            player.stats[stats.crafting] = 14
            player.withProtectedAccess { deps.crafting.craft(this, recipe, 1) }
            assertEquals(1, player.count(recipe.shell))
            assertMessageSent("You need a Crafting level of 15 to make snelms.")
        }

    private fun GameTestScope.prepare() {
        player.members = true
        player.stats[stats.crafting] = 15
    }

    private fun GameTestState.runSnelmTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(SnelmCraftingScript::class), testBody = body)

    data class Dependencies @Inject constructor(val crafting: SnelmCrafting, val heldU: HeldUInteractions, val objs: ObjTypeList)
}
