package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class LampOilStillTest {
    @Test
    fun GameTestState.`cache 233 stills and supported lamp containers are pinned`() =
        runLampOilTest { deps ->
            assertEquals(5909, firemaking_locs.lampOilStillEmpty.id)
            assertEquals(5910, firemaking_locs.lampOilStillFull.id)
            assertEquals(22773, firemaking_locs.dorgeshLampStill.id)
            assertEquals(1939, objs.swamp_tar.id)
            assertEquals(
                listOf(4525 to 4522, 4535 to 4537, 4546 to 4548, 4700 to 4701, 20722 to 9064),
                LampOilContainers.all.map { it.empty.id to it.filled.id },
            )
            assertTrue(LampOilContainers.all.all { deps.objs[it.empty].members })
            assertTrue(LampOilContainers.all.all { deps.objs[it.filled].members })
            assertNotNull(deps.locs[firemaking_locs.lampOilStillEmpty])
            assertNotNull(deps.locs[firemaking_locs.lampOilStillFull])
            assertNotNull(deps.locs[firemaking_locs.dorgeshLampStill])
        }

    @Test
    fun GameTestState.`Rimmington still consumes one tar then fills every supported lamp`() =
        runLampOilTest { deps ->
            for ((index, recipe) in LampOilContainers.all.withIndex()) {
                preparePlayer()
                player.inv[TAR_SLOT] = InvObj(objs.swamp_tar, 2)
                player.inv[CONTAINER_SLOT] = InvObj(recipe.empty)
                val emptyStill = prepareStill(firemaking_locs.lampOilStillEmpty, index)

                player.withProtectedAccess {
                    assertTrue(deps.still.addTar(this, emptyStill, TAR_SLOT))
                }

                assertEquals(1, player.count(objs.swamp_tar))
                val fullStill = currentStill(emptyStill.coords, firemaking_locs.lampOilStillFull)
                player.withProtectedAccess {
                    assertTrue(deps.still.fill(this, fullStill, CONTAINER_SLOT, recipe))
                }

                assertEquals(recipe.filled.id, player.inv[CONTAINER_SLOT]?.id)
                assertNotNull(findLoc(emptyStill.coords, firemaking_locs.lampOilStillEmpty))
                assertEquals(0, player.statMap.getFineXP(stats.firemaking))
            }
        }

    @Test
    fun GameTestState.`Dorgesh still tracks one load without changing its location`() =
        runLampOilTest { deps ->
            preparePlayer()
            player.inv[TAR_SLOT] = InvObj(objs.swamp_tar, 2)
            player.inv[CONTAINER_SLOT] = InvObj(objs.empty_oil_lamp)
            val still = prepareStill(firemaking_locs.dorgeshLampStill, 20)

            player.withProtectedAccess { assertTrue(deps.still.addTar(this, still, TAR_SLOT)) }
            assertEquals(1, player.count(objs.swamp_tar))
            assertNotNull(findLoc(still.coords, firemaking_locs.dorgeshLampStill))

            player.withProtectedAccess { assertFalse(deps.still.addTar(this, still, TAR_SLOT)) }
            assertEquals(1, player.count(objs.swamp_tar))
            assertMessageSent("The still already contains lamp oil.")

            player.withProtectedAccess {
                assertTrue(
                    deps.still.fill(this, still, CONTAINER_SLOT, LampOilContainers.all.first())
                )
            }
            assertEquals(objs.unlit_oil_lamp.id, player.inv[CONTAINER_SLOT]?.id)

            player.inv[CONTAINER_SLOT] = InvObj(objs.empty_oil_lamp)
            player.withProtectedAccess {
                assertFalse(
                    deps.still.fill(this, still, CONTAINER_SLOT, LampOilContainers.all.first())
                )
            }
            assertEquals(objs.empty_oil_lamp.id, player.inv[CONTAINER_SLOT]?.id)
            assertMessageSent("You need to put swamp tar in the still first.")
        }

    @Test
    fun GameTestState.`lamp filling is safe with a full inventory`() = runLampOilTest { deps ->
        preparePlayer()
        player.inv[TAR_SLOT] = InvObj(objs.swamp_tar)
        val recipe = LampOilContainers.all.last()
        player.inv[CONTAINER_SLOT] = InvObj(recipe.empty)
        for (slot in 2 until player.inv.size) {
            player.inv[slot] = InvObj(objs.coins)
        }
        val emptyStill = prepareStill(firemaking_locs.lampOilStillEmpty, 30)
        player.withProtectedAccess { assertTrue(deps.still.addTar(this, emptyStill, TAR_SLOT)) }
        player.inv[TAR_SLOT] = InvObj(objs.coins)
        val fullStill = currentStill(emptyStill.coords, firemaking_locs.lampOilStillFull)

        player.withProtectedAccess {
            assertTrue(deps.still.fill(this, fullStill, CONTAINER_SLOT, recipe))
        }

        assertEquals(recipe.filled.id, player.inv[CONTAINER_SLOT]?.id)
        assertEquals(28, player.inv.count { it != null })
    }

    @Test
    fun GameTestState.`unloaded and stale still interactions do not consume items`() =
        runLampOilTest { deps ->
            preparePlayer()
            player.inv[TAR_SLOT] = InvObj(objs.swamp_tar, 2)
            player.inv[CONTAINER_SLOT] = InvObj(objs.empty_oil_lamp)
            val emptyStill = prepareStill(firemaking_locs.lampOilStillEmpty, 40)

            player.withProtectedAccess {
                assertFalse(
                    deps.still.fill(this, emptyStill, CONTAINER_SLOT, LampOilContainers.all.first())
                )
            }
            assertEquals(objs.empty_oil_lamp.id, player.inv[CONTAINER_SLOT]?.id)

            player.withProtectedAccess {
                assertTrue(deps.still.addTar(this, emptyStill, TAR_SLOT))
                assertFalse(deps.still.addTar(this, emptyStill, TAR_SLOT))
            }
            assertEquals(1, player.count(objs.swamp_tar))
        }

    @Test
    fun GameTestState.`members gate leaves still and inventory unchanged`() =
        runLampOilTest { deps ->
            preparePlayer(members = false)
            player.inv[TAR_SLOT] = InvObj(objs.swamp_tar)
            player.inv[CONTAINER_SLOT] = InvObj(objs.empty_oil_lamp)
            val emptyStill = prepareStill(firemaking_locs.lampOilStillEmpty, 50)

            player.withProtectedAccess {
                assertFalse(deps.still.addTar(this, emptyStill, TAR_SLOT))
            }
            assertEquals(1, player.count(objs.swamp_tar))
            assertNotNull(findLoc(emptyStill.coords, firemaking_locs.lampOilStillEmpty))
            assertMessageSent("You need to be on a members' world to make lamp oil.")
        }

    @Test
    fun GameTestState.`registered item-on-location routes perform both still stages`() =
        runLampOilTest { deps ->
            preparePlayer()
            player.inv[TAR_SLOT] = InvObj(objs.swamp_tar)
            player.inv[CONTAINER_SLOT] = InvObj(objs.empty_sapphire_lantern)
            val emptyStill = prepareStill(firemaking_locs.lampOilStillEmpty, 60)

            useOnStill(deps, emptyStill, TAR_SLOT, objs.swamp_tar)
            val fullStill = currentStill(emptyStill.coords, firemaking_locs.lampOilStillFull)
            useOnStill(deps, fullStill, CONTAINER_SLOT, objs.empty_sapphire_lantern)

            assertEquals(objs.unlit_sapphire_lantern.id, player.inv[CONTAINER_SLOT]?.id)
            assertNotNull(findLoc(emptyStill.coords, firemaking_locs.lampOilStillEmpty))
        }

    private fun GameTestScope.preparePlayer(members: Boolean = true) {
        player.clearInv()
        player.members = members
        player.statMap.setXP(stats.firemaking, 0)
        client.clear()
    }

    private fun GameTestScope.prepareStill(type: LocType, offset: Int): BoundLocInfo {
        val still =
            placeMapLoc(
                CoordGrid(2000 + offset * 4, 2000, 0),
                type,
                LocShape.CentrepieceStraight,
                LocAngle.West,
            )
        player.placeAt(still.coords.translateX(-1))
        return still
    }

    private fun GameTestScope.currentStill(coords: CoordGrid, type: LocType): BoundLocInfo {
        val info = requireNotNull(findLoc(coords, type))
        return BoundLocInfo(info, locTypes.getValue(type.id))
    }

    private fun GameTestScope.useOnStill(
        deps: Dependencies,
        still: BoundLocInfo,
        slot: Int,
        obj: ObjType,
    ) {
        player.withProtectedAccess {
            deps.locU.interactOp(
                this,
                still,
                still,
                locTypes.getValue(still.id),
                objTypes[obj],
                player.inv,
                slot,
            )
        }
    }

    private fun GameTestState.runLampOilTest(body: GameTestScope.(Dependencies) -> Unit) =
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
        val still: LampOilStill,
        val locU: LocUInteractions,
        val objs: ObjTypeList,
        val locs: LocTypeList,
    )

    private companion object {
        const val TAR_SLOT = 0
        const val CONTAINER_SLOT = 1
    }
}
