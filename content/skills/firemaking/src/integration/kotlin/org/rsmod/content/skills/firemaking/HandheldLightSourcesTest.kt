package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos

@Execution(ExecutionMode.SAME_THREAD)
class HandheldLightSourcesTest {
    @Test
    fun GameTestState.`cache 233 light-source identities levels and operations are pinned`() =
        runLightSourceTest { deps ->
            assertEquals(
                listOf(
                    Contract(596, 594, 1, 4, drySticks = true),
                    Contract(36, 33, 1, 1),
                    Contract(38, 32, 1, 1),
                    Contract(4529, 4531, 4, 2),
                    Contract(4532, 4534, 4, 2),
                    Contract(4522, 4524, 12, 2),
                    Contract(4537, 4539, 26, 2),
                    Contract(4548, 4550, 49, 2),
                    Contract(4701, 4702, 49, 1),
                    Contract(9064, 9065, 49, 3),
                    Contract(5014, 5013, 65, 3, wearable = true),
                ),
                LightSourceRecipes.all.map {
                    Contract(
                        it.unlit.id,
                        it.lit.id,
                        it.level,
                        it.extinguishOp,
                        it.canUseDrySticks,
                        it.wearable,
                    )
                },
            )
            assertTrue(LightSourceRecipes.all.all { deps.objs[it.unlit].members })
            assertTrue(LightSourceRecipes.all.all { deps.objs[it.lit].members })
            val operationMismatches =
                LightSourceRecipes.all.mapNotNull { recipe ->
                    val operations = deps.objs[recipe.lit].iop.toList()
                    if (operations.getOrNull(recipe.extinguishOp - 1) == "Extinguish") {
                        null
                    } else {
                        "${deps.objs[recipe.lit].name}: $operations"
                    }
                }
            assertEquals(emptyList<String>(), operationMismatches)
            assertEquals("Wear", deps.objs[objs.unlit_mining_helmet].iop[1])
            assertEquals("Wear", deps.objs[objs.lit_mining_helmet].iop[1])
            assertEquals("Rub-together", deps.objs[objs.dry_sticks].iop[0])
        }

