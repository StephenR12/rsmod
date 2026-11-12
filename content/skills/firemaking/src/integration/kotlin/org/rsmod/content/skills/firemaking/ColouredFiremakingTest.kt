package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ColouredFiremakingTest {
    @Test
    fun GameTestState.`cache 233 coloured log and fire contracts are pinned`() =
        runFiremakingTest { deps ->
            assertEquals(
                listOf(
                    ColouredContract("red", 7329, 7404, 26186),
                    ColouredContract("green", 7330, 7405, 26575),
                    ColouredContract("blue", 7331, 7406, 26576),
                    ColouredContract("purple", 10326, 10329, 20001),
                    ColouredContract("white", 10327, 10328, 20000),
                ),
                ColouredLogRecipes.all.map {
                    ColouredContract(it.colour, it.firelighter.id, it.log.id, it.fire.id)
                },
            )
            assertTrue(
                ColouredLogRecipes.all.all { deps.objs.getValue(it.firelighter.id).stackable }
            )
            assertTrue(ColouredLogRecipes.all.all { deps.objs.getValue(it.firelighter.id).members })
            assertTrue(ColouredLogRecipes.all.all { deps.objs.getValue(it.log.id).members })
            assertTrue(ColouredLogRecipes.all.all { deps.types.locs.containsKey(it.fire.id) })
            assertTrue(
                ColouredLogRecipes.all.all {
                    deps.objs.getValue(it.log.id).op.indexOf("Light") == 3
                }
            )
            assertTrue(
                FiremakingRecipes.coloured.all {
                    it.level == 1 && it.xp == 50.0 && it.members && it.guaranteed
                }
            )
        }

    @Test
    fun GameTestState.`cache 233 gnomish holder contract and packed state are pinned`() =
        runFiremakingTest { deps ->
            assertEquals(20275, objs.gnomish_firelighter.id)
            assertEquals(20278, objs.gnomish_firelighter_charged.id)
            assertEquals("Check", deps.objs.getValue(objs.gnomish_firelighter.id).iop[3])
            assertEquals("Check", deps.objs.getValue(objs.gnomish_firelighter_charged.id).iop[3])
            assertEquals("Uncharge", deps.objs.getValue(objs.gnomish_firelighter_charged.id).iop[4])
            assertEquals(0..9, varobjs.gnomish_firelighter_charges.bits)
            assertEquals(10..12, varobjs.gnomish_firelighter_colour.bits)

            for ((index, colour) in ColouredLogRecipes.all.withIndex()) {
                val state = GnomishFirelighterState(colour, charges = 201 + index)
                assertEquals(state, GnomishFirelighterState.from(state.toInvObj()))
            }
        }

    @Test
    fun GameTestState.`holder charging tops up one colour and enforces its cap`() =
        runFiremakingTest { deps ->
            val red = ColouredLogRecipes.all.first { it.colour == "red" }
            val green = ColouredLogRecipes.all.first { it.colour == "green" }
            preparePlayer(224)
            player.inv[FIRELIGHTER_SLOT] = InvObj(red.firelighter, count = 1_200)
            player.inv[CHARGE_HOLDER_SLOT] = InvObj(objs.gnomish_firelighter)

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objs.getValue(red.firelighter.id),
                    FIRELIGHTER_SLOT,
                    deps.objs.getValue(objs.gnomish_firelighter.id),
                    CHARGE_HOLDER_SLOT,
                )
            }

            assertEquals(200, player.inv[FIRELIGHTER_SLOT]?.count)
            assertEquals(
                GnomishFirelighterState(red, GnomishFirelighterState.MAX_CHARGES),
                deps.gnomishFirelighter.state(player.inv, CHARGE_HOLDER_SLOT),
            )

            preparePlayer(232)
            player.inv[FIRELIGHTER_SLOT] = InvObj(red.firelighter, count = 2)
            player.inv[CHARGE_HOLDER_SLOT] = GnomishFirelighterState(red, 999).toInvObj()
            player.withProtectedAccess {
                deps.gnomishFirelighter.charge(this, FIRELIGHTER_SLOT, CHARGE_HOLDER_SLOT, red)
            }
            assertEquals(1, player.inv[FIRELIGHTER_SLOT]?.count)
            assertEquals(
                GnomishFirelighterState(red, GnomishFirelighterState.MAX_CHARGES),
                deps.gnomishFirelighter.state(player.inv, CHARGE_HOLDER_SLOT),
            )

            preparePlayer(240)
            player.inv[FIRELIGHTER_SLOT] = InvObj(green.firelighter, count = 3)
            player.inv[CHARGE_HOLDER_SLOT] = GnomishFirelighterState(red, 500).toInvObj()
            player.withProtectedAccess {
                deps.gnomishFirelighter.charge(this, FIRELIGHTER_SLOT, CHARGE_HOLDER_SLOT, green)
            }
            assertEquals(3, player.inv[FIRELIGHTER_SLOT]?.count)
            assertEquals(
                GnomishFirelighterState(red, 500),
                deps.gnomishFirelighter.state(player.inv, CHARGE_HOLDER_SLOT),
            )
            assertMessageSent(
                "You must uncharge the gnomish firelighter before changing its colour."
            )
        }

    @Test
    fun GameTestState.`holder check and uncharge routes preserve state atomically`() =
        runFiremakingTest { deps ->
            val purple = ColouredLogRecipes.all.first { it.colour == "purple" }
            preparePlayer(248)
            player.inv[HOLDER_SLOT] = GnomishFirelighterState(purple, 3).toInvObj()

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, HOLDER_SLOT, HeldOp.Op4)
            }
            assertMessageSent("Your gnomish firelighter contains 3 purple firelighters.")

            player.withProtectedAccess {
                deps.held.interact(this, player.inv, HOLDER_SLOT, HeldOp.Op5)
            }
            assertEquals(objs.gnomish_firelighter.id, player.inv[HOLDER_SLOT]?.id)
            assertEquals(
                3,
                player.inv.filterNotNull().single { it.id == purple.firelighter.id }.count,
            )

            preparePlayer(256)
            for (slot in player.inv.indices) {
                player.inv[slot] = InvObj(objs.logs)
            }
            val fullState = GnomishFirelighterState(purple, 3)
            player.inv[HOLDER_SLOT] = fullState.toInvObj()
            player.withProtectedAccess { deps.gnomishFirelighter.uncharge(this, HOLDER_SLOT) }

            assertEquals(fullState, deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT))
            assertMessageSent(
                "You don't have enough inventory space to uncharge the gnomish firelighter."
            )
        }

    @Test
    fun GameTestState.`charged holder lights every coloured fire and consumes one charge`() =
        runFiremakingTest { deps ->
            for ((index, colour) in ColouredLogRecipes.all.withIndex()) {
                preparePlayer(272 + index * 8, firemaking = 1)
                player.inv[HOLDER_SLOT] = GnomishFirelighterState(colour, 2).toInvObj()
                player.inv[LOG_SLOT] = InvObj(objs.logs)
                random.next = Firemaking.MIN_FIRE_TICKS
                val fireCoords = player.coords

                player.withProtectedAccess {
                    deps.firemaking.lightFromGnomishFirelighter(this, LOG_SLOT, HOLDER_SLOT)
                }
                advance(Firemaking.ATTEMPT_TICKS)

                assertNotNull(deps.locRepo.findExact(fireCoords, colour.fire), colour.colour)
                assertEquals(500, firemakingXp())
                assertEquals(
                    GnomishFirelighterState(colour, 1),
                    deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT),
                )
            }

            preparePlayer(320, firemaking = 1)
            val white = ColouredLogRecipes.all.first { it.colour == "white" }
            player.inv[HOLDER_SLOT] = GnomishFirelighterState(white, 1).toInvObj()
            player.inv[LOG_SLOT] = InvObj(objs.logs)
            random.next = Firemaking.MIN_FIRE_TICKS
            player.withProtectedAccess {
                deps.firemaking.lightFromGnomishFirelighter(this, LOG_SLOT, HOLDER_SLOT)
            }
            advance(Firemaking.ATTEMPT_TICKS)

            assertEquals(objs.gnomish_firelighter.id, player.inv[HOLDER_SLOT]?.id)
            assertMessageSent("Your Gnomish firelighter has run out of charges.")
        }

    @Test
    fun GameTestState.`holder is members only and lights standard logs only`() =
        runFiremakingTest { deps ->
            val red = ColouredLogRecipes.all.first()
            preparePlayer(328)
            val state = GnomishFirelighterState(red, 2)
            player.inv[HOLDER_SLOT] = state.toInvObj()
            player.inv[LOG_SLOT] = InvObj(objs.oak_logs)
            player.withProtectedAccess {
                deps.firemaking.lightFromGnomishFirelighter(this, LOG_SLOT, HOLDER_SLOT)
            }
            assertEquals(objs.oak_logs.id, player.inv[LOG_SLOT]?.id)
            assertEquals(state, deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT))

            preparePlayer(336, members = false)
            player.inv[HOLDER_SLOT] = state.toInvObj()
            player.inv[LOG_SLOT] = InvObj(objs.logs)
            player.withProtectedAccess {
                deps.firemaking.lightFromGnomishFirelighter(this, LOG_SLOT, HOLDER_SLOT)
            }
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertEquals(state, deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT))
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState
        .`ground Light falls back to holder and pending state changes cancel safely`() =
        runFiremakingTest { deps ->
            val blue = ColouredLogRecipes.all.first { it.colour == "blue" }
            preparePlayer(344, firemaking = 1)
            player.inv[HOLDER_SLOT] = GnomishFirelighterState(blue, 2).toInvObj()
            random.next = Firemaking.MIN_FIRE_TICKS
            val fireCoords = player.coords
            val groundLog =
                deps.objRepo.add(objs.logs, fireCoords, duration = Int.MAX_VALUE, receiver = player)
            player.withProtectedAccess {
                deps.firemaking.lightFromGround(
                    this,
                    groundLog,
                    requireNotNull(FiremakingRecipes.byLog[objs.logs.id]),
                )
            }
            advance(Firemaking.ATTEMPT_TICKS)
            assertNotNull(deps.locRepo.findExact(fireCoords, blue.fire))
            assertEquals(
                GnomishFirelighterState(blue, 1),
                deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT),
            )

            preparePlayer(352, firemaking = 1)
            player.inv[HOLDER_SLOT] = GnomishFirelighterState(blue, 2).toInvObj()
            player.inv[LOG_SLOT] = InvObj(objs.logs)
            val pendingCoords = player.coords
            player.withProtectedAccess {
                deps.firemaking.lightFromGnomishFirelighter(this, LOG_SLOT, HOLDER_SLOT)
            }
            player.inv[HOLDER_SLOT] = GnomishFirelighterState(blue, 1).toInvObj()
            advance(Firemaking.ATTEMPT_TICKS)

            assertNull(deps.locRepo.findExact(pendingCoords, blue.fire))
            assertEquals(1, groundCount(deps, objs.logs.id, pendingCoords))
            assertEquals(0, firemakingXp())
            assertEquals(
                GnomishFirelighterState(blue, 1),
                deps.gnomishFirelighter.state(player.inv, HOLDER_SLOT),
            )
        }

    @Test
    fun GameTestState.`all firelighter colours convert logs symmetrically and atomically`() =
        runFiremakingTest { deps ->
            for ((recipeIndex, recipe) in ColouredLogRecipes.all.withIndex()) {
                for ((orderIndex, selectedFirelighter) in listOf(true, false).withIndex()) {
                    preparePlayer(recipeIndex * 16 + orderIndex * 8)
                    player.inv[FIRELIGHTER_SLOT] = InvObj(recipe.firelighter, count = 2)
                    player.inv[LOG_SLOT] = InvObj(objs.logs)
                    val firelighter = deps.objs.getValue(recipe.firelighter.id)
                    val logs = deps.objs.getValue(objs.logs.id)

                    player.withProtectedAccess {
                        if (selectedFirelighter) {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                firelighter,
                                FIRELIGHTER_SLOT,
                                logs,
                                LOG_SLOT,
                            )
                        } else {
                            deps.heldU.interact(
                                this,
                                player.inv,
                                logs,
                                LOG_SLOT,
                                firelighter,
                                FIRELIGHTER_SLOT,
                            )
                        }
                    }

                    assertEquals(recipe.firelighter.id, player.inv[FIRELIGHTER_SLOT]?.id)
                    assertEquals(1, player.inv[FIRELIGHTER_SLOT]?.count)
                    assertEquals(recipe.log.id, player.inv[LOG_SLOT]?.id)
                    assertMessageSent("You coat the logs with the ${recipe.colour} chemicals.")
                }
            }
        }

    @Test
    fun GameTestState.`free world conversion preserves both materials`() =
        runFiremakingTest { deps ->
            preparePlayer(96, members = false)
            val recipe = ColouredLogRecipes.all.first()
            player.inv[FIRELIGHTER_SLOT] = InvObj(recipe.firelighter, count = 2)
            player.inv[LOG_SLOT] = InvObj(objs.logs)

            player.withProtectedAccess {
                deps.firemaking.colourLogs(this, FIRELIGHTER_SLOT, LOG_SLOT, recipe)
            }

            assertEquals(2, player.inv[FIRELIGHTER_SLOT]?.count)
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState.`all coloured logs light first try with exact fire and xp`() =
        runFiremakingTest { deps ->
            for ((index, coloured) in ColouredLogRecipes.all.withIndex()) {
                preparePlayer(112 + index * 8, firemaking = 1)
                player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
                player.inv[LOG_SLOT] = InvObj(coloured.log)
                random.next = Firemaking.MIN_FIRE_TICKS
                val fireCoords = player.coords

                val recipe = requireNotNull(FiremakingRecipes.byLog[coloured.log.id])
                player.withProtectedAccess {
                    deps.firemaking.lightFromInventory(this, LOG_SLOT, recipe)
                }
                advance(Firemaking.ATTEMPT_TICKS)

                assertNotNull(deps.locRepo.findExact(fireCoords, coloured.fire), coloured.colour)
                assertEquals(500, firemakingXp())
                assertFalse(firemaking_queues.attempt in player.weakQueueList)
            }
        }

    @Test
    fun GameTestState.`coloured logs support completed barbarian bow lighting`() =
        runFiremakingTest { deps ->
            for ((index, coloured) in ColouredLogRecipes.all.withIndex()) {
                preparePlayer(160 + index * 8, firemaking = 21)
                player.withProtectedAccess {
                    vars[varbits.brut_fire] = Firemaking.BARBARIAN_TRAINING_COMPLETE
                }
                player.inv[BOW_SLOT] = InvObj(objs.shortbow)
                player.inv[LOG_SLOT] = InvObj(coloured.log)
                random.next = Firemaking.MIN_FIRE_TICKS
                val fireCoords = player.coords
                val recipe = requireNotNull(FiremakingRecipes.byLog[coloured.log.id])
                val tool = requireNotNull(BowFiremakingTools.byBow[objs.shortbow.id])

                player.withProtectedAccess {
                    deps.firemaking.lightFromBow(this, LOG_SLOT, BOW_SLOT, recipe, tool)
                }
                advance(Firemaking.ATTEMPT_TICKS)

                assertNotNull(deps.locRepo.findExact(fireCoords, coloured.fire), coloured.colour)
                assertEquals(500, firemakingXp())
                assertEquals(objs.shortbow.id, player.inv[BOW_SLOT]?.id)
            }
        }

    @Test
    fun GameTestState.`coloured fire expires into ashes on the shared lifecycle`() =
        runFiremakingTest { deps ->
            preparePlayer(208)
            val coloured = ColouredLogRecipes.all.first()
            player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
            player.inv[LOG_SLOT] = InvObj(coloured.log)
            random.next = Firemaking.MIN_FIRE_TICKS
            val fireCoords = player.coords

            player.withProtectedAccess {
                deps.firemaking.lightFromInventory(
                    this,
                    LOG_SLOT,
                    requireNotNull(FiremakingRecipes.byLog[coloured.log.id]),
                )
            }
            advance(Firemaking.ATTEMPT_TICKS)
            advance(Firemaking.MIN_FIRE_TICKS - 2)

            assertNotNull(deps.locRepo.findExact(fireCoords, coloured.fire))
            assertEquals(0, groundCount(deps, objs.ashes.id, fireCoords))
            advance()
            assertNull(deps.locRepo.findExact(fireCoords, coloured.fire))
            assertEquals(1, groundCount(deps, objs.ashes.id, fireCoords))
        }

    private fun GameTestScope.preparePlayer(
        xOffset: Int,
        firemaking: Int = 99,
        members: Boolean = true,
    ) {
        player.clearInv()
        player.observerUUID = 1L
        player.members = members
        player.setBaseLevel(stats.firemaking, firemaking)
        player.setCurrentLevel(stats.firemaking, firemaking)
        player.statMap.setXP(stats.firemaking, 0)
        player.weakQueueList.clear()
        player.placeAt(TEST_ORIGIN.translateX(xOffset))
        client.clear()
    }

    private fun GameTestScope.firemakingXp(): Int = player.statMap.getFineXP(stats.firemaking)

    private fun GameTestScope.groundCount(deps: Dependencies, objId: Int, coords: CoordGrid): Int =
        deps.objRepo.findAll(coords).count { it.type == objId }

    private fun GameTestState.runFiremakingTest(body: GameTestScope.(Dependencies) -> Unit) =
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
        val firemaking: Firemaking,
        val gnomishFirelighter: GnomishFirelighter,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val objs: ObjTypeList,
        val types: TypeListMap,
        val locRepo: LocRepository,
        val objRepo: ObjRepository,
    )

    private data class ColouredContract(
        val colour: String,
        val firelighter: Int,
        val logs: Int,
        val fire: Int,
    )

    private companion object {
        const val TINDERBOX_SLOT = 0
        const val FIRELIGHTER_SLOT = 0
        const val BOW_SLOT = 0
        const val CHARGE_HOLDER_SLOT = 1
        const val HOLDER_SLOT = 0
        const val LOG_SLOT = 1
        val TEST_ORIGIN = CoordGrid(1000, 1000, 0)
    }
}
