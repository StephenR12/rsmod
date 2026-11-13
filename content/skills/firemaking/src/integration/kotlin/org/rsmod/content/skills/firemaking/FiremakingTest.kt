package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.diary.recordLumbridgeOakCut
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.interact.ObjTInteractions
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.game.type.synth.SynthTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
class FiremakingTest {
    @Test
    fun GameTestState
        .`successful local oak fire completes the previously recorded local cut step`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 0, level = 99)
            player.placeAt(CoordGrid(3203, 3225))
            allocZoneCollision(player.coords)
            player.recordLumbridgeOakCut(CoordGrid(3205, 3245))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
            addLightingKit(objs.oak_logs)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS
            lightInventory(deps, objs.oak_logs.id)
            advance(Firemaking.ATTEMPT_TICKS)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ChopAndBurnLumbridgeOak))
        }

    @Test
    fun GameTestState.`cache 233 ordinary log and audiovisual contracts are pinned`() =
        runFiremakingTest { deps ->
            assertEquals(
                listOf(
                    RecipeContract(1511, 1, 40.0, false),
                    RecipeContract(2862, 1, 40.0, true),
                    RecipeContract(1521, 15, 60.0, false),
                    RecipeContract(1519, 30, 90.0, false),
                    RecipeContract(6333, 35, 105.0, true),
                    RecipeContract(10810, 42, 125.0, true),
                    RecipeContract(1517, 45, 135.0, false),
                    RecipeContract(6332, 50, 157.5, true),
                    RecipeContract(1515, 60, 202.5, false),
                    RecipeContract(24691, 62, 96.0, true),
                    RecipeContract(1513, 75, 303.8, true),
                    RecipeContract(19669, 90, 350.0, true),
                ),
                FiremakingRecipes.ordinary.map {
                    RecipeContract(it.log.id, it.level, it.xp, it.members)
                },
            )
            assertEquals(590, objs.tinderbox.id)
            assertEquals(592, objs.ashes.id)
            assertFalse(deps.objs.getValue(objs.ashes.id).isDummyItem)
            assertEquals(26185, firemaking_locs.fire.id)
            assertEquals(733, seqs.human_createfire.id)
            assertEquals(2597, synths.tinder_strike.id)
            assertTrue(deps.seqs.containsKey(seqs.human_createfire.id))
            assertTrue(deps.synths.containsKey(synths.tinder_strike.id))

            val operations =
                FiremakingRecipes.ordinary.associate { recipe ->
                    recipe.log.id to deps.objs.getValue(recipe.log.id).op.toList()
                }
            assertTrue(
                operations.values.all { "Light" in it },
                "Every ordinary log must expose a Light ground operation: $operations",
            )
            assertEquals(
                setOf(3),
                operations.values.mapTo(mutableSetOf()) { it.indexOf("Light") },
                "Cache Light operation moved: $operations",
            )
            for (recipe in FiremakingRecipes.ordinary) {
                assertEquals(
                    recipe.members,
                    deps.objs.getValue(recipe.log.id).members,
                    deps.objs.getValue(recipe.log.id).name,
                )
            }
        }

    @Test
    fun GameTestState.`inventory route drops then lights a log after four ticks`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 0, level = 99)
            addLightingKit(objs.logs)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS

            lightInventory(deps, objs.logs.id)

            assertNull(player.inv[LOG_SLOT])
            assertEquals(1, groundCount(deps, objs.logs.id, player.coords))
            assertEquals(seqs.human_createfire.id, player.pendingSequence.id)
            assertEquals(2597, client.filterIsInstance<SynthSound>().single().id)
            assertTrue(firemaking_queues.attempt in player.weakQueueList)
            assertNull(deps.locRepo.findExact(player.coords, firemaking_locs.fire))

            advance(Firemaking.ATTEMPT_TICKS - 1)
            assertNull(deps.locRepo.findExact(TEST_ORIGIN, firemaking_locs.fire))
            advance()

            assertNotNull(deps.locRepo.findExact(TEST_ORIGIN, firemaking_locs.fire))
            assertEquals(0, groundCount(deps, objs.logs.id, TEST_ORIGIN))
            assertEquals(400, firemakingXp())
            assertFalse(firemaking_queues.attempt in player.weakQueueList)
            assertMessageSent("The fire catches and the logs begin to burn.")

            advance()
            assertEquals(TEST_ORIGIN.translate(Direction.West), player.coords)
        }

    @Test
    fun GameTestState.`failed roll retries on the four tick cadence`() = runFiremakingTest { deps ->
        preparePlayer(xOffset = 8, level = 1)
        addLightingKit(objs.logs)
        random.next = 65
        random.then = 64
        random.then = Firemaking.MIN_FIRE_TICKS
        val fireCoords = player.coords

        lightInventory(deps, objs.logs.id)
        advance(Firemaking.ATTEMPT_TICKS)

        assertNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
        assertEquals(1, groundCount(deps, objs.logs.id, fireCoords))
        assertTrue(firemaking_queues.attempt in player.weakQueueList)
        assertEquals(0, firemakingXp())
        assertEquals(2597, client.filterIsInstance<SynthSound>().single().id)

        advance(Firemaking.ATTEMPT_TICKS)
        assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
        assertEquals(400, firemakingXp())
    }

    @Test
    fun GameTestState.`current boosted level satisfies the recipe boundary`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 16, level = 14)
            addLightingKit(objs.oak_logs)

            lightInventory(deps, objs.oak_logs.id)

            assertEquals(objs.oak_logs.id, player.inv[LOG_SLOT]?.id)
            assertFalse(firemaking_queues.attempt in player.weakQueueList)
            assertMessageSent("You need a Firemaking level of 15 to light these logs.")

            player.setCurrentLevel(stats.firemaking, 15)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS
            lightInventory(deps, objs.oak_logs.id)
            advance(Firemaking.ATTEMPT_TICKS)

            assertNotNull(deps.locRepo.findExact(TEST_ORIGIN.translateX(16), firemaking_locs.fire))
            assertEquals(600, firemakingXp())
        }

    @Test
    fun GameTestState.`members logs are rejected without consuming inventory`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 24, level = 99, members = false)
            addLightingKit(objs.magic_logs)

            lightInventory(deps, objs.magic_logs.id)

            assertEquals(objs.magic_logs.id, player.inv[LOG_SLOT]?.id)
            assertEquals(0, groundCount(deps, objs.magic_logs.id, player.coords))
            assertEquals(0, firemakingXp())
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState.`scenery rejection is atomic and preserves the inventory log`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 32, level = 99)
            addLightingKit(objs.logs)
            deps.locRepo.add(
                player.coords,
                firemaking_locs.fire,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.GroundDecor,
            )

            lightInventory(deps, objs.logs.id)

            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertEquals(0, groundCount(deps, objs.logs.id, player.coords))
            assertEquals(0, firemakingXp())
            assertFalse(firemaking_queues.attempt in player.weakQueueList)
            assertMessageSent(Firemaking.CANNOT_LIGHT_HERE)
        }

    @Test
    fun GameTestState.`clearing the weak queue interrupts safely and leaves the dropped log`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 40, level = 99)
            addLightingKit(objs.logs)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS
            val fireCoords = player.coords

            lightInventory(deps, objs.logs.id)
            player.weakQueueList.clear()
            advance(Firemaking.ATTEMPT_TICKS + 1)

            assertNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
            assertEquals(1, groundCount(deps, objs.logs.id, fireCoords))
            assertEquals(0, firemakingXp())
        }

    @Test
    fun GameTestState.`held use dispatch is symmetric and always consumes the log slot`() =
        runFiremakingTest { deps ->
            for ((index, selectedTinderbox) in listOf(true, false).withIndex()) {
                preparePlayer(xOffset = 48 + index * 8, level = 99)
                addLightingKit(objs.logs)
                random.next = 0
                random.then = Firemaking.MIN_FIRE_TICKS
                val tinderboxType = deps.objs.getValue(objs.tinderbox.id)
                val logType = deps.objs.getValue(objs.logs.id)
                val fireCoords = player.coords

                player.withProtectedAccess {
                    if (selectedTinderbox) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            tinderboxType,
                            TINDERBOX_SLOT,
                            logType,
                            LOG_SLOT,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            logType,
                            LOG_SLOT,
                            tinderboxType,
                            TINDERBOX_SLOT,
                        )
                    }
                }

                assertEquals(objs.tinderbox.id, player.inv[TINDERBOX_SLOT]?.id)
                assertNull(player.inv[LOG_SLOT])
                advance(Firemaking.ATTEMPT_TICKS)
                assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
            }
        }

    @Test
    fun GameTestState.`ground Light operation and tinderbox on ground route both dispatch`() =
        runFiremakingTest { deps ->
            for ((index, route) in GroundRoute.entries.withIndex()) {
                preparePlayer(xOffset = 64 + index * 8, level = 99)
                player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
                val fireCoords = player.coords
                val groundLog =
                    deps.objRepo.add(
                        objs.logs,
                        fireCoords,
                        duration = Int.MAX_VALUE,
                        receiver = player,
                    )
                random.next = 0
                random.then = Firemaking.MIN_FIRE_TICKS

                val event =
                    when (route) {
                        GroundRoute.Light ->
                            deps.objOps.opTrigger(
                                groundLog,
                                InteractionOp.Op4,
                                deps.objs.getValue(objs.logs.id),
                            )
                        GroundRoute.Tinderbox ->
                            deps.objT.opTrigger(
                                groundLog,
                                deps.objs.getValue(objs.logs.id),
                                components.inv_items,
                                TINDERBOX_SLOT,
                                objs.tinderbox,
                            )
                    }
                assertNotNull(event)
                player.withProtectedAccess { deps.events.publish(this, requireNotNull(event)) }
                assertTrue(firemaking_queues.attempt in player.weakQueueList)

                advance(Firemaking.ATTEMPT_TICKS)
                assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
                assertEquals(0, groundCount(deps, objs.logs.id, fireCoords))
                assertEquals(400, firemakingXp())
            }
        }

    @Test
    fun GameTestState.`ground routes require a tinderbox and preserve the log on failure`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 80, level = 99)
            val fireCoords = player.coords
            val groundLog =
                deps.objRepo.add(objs.logs, fireCoords, duration = Int.MAX_VALUE, receiver = player)

            player.withProtectedAccess {
                deps.firemaking.lightFromGround(this, groundLog, recipe(objs.logs.id))
            }

            assertEquals(1, groundCount(deps, objs.logs.id, fireCoords))
            assertFalse(firemaking_queues.attempt in player.weakQueueList)
            assertMessageSent("You need a tinderbox to light these logs.")
        }

    @Test
    fun GameTestState.`fire expiry at the minimum duration replaces it with ashes`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 88, level = 99)
            addLightingKit(objs.logs)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS
            val fireCoords = player.coords
            lightInventory(deps, objs.logs.id)
            advance(Firemaking.ATTEMPT_TICKS)

            advance(Firemaking.MIN_FIRE_TICKS - 2)
            assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
            assertEquals(0, groundCount(deps, objs.ashes.id, fireCoords))

            advance()
            assertNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
            assertEquals(1, groundCount(deps, objs.ashes.id, fireCoords))
        }

    @Test
    fun GameTestState.`post light movement falls back west east south north then stays`() =
        runFiremakingTest { deps ->
            for ((index, openDirection) in (Firemaking.MOVE_ORDER + null).withIndex()) {
                preparePlayer(xOffset = 96 + index * 8, level = 99)
                addLightingKit(objs.logs)
                random.next = 0
                random.then = Firemaking.MIN_FIRE_TICKS
                val fireCoords = player.coords
                for (direction in Firemaking.MOVE_ORDER) {
                    if (direction != openDirection) {
                        val blocked = fireCoords.translate(direction)
                        deps.collision.add(
                            blocked.x,
                            blocked.z,
                            blocked.level,
                            CollisionFlag.BLOCK_WALK,
                        )
                    }
                }

                lightInventory(deps, objs.logs.id)
                advance(Firemaking.ATTEMPT_TICKS + 1)

                val expected = openDirection?.let(fireCoords::translate) ?: fireCoords
                assertEquals(expected, player.coords, "open direction: $openDirection")
            }
        }

    @Test
    fun GameTestState.`same tile race creates one fire and leaves the losing log`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 136, level = 99)
            player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
            val fireCoords = player.coords
            val first =
                deps.objRepo.add(objs.logs, fireCoords, duration = Int.MAX_VALUE, receiver = player)
            val second =
                deps.objRepo.add(objs.logs, fireCoords, duration = Int.MAX_VALUE, receiver = player)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS

            player.withProtectedAccess {
                deps.firemaking.process(this, FiremakingRequest(first, recipe(objs.logs.id)))
            }
            player.withProtectedAccess {
                deps.firemaking.process(this, FiremakingRequest(second, recipe(objs.logs.id)))
            }

            assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
            assertEquals(1, groundCount(deps, objs.logs.id, fireCoords))
            assertEquals(400, firemakingXp())
            assertMessageSent(Firemaking.CANNOT_LIGHT_HERE)
        }

    @Test
    fun GameTestState.`all twelve ordinary recipes light with exact xp`() =
        runFiremakingTest { deps ->
            for ((index, recipe) in FiremakingRecipes.ordinary.withIndex()) {
                preparePlayer(xOffset = 144 + index * 8, level = recipe.level)
                addLightingKit(recipe.log)
                random.next = 0
                random.then = Firemaking.MAX_FIRE_TICKS
                val fireCoords = player.coords

                lightInventory(deps, recipe.log.id)
                advance(Firemaking.ATTEMPT_TICKS)

                assertNotNull(
                    deps.locRepo.findExact(fireCoords, firemaking_locs.fire),
                    recipe.toString(),
                )
                assertEquals((recipe.xp * 10).toInt(), firemakingXp(), recipe.toString())
            }
        }

    private fun GameTestScope.preparePlayer(xOffset: Int, level: Int, members: Boolean = true) {
        player.clearInv()
        player.observerUUID = 1L
        player.members = members
        player.setBaseLevel(stats.firemaking, level)
        player.setCurrentLevel(stats.firemaking, level)
        player.statMap.setXP(stats.firemaking, 0)
        player.weakQueueList.clear()
        player.placeAt(TEST_ORIGIN.translateX(xOffset))
        client.clear()
    }

    private fun GameTestScope.addLightingKit(log: ObjType) {
        player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
        player.inv[LOG_SLOT] = InvObj(log)
    }

    private fun GameTestScope.lightInventory(deps: Dependencies, logId: Int) {
        player.withProtectedAccess {
            deps.firemaking.lightFromInventory(this, LOG_SLOT, recipe(logId))
        }
    }

    private fun GameTestScope.groundCount(deps: Dependencies, objId: Int, coords: CoordGrid): Int =
        deps.objRepo.findAll(coords).count { it.type == objId }

    private fun GameTestScope.firemakingXp(): Int = player.statMap.getFineXP(stats.firemaking)

    private fun recipe(logId: Int): FiremakingRecipe =
        requireNotNull(FiremakingRecipes.byLog[logId])

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
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
        val synths: SynthTypeList,
        val heldU: HeldUInteractions,
        val objOps: ObjInteractions,
        val objT: ObjTInteractions,
        val events: EventBus,
        val locRepo: LocRepository,
        val objRepo: ObjRepository,
        val collision: CollisionFlagMap,
    )

    private data class RecipeContract(
        val id: Int,
        val level: Int,
        val xp: Double,
        val members: Boolean,
    )

    private enum class GroundRoute {
        Light,
        Tinderbox,
    }

    private companion object {
        const val TINDERBOX_SLOT = 0
        const val LOG_SLOT = 1
        val TEST_ORIGIN = CoordGrid(100, 100, 0)
    }
}
