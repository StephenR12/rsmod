package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FiremakingPlacementTest {
    @Test
    fun GameTestState.`multi tile scenery rejects every covered tile across a zone boundary`() =
        runPlacementTest { deps ->
            val scenery = findLocTypes { it.width >= 2 && it.length >= 2 }.first()
            val origin = CoordGrid(799, 799, 0)
            val covered = origin.translate(1, 1)
            val outside = origin.translate(scenery.width, scenery.length)
            assertTrue(
                deps.locRepo
                    .add(
                        origin,
                        scenery,
                        Int.MAX_VALUE,
                        LocAngle.West,
                        LocShape.CentrepieceStraight,
                    )
                    .id == scenery.id
            )

            assertEquals(Firemaking.CANNOT_LIGHT_HERE, deps.placement.rejectionAt(origin))
            assertEquals(Firemaking.CANNOT_LIGHT_HERE, deps.placement.rejectionAt(covered))
            assertNull(deps.placement.rejectionAt(outside))
        }

    @Test
    fun GameTestState.`all generic closed door families block each model swing tile`() =
        runPlacementTest { deps ->
            val groups =
                listOf(
                    content.closed_single_door,
                    content.closed_left_door,
                    content.closed_right_door,
                )
            var index = 0
            for (group in groups) {
                val door = findLocType(group)
                for (shape in listOf(LocShape.WallStraight, LocShape.WallDiagonal)) {
                    for (angle in LocAngle.entries) {
                        val origin = CoordGrid(900 + index * 8, 900, 0)
                        deps.locRepo.add(origin, door, Int.MAX_VALUE, angle, shape)
                        val swingTile = DoorTranslations.translateOpen(origin, shape, angle)

                        assertEquals(
                            Firemaking.CANNOT_LIGHT_HERE,
                            deps.placement.rejectionAt(swingTile),
                            "$group, $shape, $angle",
                        )
                        index++
                    }
                }
            }
        }

    @Test
    fun GameTestState.`ordinary walls roofs and opened doors do not create false positives`() =
        runPlacementTest { deps ->
            val ordinary = findLocTypes { it.width == 1 && it.length == 1 }.first()
            val wallTile = CoordGrid(1200, 1200, 0)
            val roofTile = CoordGrid(1208, 1200, 0)
            deps.locRepo.add(
                wallTile,
                ordinary,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.WallStraight,
            )
            deps.locRepo.add(roofTile, ordinary, Int.MAX_VALUE, LocAngle.West, LocShape.RoofFlat)

            val opened = findLocType(content.opened_single_door)
            val openedTile = CoordGrid(1216, 1200, 0)
            deps.locRepo.add(
                openedTile,
                opened,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.WallDiagonal,
            )

            assertNull(deps.placement.rejectionAt(wallTile))
            assertNull(deps.placement.rejectionAt(roofTile))
            assertNull(deps.placement.rejectionAt(openedTile))
        }

    @Test
    fun GameTestState.`published polygons reject atomically with their exact messages`() =
        runPlacementTest { deps ->
            val cases =
                listOf(
                    CoordGrid(3000, 3430, 0) to
                        "The dwarves won't be happy if you light a fire here.",
                    CoordGrid(3120, 3400, 0) to
                        "A colourful magical force won't let you light a fire here.",
                    CoordGrid(2450, 3040, 0) to "The ogres won't be happy if you light a fire here.",
                )
            for ((coords, message) in cases) {
                preparePlayer(coords)
                player.withProtectedAccess {
                    deps.firemaking.lightFromInventory(
                        this,
                        LOG_SLOT,
                        requireNotNull(FiremakingRecipes.byLog[objs.logs.id]),
                    )
                }

                assertEquals(objs.logs.id, player.inv[LOG_SLOT]?.id)
                assertFalse(firemaking_queues.attempt in player.weakQueueList)
                assertMessageSent(message)
            }
        }

    private fun GameTestScope.preparePlayer(coords: CoordGrid) {
        player.clearInv()
        player.observerUUID = 1L
        player.members = true
        player.setBaseLevel(stats.firemaking, 99)
        player.setCurrentLevel(stats.firemaking, 99)
        player.statMap.setXP(stats.firemaking, 0)
        player.weakQueueList.clear()
        player.placeAt(coords)
        player.inv[TINDERBOX_SLOT] = InvObj(objs.tinderbox)
        player.inv[LOG_SLOT] = InvObj(objs.logs)
        client.clear()
    }

    private fun GameTestState.runPlacementTest(body: GameTestScope.(Dependencies) -> Unit) =
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
        val placement: FiremakingPlacement,
        val locRepo: LocRepository,
    )

    private companion object {
        const val TINDERBOX_SLOT = 0
        const val LOG_SLOT = 1
    }
}
