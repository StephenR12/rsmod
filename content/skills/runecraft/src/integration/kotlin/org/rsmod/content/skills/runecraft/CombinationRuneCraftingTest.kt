package org.rsmod.content.skills.runecraft

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.front
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.spell.utility.standard.MagicImbueStateRepository
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class CombinationRuneCraftingTest {
    @Test
    fun GameTestState.`cache 233 contracts define every combination rune direction`() =
        runRunecraftTest { _ ->
            assertEquals(
                listOf(34760, 34762, 34763, 34764, 34766),
                CombinationRuneRecipes.all.map { it.altar.id }.distinct(),
            )
            assertEquals(
                listOf(8.0, 8.5, 8.3, 9.0, 9.3, 9.5, 8.5, 9.5, 9.3, 10.0, 10.0, 10.5),
                CombinationRuneRecipes.elemental.map { it.xp },
            )
            assertEquals(
                listOf(6, 6, 10, 10, 13, 13, 15, 15, 19, 19, 23, 23),
                CombinationRuneRecipes.elemental.map { it.level },
            )
            assertEquals(
                setOf(4694, 4695, 4696, 4697, 4698, 4699),
                CombinationRuneRecipes.elemental.mapTo(mutableSetOf()) { it.output.id },
            )
            assertEquals(30843, CombinationRuneRecipes.aether.output.id)
            assertEquals(30771, RunecraftObjs.aetherCatalyst.id)
            assertEquals(24704, RunecraftObjs.daeyaltEssence.id)
            assertFalse(objTypes[RunecraftObjs.pureEssence].stackable)
            assertFalse(objTypes[RunecraftObjs.daeyaltEssence].stackable)
            assertTrue(objTypes[RunecraftObjs.aetherCatalyst].stackable)
            assertEquals(
                listOf(null, "Wear", "Check", null, "Destroy"),
                objTypes[objs.binding_necklace].iop.toList(),
            )
            assertEquals(791, RunecraftSeqs.craft.id)
            assertEquals(186, RunecraftSpotanims.craft.id)
            assertEquals(487, RunecraftVarps.bindingNecklace.id)
        }

    @Test
    fun GameTestState
        .`all twelve elemental routes consume exact inputs and award directional xp`() =
        runRunecraftTest { deps ->
            for (recipe in CombinationRuneRecipes.elemental) {
                preparePlayer(deps)
                player.inv[0] = InvObj(recipe.inputRune, 2)
                player.inv[1] = InvObj(RunecraftObjs.pureEssence)
                player.inv[2] = InvObj(RunecraftObjs.pureEssence)
                player.inv[3] = InvObj(requireNotNull(recipe.talisman))
                player.front = InvObj(objs.binding_necklace)
                val startXp = player.statMap.getFineXP(stats.runecrafting)

                player.withProtectedAccess { deps.crafting.craft(this, recipe, 0) }

                assertEquals(2, player.count(recipe.output), recipe.toString())
                assertEquals(0, player.count(recipe.inputRune), recipe.toString())
                assertEquals(0, player.count(RunecraftObjs.pureEssence), recipe.toString())
                assertEquals(0, player.count(requireNotNull(recipe.talisman)), recipe.toString())
                assertEquals(
                    (recipe.xp * 20).toInt(),
                    player.statMap.getFineXP(stats.runecrafting) - startXp,
                )
                assertEquals(RunecraftSeqs.craft.id, player.pendingSequence.id)
                assertEquals(15, deps.necklace.remaining(player))
            }
        }

    @Test
    fun GameTestState
        .`ordinary rolls are independent and consume failed essence rune and talisman`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.elemental.first()
            preparePlayer(deps)
            player.inv[0] = InvObj(recipe.inputRune, 4)
            repeat(4) { player.inv[it + 1] = InvObj(RunecraftObjs.pureEssence) }
            player.inv[5] = InvObj(requireNotNull(recipe.talisman))
            random.next = 0
            random.then = 1
            random.then = 0
            random.then = 1
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent(
                    "You partially succeed to bind the temple's power into mist runes."
                )
            }

            assertEquals(2, player.count(recipe.output))
            assertEquals(0, player.count(recipe.inputRune))
            assertEquals(0, player.count(RunecraftObjs.pureEssence))
            assertEquals(0, player.count(requireNotNull(recipe.talisman)))
            assertEquals(160, player.statMap.getFineXP(stats.runecrafting) - startXp)
            assertEquals(0 until 2, random.lastInclusiveRange)
        }

    @Test
    fun GameTestState
        .`Magic Imbue bypasses only the talisman and leaves success at fifty percent`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.elemental.first()
            preparePlayer(deps)
            deps.magicImbue.activate(player)
            player.inv[0] = InvObj(recipe.inputRune)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            random.next = 1

            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent("You fail to bind the temple's power into mist runes.")
            }

            assertEquals(0, player.count(recipe.output))
            assertEquals(0, player.count(recipe.inputRune))
            assertEquals(0, player.count(RunecraftObjs.pureEssence))
            assertTrue(deps.magicImbue.isActive(player))
        }

    @Test
    fun GameTestState.`explicitly using a talisman consumes it even while Magic Imbue is active`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.elemental.first()
            preparePlayer(deps)
            deps.magicImbue.activate(player)
            player.inv[0] = InvObj(recipe.inputRune)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.inv[2] = InvObj(requireNotNull(recipe.talisman))
            player.front = InvObj(objs.binding_necklace)

            player.withProtectedAccess { deps.crafting.craft(this, recipe, 2) }

            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(requireNotNull(recipe.talisman)))
        }

    @Test
    fun GameTestState.`members level and missing-resource failures are atomic`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.elemental.last()
            preparePlayer(deps)
            player.inv[0] = InvObj(recipe.inputRune)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.inv[2] = InvObj(requireNotNull(recipe.talisman))

            player.members = false
            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent("You need to be on a members' world to craft combination runes.")
            }
            player.members = true
            player.stats[stats.runecrafting] = recipe.level - 1
            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent(
                    "You need a Runecrafting level of at least 23 to craft lava runes."
                )
            }

            assertEquals(1, player.count(recipe.inputRune))
            assertEquals(1, player.count(RunecraftObjs.pureEssence))
            assertEquals(1, player.count(requireNotNull(recipe.talisman)))
            assertEquals(0, player.count(recipe.output))
        }

    @Test
    fun GameTestState
        .`aether requires active imbue and Binding necklace then consumes exact resources`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.aether
            preparePlayer(deps)
            player.inv[0] = InvObj(recipe.inputRune, 3)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.inv[2] = InvObj(RunecraftObjs.daeyaltEssence)
            player.inv[3] = InvObj(RunecraftObjs.daeyaltEssence)
            player.inv[4] = InvObj(RunecraftObjs.aetherCatalyst, 3)
            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent("You need Magic Imbue to be active to craft aether runes.")
            }
            assertEquals(3, player.count(recipe.inputRune))

            deps.magicImbue.activate(player)
            player.withProtectedAccess {
                deps.crafting.craft(this, recipe, 0)
                assertMessageSent("You need to wear a Binding necklace to craft aether runes.")
            }
            assertEquals(3, player.count(recipe.inputRune))

            player.front = InvObj(objs.binding_necklace)
            val startXp = player.statMap.getFineXP(stats.runecrafting)
            player.withProtectedAccess { deps.crafting.craft(this, recipe, 0) }

            assertEquals(3, player.count(recipe.output))
            assertEquals(0, player.count(recipe.inputRune))
            assertEquals(0, player.count(RunecraftObjs.pureEssence))
            assertEquals(0, player.count(RunecraftObjs.daeyaltEssence))
            assertEquals(0, player.count(RunecraftObjs.aetherCatalyst))
            assertEquals(600, player.statMap.getFineXP(stats.runecrafting) - startXp)
            assertEquals(15, deps.necklace.remaining(player))
        }

    @Test
    fun GameTestState
        .`Binding necklace state is shared and disintegrates on the sixteenth interaction`() =
        runRunecraftTest { deps ->
            val recipe = CombinationRuneRecipes.elemental.first()
            preparePlayer(deps)
            deps.magicImbue.activate(player)
            player.front = InvObj(objs.binding_necklace)
            repeat(16) { use ->
                player.inv[0] = InvObj(recipe.inputRune)
                player.inv[1] = InvObj(RunecraftObjs.pureEssence)
                player.withProtectedAccess { deps.crafting.craft(this, recipe, 0) }
                if (use < 15) {
                    assertEquals(15 - use, deps.necklace.remaining(player))
                    assertEquals(objs.binding_necklace.id, player.front?.id)
                }
            }

            assertNull(player.front)
            assertEquals(16, deps.necklace.remaining(player))
            player.inv[2] = InvObj(objs.binding_necklace)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, 2, HeldOp.Op3)
                assertMessageSent(
                    "You have 16 charges left before your Binding necklace disintegrates."
                )
            }
            deps.necklace.consume(player)
            player.withProtectedAccess {
                deps.crafting.checkNecklace(this)
                assertMessageSent(
                    "You have 15 charges left before your Binding necklace disintegrates."
                )
            }
            deps.crafting.resetNecklaceCharges(player)
            assertEquals(16, deps.necklace.remaining(player))
        }

    private fun GameTestScope.preparePlayer(deps: Dependencies) {
        player.members = true
        player.stats[stats.runecrafting] = 99
        player.clearInv()
        player.front = null
        deps.magicImbue.clear(player)
        deps.necklace.reset(player)
        client.clear()
    }

    private fun GameTestState.runRunecraftTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            RunecraftScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(RunecraftModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: CombinationRuneCrafting,
        val necklace: BindingNecklaceRepository,
        val magicImbue: MagicImbueStateRepository,
        val held: HeldInteractions,
    )
}
