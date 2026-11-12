package org.rsmod.content.skills.crafting.birdhouses

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BirdhouseCraftingTest {
    @Test
    fun GameTestState.`cache 233 pins every birdhouse recipe and both animations`() =
        runBirdhouseTest { deps ->
            assertEquals(8792, BirdhouseObjs.clockwork.id)
            assertEquals(7057, deps.seqTypes[BirdhouseSeqs.make].id)
            assertEquals(8916, deps.seqTypes[BirdhouseSeqs.makeWithImcandoHammer].id)
            assertEquals(
                listOf(1511, 1521, 1519, 6333, 1517, 6332, 1515, 1513, 19669),
                BirdhouseRecipes.entries.map { it.logs.id },
            )
            assertEquals(
                listOf(21512, 21515, 21518, 21521, 22192, 22195, 22198, 22201, 22204),
                BirdhouseRecipes.entries.map { it.output.id },
            )
            assertEquals(
                listOf(5, 15, 25, 35, 45, 50, 60, 75, 90),
                BirdhouseRecipes.entries.map { it.level },
            )
            assertEquals(
                listOf(15.0, 20.0, 25.0, 30.0, 35.0, 40.0, 45.0, 50.0, 55.0),
                BirdhouseRecipes.entries.map { it.xp },
            )
            for (recipe in BirdhouseRecipes.entries) {
                val output = deps.objTypes[recipe.output]
                assertTrue(output.members)
                assertEquals(recipe.name.replaceFirstChar { it.uppercase() }, output.name)
            }
        }

    @Test
    fun GameTestState.`all nine exact levels make one house after two ticks for exact xp`() =
        runBirdhouseTest { deps ->
            for (recipe in BirdhouseRecipes.entries) {
                prepare(recipe)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.crafting.start(this, recipe) }
                assertEquals(BirdhouseSeqs.make.id, player.pendingSequence.id)
                advance(2)

                assertEquals(1, player.count(recipe.output))
                assertEquals(0, player.count(recipe.logs))
                assertEquals(0, player.count(BirdhouseObjs.clockwork))
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                )
            }
        }

    @Test
    fun GameTestState.`membership boosted level chisel and hammer gates preserve materials`() =
        runBirdhouseTest { deps ->
            val recipe = BirdhouseRecipes.entries.last()
            prepare(recipe)
            player.members = false
            start(deps, recipe)
            assertMessageSent("You need to be on a members' world to make a bird house.")
            assertMaterialsPreserved(recipe)

            prepare(recipe)
            player.stats[stats.crafting] = recipe.level - 1
            start(deps, recipe)
            assertMessageSent("You need a Crafting level of 90 to make this bird house.")
            assertMaterialsPreserved(recipe)

            prepare(recipe)
            remove(objs.chisel)
            start(deps, recipe)
            assertMessageSent("You need a chisel to make a bird house.")
            assertMaterialsPreserved(recipe)

            prepare(recipe)
            remove(objs.hammer)
            start(deps, recipe)
            assertMessageSent("You need a hammer to make a bird house.")
            assertMaterialsPreserved(recipe)
        }

    @Test
    fun GameTestState
        .`reverse use succeeds in a full inventory and imcando tools choose their animation`() =
        runBirdhouseTest { deps ->
            val recipe = BirdhouseRecipes.entries[4]
            player.members = true
            player.stats[stats.crafting] = recipe.level
            player.fillInv()
            player.inv[0] = InvObj(BirdhouseObjs.clockwork)
            player.inv[1] = InvObj(recipe.logs)
            player.inv[2] = InvObj(objs.chisel)
            player.worn[Wearpos.LeftHand.slot] = InvObj(objs.imcando_hammer_offhand)

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objTypes[recipe.logs],
                    1,
                    deps.objTypes[BirdhouseObjs.clockwork],
                    0,
                )
            }
            assertEquals(BirdhouseSeqs.makeWithImcandoHammer.id, player.pendingSequence.id)
            advance(2)

            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.inv.freeSpace())
        }

    @Test
    fun GameTestState
        .`make all repeats on the third tick and crafting cancellation loses nothing`() =
        runBirdhouseTest { deps ->
            val recipe = BirdhouseRecipes.entries[1]
            prepare(recipe, count = 2)
            val startXp = player.statMap.getFineXP(stats.crafting)
            start(deps, recipe)

            advance(2)
            assertEquals(1, player.count(recipe.output))
            advance(2)
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(2, player.count(recipe.output))
            assertEquals(400, player.statMap.getFineXP(stats.crafting) - startXp)

            prepare(recipe)
            start(deps, recipe)
            player.withProtectedAccess { clearCraftingQueues() }
            advance(2)
            assertEquals(0, player.count(recipe.output))
            assertMaterialsPreserved(recipe)
        }

    private fun GameTestState.runBirdhouseTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(BirdhouseCraftingScript::class),
            testBody = body,
        )

    private fun GameTestScope.prepare(recipe: BirdhouseRecipe, count: Int = 1) {
        player.withProtectedAccess { clearCraftingQueues() }
        player.clearInv()
        client.clear()
        player.members = true
        player.stats[stats.crafting] = recipe.level
        repeat(count) { put(InvObj(BirdhouseObjs.clockwork)) }
        repeat(count) { put(InvObj(recipe.logs)) }
        put(InvObj(objs.chisel))
        put(InvObj(objs.hammer))
        player.worn[Wearpos.RightHand.slot] = null
        player.worn[Wearpos.LeftHand.slot] = null
    }

    private fun GameTestScope.remove(obj: org.rsmod.game.type.obj.ObjType) {
        player.inv[player.inv.indexOfFirst { it?.id == obj.id }] = null
    }

    private fun GameTestScope.put(obj: InvObj) {
        player.inv[player.inv.indexOfFirst { it == null }] = obj
    }

    private fun GameTestScope.start(deps: Dependencies, recipe: BirdhouseRecipe) {
        player.withProtectedAccess { deps.crafting.start(this, recipe) }
    }

    private fun GameTestScope.assertMaterialsPreserved(recipe: BirdhouseRecipe) {
        assertEquals(1, player.count(BirdhouseObjs.clockwork))
        assertEquals(1, player.count(recipe.logs))
        assertEquals(0, player.count(recipe.output))
    }

    private data class Dependencies
    @Inject
    constructor(
        val crafting: BirdhouseCrafting,
        val heldU: HeldUInteractions,
        val objTypes: ObjTypeList,
        val seqTypes: SeqTypeList,
    )
}
