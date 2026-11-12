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
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianFiremakingTest {
    @Test
    fun GameTestState.`cache 233 barbarian recipes tools and audiovisual contracts are pinned`() =
        runFiremakingTest { deps ->
            assertEquals(
                listOf(
                    BarbarianRecipeContract(1511, 21, 40.0),
                    BarbarianRecipeContract(2862, 21, 40.0),
                    BarbarianRecipeContract(1521, 35, 60.0),
                    BarbarianRecipeContract(1519, 50, 90.0),
                    BarbarianRecipeContract(6333, 55, 105.0),
                    BarbarianRecipeContract(10810, 62, 125.0),
                    BarbarianRecipeContract(1517, 65, 135.0),
                    BarbarianRecipeContract(6332, 70, 157.5),
                    BarbarianRecipeContract(1515, 80, 202.5),
                    BarbarianRecipeContract(1513, 95, 303.8),
                    BarbarianRecipeContract(19669, 99, 350.0),
                    BarbarianRecipeContract(7404, 21, 50.0),
                    BarbarianRecipeContract(7405, 21, 50.0),
                    BarbarianRecipeContract(7406, 21, 50.0),
                    BarbarianRecipeContract(10329, 21, 50.0),
                    BarbarianRecipeContract(10328, 21, 50.0),
                ),
                FiremakingRecipes.barbarian.map {
                    BarbarianRecipeContract(it.log.id, Firemaking.barbarianLevel(it), it.xp)
                },
            )
            assertEquals(
                listOf(
                    BowContract(9705, 6713),
                    BowContract(841, 6714),
                    BowContract(839, 6714),
                    BowContract(843, 6715),
                    BowContract(845, 6715),
                    BowContract(849, 6716),
                    BowContract(847, 6716),
                    BowContract(853, 6717),
                    BowContract(851, 6717),
                    BowContract(857, 6718),
                    BowContract(855, 6718),
                    BowContract(861, 6719),
                    BowContract(859, 6719),
                    BowContract(12788, 6719),
                    BowContract(6724, 6720),
                ),
                BowFiremakingTools.all.map { BowContract(it.bow.id, it.sequence.id) },
            )
            assertTrue(BowFiremakingTools.all.all { deps.objs.containsKey(it.bow.id) })
            assertTrue(BowFiremakingTools.all.all { deps.seqs.containsKey(it.sequence.id) })
            assertEquals(1169, spotanims.brut_bow_firemaking.id)
            assertTrue(deps.types.spotanims.containsKey(spotanims.brut_bow_firemaking.id))
            assertEquals(3759, varbits.brut_fire.id)
            assertEquals(4..5, deps.types.varbits[varbits.brut_fire].bits)
        }

    @Test
    fun GameTestState.`bow held use dispatch is symmetric and preserves the bow`() =
        runFiremakingTest { deps ->
            for ((index, selectedBow) in listOf(true, false).withIndex()) {
                preparePlayer(xOffset = index * 8, firemaking = 21)
                setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
                addBowKit(objs.shortbow, objs.logs)
                random.next = 0
                random.then = Firemaking.MIN_FIRE_TICKS
                val bowType = deps.objs.getValue(objs.shortbow.id)
                val logType = deps.objs.getValue(objs.logs.id)
                val fireCoords = player.coords

                player.withProtectedAccess {
                    if (selectedBow) {
                        deps.heldU.interact(this, player.inv, bowType, BOW_SLOT, logType, LOG_SLOT)
                    } else {
                        deps.heldU.interact(this, player.inv, logType, LOG_SLOT, bowType, BOW_SLOT)
                    }
                }

                assertEquals(objs.shortbow.id, player.inv[BOW_SLOT]?.id)
                assertNull(player.inv[LOG_SLOT])
                assertEquals(6714, player.pendingSequence.id)
                assertEquals(1169, EntitySpotanim(player.pendingSpotanims.getLong(0)).id)
                advance(Firemaking.ATTEMPT_TICKS)
                assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
                assertEquals(400, firemakingXp())
            }
        }

    @Test
    fun GameTestState.`training task requires unboosted levels and only accepts oak`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 16, firemaking = 99, baseFiremaking = 34, crafting = 99)
            setProgress(Firemaking.BARBARIAN_TASK_ASSIGNED)
            addBowKit(objs.shortbow, objs.oak_logs)
            lightBow(deps, objs.oak_logs, objs.shortbow)
            assertEquals(objs.oak_logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent(
                "You need Firemaking level 35 and Crafting level 11 to begin this training."
            )

            preparePlayer(xOffset = 24, firemaking = 99, baseFiremaking = 35, crafting = 10)
            setProgress(Firemaking.BARBARIAN_TASK_ASSIGNED)
            addBowKit(objs.shortbow, objs.oak_logs)
            lightBow(deps, objs.oak_logs, objs.shortbow)
            assertEquals(objs.oak_logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent(
                "You need Firemaking level 35 and Crafting level 11 to begin this training."
            )

            preparePlayer(xOffset = 32, firemaking = 99, baseFiremaking = 35, crafting = 11)
            setProgress(Firemaking.BARBARIAN_TASK_ASSIGNED)
            addBowKit(objs.shortbow, objs.logs)
            lightBow(deps, objs.logs, objs.shortbow)
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("Otto asked you to light oak logs with a bow.")
        }

    @Test
    fun GameTestState.`first successful oak fire advances only to Otto follow-up state`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 40, firemaking = 35, crafting = 11)
            setProgress(Firemaking.BARBARIAN_TASK_ASSIGNED)
            addBowKit(objs.oak_shortbow, objs.oak_logs)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS

            lightBow(deps, objs.oak_logs, objs.oak_shortbow)

            assertEquals(Firemaking.BARBARIAN_TASK_ASSIGNED, progress())
            advance(Firemaking.ATTEMPT_TICKS)
            assertEquals(Firemaking.BARBARIAN_FIRE_LIT, progress())
            assertEquals(600, firemakingXp())

            preparePlayer(xOffset = 48, firemaking = 99, crafting = 99)
            setProgress(Firemaking.BARBARIAN_FIRE_LIT)
            addBowKit(objs.shortbow, objs.logs)
            lightBow(deps, objs.logs, objs.shortbow)
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("You need to complete Otto's barbarian Firemaking training first.")
        }

    @Test
    fun GameTestState.`locked free world and unsupported log routes are atomic`() =
        runFiremakingTest { deps ->
            preparePlayer(xOffset = 56, firemaking = 99)
            setProgress(0)
            addBowKit(objs.shortbow, objs.logs)
            lightBow(deps, objs.logs, objs.shortbow)
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertFalse(firemaking_queues.attempt in player.weakQueueList)

            preparePlayer(xOffset = 64, firemaking = 99, members = false)
            setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
            addBowKit(objs.shortbow, objs.logs)
            lightBow(deps, objs.logs, objs.shortbow)
            assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("You need to be on a members' world to light logs with a bow.")

            preparePlayer(xOffset = 72, firemaking = 99)
            setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
            addBowKit(objs.shortbow, objs.blisterwood_logs)
            lightBow(deps, objs.blisterwood_logs, objs.shortbow)
            assertEquals(objs.blisterwood_logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("You can't light these logs with a bow.")
        }

    @Test
    fun GameTestState.`completed training uses the boosted bow level boundary`() =
        runFiremakingTest { deps ->
            val oak = requireNotNull(FiremakingRecipes.byLog[objs.oak_logs.id])
            assertEquals(35, Firemaking.barbarianLevel(oak))
            preparePlayer(xOffset = 80, firemaking = 34, baseFiremaking = 34)
            setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
            addBowKit(objs.shortbow, objs.oak_logs)

            lightBow(deps, objs.oak_logs, objs.shortbow)
            assertEquals(objs.oak_logs.id, player.inv[LOG_SLOT]?.id)
            assertMessageSent("You need a Firemaking level of 35 to light these logs with a bow.")

            player.setCurrentLevel(stats.firemaking, 35)
            random.next = 0
            random.then = Firemaking.MIN_FIRE_TICKS
            lightBow(deps, objs.oak_logs, objs.shortbow)
            advance(Firemaking.ATTEMPT_TICKS)
            assertEquals(600, firemakingXp())
        }

    @Test
    fun GameTestState.`all eligible bows use their cache animation and shared retry cadence`() =
        runFiremakingTest { deps ->
            for ((index, tool) in BowFiremakingTools.all.withIndex()) {
                preparePlayer(xOffset = 88 + index * 8, firemaking = 21)
                setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
                addBowKit(tool.bow, objs.logs)
                random.next = if (index == 0) 255 else 0
                if (index == 0) {
                    random.then = 64
                }
                random.then = Firemaking.MIN_FIRE_TICKS
                val fireCoords = player.coords

                lightBow(deps, objs.logs, tool.bow)

                assertEquals(tool.sequence.id, player.pendingSequence.id, tool.toString())
                assertEquals(
                    spotanims.brut_bow_firemaking.id,
                    EntitySpotanim(player.pendingSpotanims.getLong(0)).id,
                    tool.toString(),
                )
                assertTrue(client.filterIsInstance<SynthSound>().isEmpty())
                advance(Firemaking.ATTEMPT_TICKS)
                if (index == 0) {
                    assertNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
                    assertTrue(firemaking_queues.attempt in player.weakQueueList)
                    advance(Firemaking.ATTEMPT_TICKS)
                }
                assertNotNull(deps.locRepo.findExact(fireCoords, firemaking_locs.fire))
                assertEquals(tool.bow.id, player.inv[BOW_SLOT]?.id)
                assertEquals(400, firemakingXp())
            }
        }

    @Test
    fun GameTestState.`all sixteen cutoff bow recipes award the tinderbox xp`() =
        runFiremakingTest { deps ->
            for ((index, recipe) in FiremakingRecipes.barbarian.withIndex()) {
                val requiredLevel = Firemaking.barbarianLevel(recipe)
                preparePlayer(xOffset = 216 + index * 8, firemaking = requiredLevel)
                setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
                addBowKit(objs.shortbow, recipe.log)
                if (recipe.guaranteed) {
                    random.next = Firemaking.MAX_FIRE_TICKS
                } else {
                    random.next = 0
                    random.then = Firemaking.MAX_FIRE_TICKS
                }
                val fireCoords = player.coords

                lightBow(deps, recipe.log, objs.shortbow)
                advance(Firemaking.ATTEMPT_TICKS)

                assertNotNull(deps.locRepo.findExact(fireCoords, recipe.fire), recipe.toString())
                assertEquals((recipe.xp * 10).toInt(), firemakingXp(), recipe.toString())
            }
        }

    @Test
    fun GameTestState.`known invalid bows have no barbarian firemaking handler`() =
        runFiremakingTest { deps ->
            for ((index, invalidBowId) in listOf(11235, 20997, 22547, 23357).withIndex()) {
                preparePlayer(xOffset = 304 + index * 8, firemaking = 99)
                setProgress(Firemaking.BARBARIAN_TRAINING_COMPLETE)
                val invalidBow = deps.objs.getValue(invalidBowId)
                val log = deps.objs.getValue(objs.logs.id)
                player.inv[BOW_SLOT] = InvObj(invalidBow)
                player.inv[LOG_SLOT] = InvObj(objs.logs)

                player.withProtectedAccess {
                    deps.heldU.interact(this, player.inv, invalidBow, BOW_SLOT, log, LOG_SLOT)
                }

                assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
                assertFalse(firemaking_queues.attempt in player.weakQueueList)
            }
        }

    private fun GameTestScope.preparePlayer(
        xOffset: Int,
        firemaking: Int,
        baseFiremaking: Int = firemaking,
        crafting: Int = 99,
        members: Boolean = true,
    ) {
        player.clearInv()
        player.observerUUID = 1L
        player.members = members
        player.setBaseLevel(stats.firemaking, baseFiremaking)
        player.setCurrentLevel(stats.firemaking, firemaking)
        player.setBaseLevel(stats.crafting, crafting)
        player.setCurrentLevel(stats.crafting, crafting)
        player.statMap.setXP(stats.firemaking, 0)
        player.weakQueueList.clear()
        player.pendingSpotanims.clear()
        player.placeAt(TEST_ORIGIN.translateX(xOffset))
        client.clear()
    }

    private fun GameTestScope.addBowKit(bow: ObjType, log: ObjType) {
        player.inv[BOW_SLOT] = InvObj(bow)
        player.inv[LOG_SLOT] = InvObj(log)
    }

    private fun GameTestScope.lightBow(deps: Dependencies, log: ObjType, bow: ObjType) {
        val recipe = requireNotNull(FiremakingRecipes.byLog[log.id])
        val tool = requireNotNull(BowFiremakingTools.byBow[bow.id])
        player.withProtectedAccess {
            deps.firemaking.lightFromBow(this, LOG_SLOT, BOW_SLOT, recipe, tool)
        }
    }

    private fun GameTestScope.setProgress(value: Int) {
        player.withProtectedAccess { vars[varbits.brut_fire] = value }
    }

    private fun GameTestScope.progress(): Int = player.vars[varbits.brut_fire]

    private fun GameTestScope.firemakingXp(): Int = player.statMap.getFineXP(stats.firemaking)

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
        val types: TypeListMap,
        val heldU: HeldUInteractions,
        val locRepo: LocRepository,
        val objRepo: ObjRepository,
    )

    private data class BarbarianRecipeContract(val id: Int, val level: Int, val xp: Double)

    private data class BowContract(val id: Int, val sequence: Int)

    private companion object {
        const val BOW_SLOT = 0
        const val LOG_SLOT = 1
        val TEST_ORIGIN = CoordGrid(500, 500, 0)
    }
}