    @Test
    fun GameTestState.`every handheld source lights in either tinderbox use order`() =
        runLightSourceTest { deps ->
            for ((index, recipe) in LightSourceRecipes.all.withIndex()) {
                for (reverse in listOf(false, true)) {
                    preparePlayer(recipe.level, xOffset = index * 4 + if (reverse) 1 else 0)
                    player.inv[TOOL_SLOT] = InvObj(objs.tinderbox)
                    player.inv[SOURCE_SLOT] = InvObj(recipe.unlit)

                    player.withProtectedAccess {
                        val tinderbox = deps.objs[objs.tinderbox]
                        val source = deps.objs[recipe.unlit]
                        if (reverse) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                source,
                                SOURCE_SLOT,
                                tinderbox,
                                TOOL_SLOT,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                tinderbox,
                                TOOL_SLOT,
                                source,
                                SOURCE_SLOT,
                            )
                        }
                    }

                    assertEquals(objs.tinderbox.id, player.inv[TOOL_SLOT]?.id)
                    assertEquals(recipe.lit.id, player.inv[SOURCE_SLOT]?.id)
                    assertEquals(0, player.statMap.getFineXP(stats.firemaking))
                }
            }
        }

    @Test
    fun GameTestState
        .`dry sticks rub action lights a torch at level thirty and item use does not`() =
        runLightSourceTest { deps ->
            preparePlayer(30)
            player.inv[TOOL_SLOT] = InvObj(objs.dry_sticks)
            player.inv[SOURCE_SLOT] = InvObj(objs.unlit_torch)

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, TOOL_SLOT, HeldOp.Op1)
            }

            assertEquals(objs.dry_sticks.id, player.inv[TOOL_SLOT]?.id)
            assertEquals(objs.lit_torch.id, player.inv[SOURCE_SLOT]?.id)
            assertMessageSent("You rub together the dry sticks and the sticks catch alight.")
            assertMessageSent("You place the smouldering twigs to your torch.")
            assertMessageSent("Your torch lights.")

            preparePlayer(30)
            player.inv[TOOL_SLOT] = InvObj(objs.dry_sticks)
            player.inv[SOURCE_SLOT] = InvObj(objs.unlit_torch)
            useTogether(deps, objs.dry_sticks.id, TOOL_SLOT, objs.unlit_torch.id, SOURCE_SLOT)
            assertEquals(objs.unlit_torch.id, player.inv[SOURCE_SLOT]?.id)

            preparePlayer(29)
            player.inv[TOOL_SLOT] = InvObj(objs.dry_sticks)
            player.inv[SOURCE_SLOT] = InvObj(objs.unlit_torch)
            player.withProtectedAccess {
                deps.held.interact(this, player.inv, TOOL_SLOT, HeldOp.Op1)
            }
            assertEquals(objs.unlit_torch.id, player.inv[SOURCE_SLOT]?.id)
            assertMessageSent("You need a Firemaking level of at least 30 to light this.")
        }

    @Test
    fun GameTestState.`lighting uses boosted Firemaking and enforces members and level gates`() =
        runLightSourceTest { deps ->
            val recipe = LightSourceRecipes.all.first { it.unlit == objs.unlit_bullseye_lantern }

            preparePlayer(level = 49, baseLevel = 44)
            addLightingKit(recipe)
            useTogether(deps, objs.tinderbox.id, TOOL_SLOT, recipe.unlit.id, SOURCE_SLOT)
            assertEquals(recipe.lit.id, player.inv[SOURCE_SLOT]?.id)

            preparePlayer(level = 48)
            addLightingKit(recipe)
            useTogether(deps, objs.tinderbox.id, TOOL_SLOT, recipe.unlit.id, SOURCE_SLOT)
            assertEquals(recipe.unlit.id, player.inv[SOURCE_SLOT]?.id)
            assertMessageSent("You need a Firemaking level of at least 49 to light this.")

            preparePlayer(level = 99, members = false)
            addLightingKit(recipe)
            useTogether(deps, objs.tinderbox.id, TOOL_SLOT, recipe.unlit.id, SOURCE_SLOT)
            assertEquals(recipe.unlit.id, player.inv[SOURCE_SLOT]?.id)
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState.`all lit inventory sources dispatch their cache extinguish operation`() =
        runLightSourceTest { deps ->
            for ((index, recipe) in LightSourceRecipes.all.withIndex()) {
                preparePlayer(99, xOffset = index)
                player.inv[SOURCE_SLOT] = InvObj(recipe.lit)
                val op = requireNotNull(HeldOp[recipe.extinguishOp])

                player.withProtectedAccess { deps.held.interact(this, player.inv, SOURCE_SLOT, op) }

                assertEquals(recipe.unlit.id, player.inv[SOURCE_SLOT]?.id)
            }
        }

    @Test
    fun GameTestState.`worn mining helmet extinguishes without unequipping`() =
        runLightSourceTest { deps ->
            preparePlayer(99)
            player.worn[Wearpos.Hat.slot] = InvObj(objs.lit_mining_helmet, vars = 37)

            player.withProtectedAccess {
                deps.worn.interact(this, player.worn, Wearpos.Hat.slot, IfButtonOp.Op2)
            }

            assertEquals(objs.unlit_mining_helmet.id, player.worn[Wearpos.Hat.slot]?.id)
            assertEquals(37, player.worn[Wearpos.Hat.slot]?.vars)
        }

    @Test
    fun GameTestState.`all four candle-lantern combinations transform atomically with no xp`() =
        runLightSourceTest { deps ->
            for ((index, recipe) in CandleLanternRecipes.all.withIndex()) {
                preparePlayer(recipe.level, xOffset = index)
                player.inv[LANTERN_SLOT] = InvObj(objs.empty_candle_lantern)
                player.inv[CANDLE_SLOT] = InvObj(recipe.candle)
                for (slot in 2 until player.inv.size) {
                    player.inv[slot] = InvObj(objs.coins)
                }

                useTogether(
                    deps,
                    objs.empty_candle_lantern.id,
                    LANTERN_SLOT,
                    recipe.candle.id,
                    CANDLE_SLOT,
                )

                assertEquals(recipe.output.id, player.inv[LANTERN_SLOT]?.id)
                assertEquals(null, player.inv[CANDLE_SLOT])
                assertEquals(0, player.statMap.getFineXP(stats.firemaking))
            }
        }

    @Test
    fun GameTestState.`lit-candle lantern assembly requires boosted level four`() =
        runLightSourceTest { deps ->
            val recipe = CandleLanternRecipes.all.first { it.candle == objs.lit_candle }

            preparePlayer(level = 4, baseLevel = 1)
            player.inv[LANTERN_SLOT] = InvObj(objs.empty_candle_lantern)
            player.inv[CANDLE_SLOT] = InvObj(recipe.candle)
            useTogether(
                deps,
                objs.empty_candle_lantern.id,
                LANTERN_SLOT,
                recipe.candle.id,
                CANDLE_SLOT,
            )
            assertEquals(recipe.output.id, player.inv[LANTERN_SLOT]?.id)

            preparePlayer(level = 3)
            player.inv[LANTERN_SLOT] = InvObj(objs.empty_candle_lantern)
            player.inv[CANDLE_SLOT] = InvObj(recipe.candle)
            useTogether(
                deps,
                objs.empty_candle_lantern.id,
                LANTERN_SLOT,
                recipe.candle.id,
                CANDLE_SLOT,
            )
            assertEquals(objs.empty_candle_lantern.id, player.inv[LANTERN_SLOT]?.id)
            assertEquals(recipe.candle.id, player.inv[CANDLE_SLOT]?.id)
        }

    private fun GameTestScope.preparePlayer(
        level: Int,
        baseLevel: Int = level,
        members: Boolean = true,
        xOffset: Int = 0,
    ) {
        player.clearInv()
        player.worn.fillNulls()
        player.members = members
        player.setBaseLevel(stats.firemaking, baseLevel)
        player.setCurrentLevel(stats.firemaking, level)
        player.statMap.setXP(stats.firemaking, 0)
        player.placeAt(player.coords.translateX(xOffset))
        client.clear()
    }

    private fun GameTestScope.addLightingKit(recipe: LightSourceRecipe) {
        player.inv[TOOL_SLOT] = InvObj(objs.tinderbox)
        player.inv[SOURCE_SLOT] = InvObj(recipe.unlit)
    }

    private fun GameTestScope.useTogether(
        deps: Dependencies,
        firstId: Int,
        firstSlot: Int,
        secondId: Int,
        secondSlot: Int,
    ) {
        player.withProtectedAccess {
            deps.heldU.interact(
                this,
                player.inv,
                deps.objs.getValue(firstId),
                firstSlot,
                deps.objs.getValue(secondId),
                secondSlot,
            )
        }
    }

    private fun GameTestState.runLightSourceTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            FiremakingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(FiremakingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val handheld: HandheldLightSources,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val worn: WornInteractions,
        val objs: ObjTypeList,
    )

    private data class Contract(
        val unlit: Int,
        val lit: Int,
        val level: Int,
        val extinguishOp: Int,
        val drySticks: Boolean = false,
        val wearable: Boolean = false,
    )

    private companion object {
        const val TOOL_SLOT = 0
        const val SOURCE_SLOT = 1
        const val LANTERN_SLOT = 0
        const val CANDLE_SLOT = 1
    }
}
