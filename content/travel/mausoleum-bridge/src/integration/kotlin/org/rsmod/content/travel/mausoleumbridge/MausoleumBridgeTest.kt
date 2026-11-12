package org.rsmod.content.travel.mausoleumbridge

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.camera.CamLookAt
import net.rsprot.protocol.game.outgoing.camera.CamMoveTo
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.loc.LocLayerConstants

@Execution(ExecutionMode.SAME_THREAD)
class MausoleumBridgeTest {
    @Test
    fun GameTestState.`cache 233 bridge identities and private repair state are pinned`() =
        runBridgeTest { deps ->
            val expected =
                listOf(
                    MausoleumBridgeLocs.surface to Triple(5159, "Cross", 2171156116996093930L),
                    MausoleumBridgeLocs.mainlandUnrepaired to
                        Triple(5160, "Cross", 2171156116996093931L),
                    MausoleumBridgeLocs.islandUnrepaired to
                        Triple(57710, "Cross", 2171156116996146481L),
                    MausoleumBridgeLocs.mainlandBuild to
                        Triple(57711, "Build", 1700804965951581112L),
                    MausoleumBridgeLocs.islandBuild to Triple(57712, "Build", 1700804965951581113L),
                    MausoleumBridgeLocs.mainlandRepaired to
                        Triple(57713, "Cross", 2171156116996146484L),
                    MausoleumBridgeLocs.islandRepaired to
                        Triple(57714, "Cross", 2171156116996146485L),
                )
            for ((reference, contract) in expected) {
                val type = deps.locTypes[reference]
                assertEquals(contract.first, reference.id)
                assertEquals("Bridge", type.name)
                assertEquals(contract.second, type.op[0])
                assertEquals(contract.third, type.computeIdentityHash())
                assertEquals(LocShape.GroundDecor.id, type.shapes.single().toInt())
            }
            assertEquals(VarpLifetime.Perm, deps.varps[MausoleumBridgeVarps.repairState].scope)
            assertEquals(199, varbits.fenk_unlocked_cavern.id)
            assertEquals(807, seqs.human_longjump.id)
        }

    @Test
    fun GameTestState.`island side must be unlocked and is the only initial build option`() =
        runBridgeTest { deps ->
            prepare(deps, caveReached = false)
            assertPrivateLoc(MausoleumBridge.ISLAND_SIDE, MausoleumBridgeLocs.islandUnrepaired)
            assertPrivateLoc(MausoleumBridge.MAINLAND_SIDE, MausoleumBridgeLocs.mainlandUnrepaired)

            player.setVarBit(varbits.fenk_unlocked_cavern, 1)
            deps.bridge.sync(player)
            assertPrivateLoc(MausoleumBridge.ISLAND_SIDE, MausoleumBridgeLocs.islandBuild)
            assertPrivateLoc(MausoleumBridge.MAINLAND_SIDE, MausoleumBridgeLocs.mainlandUnrepaired)
        }

    @Test
    fun GameTestState.`private bridge overlays are accepted by the normal loc packet handler`() =
        runBridgeTest { deps ->
            prepare(deps)
            add(objs.hammer)
            add(objs.teak_plank, 6)
            add(objs.bronze_nails, 28)

            player.opLoc1(privateLoc(MausoleumBridgeLocs.islandBuild))
            advance(MausoleumBridge.BUILD_TICKS + 1)

            assertEquals(1, player.vars[MausoleumBridgeVarps.repairState])
            assertEquals(0, player.count(objs.teak_plank))
            assertMessageSent(MausoleumBridge.FIRST_SIDE_MESSAGE)
        }

    @Test
    fun GameTestState.`bridge state changes are sent immediately when both zones are visible`() =
        runBridgeTest { deps ->
            prepare(deps, state = 2)
            val mainlandZone = ZoneKey.from(MausoleumBridge.MAINLAND_SIDE)
            val islandZone = ZoneKey.from(MausoleumBridge.ISLAND_SIDE)
            player.buildArea = mainlandZone.toCoords()
            player.visibleZoneKeys.add(mainlandZone.packed)
            player.visibleZoneKeys.add(islandZone.packed)

            deps.bridge.sync(player)

            assertEquals(2, client.filterIsInstance<UpdateZonePartialFollows>().size)
            assertEquals(
                listOf(
                    MausoleumBridgeLocs.mainlandRepaired.id,
                    MausoleumBridgeLocs.islandRepaired.id,
                ),
                client.filterIsInstance<LocAddChangeV2>().map { it.id },
            )
        }

    @Test
    fun GameTestState.`first side consumes six planks and twenty eight nails then collapses`() =
        runBridgeTest { deps ->
            prepare(deps)
            add(objs.hammer)
            add(objs.teak_plank, 6)
            add(objs.bronze_nails, 28)
            val startXp = player.statMap.getFineXP(stats.construction)

            player.withProtectedAccess {
                deps.bridge.build(this, privateLoc(MausoleumBridgeLocs.islandBuild))
            }
            advance(MausoleumBridge.BUILD_TICKS - 1)
            assertEquals(0, player.vars[MausoleumBridgeVarps.repairState])
            assertEquals(6, player.count(objs.teak_plank))
            advance()

            assertEquals(1, player.vars[MausoleumBridgeVarps.repairState])
            assertEquals(0, player.count(objs.teak_plank))
            assertEquals(0, player.count(objs.bronze_nails))
            assertEquals(5500, player.statMap.getFineXP(stats.construction) - startXp)
            assertMessagesSent(MausoleumBridge.COLLAPSE_MESSAGE, MausoleumBridge.FIRST_SIDE_MESSAGE)
            assertPrivateLoc(MausoleumBridge.ISLAND_SIDE, MausoleumBridgeLocs.islandUnrepaired)
            assertPrivateLoc(MausoleumBridge.MAINLAND_SIDE, MausoleumBridgeLocs.mainlandBuild)
        }

    @Test
    fun GameTestState.`second side accepts mixed nails and completes eleven hundred xp shortcut`() =
        runBridgeTest { deps ->
            prepare(deps, state = 1)
            player.teleport(MausoleumBridge.MAINLAND_ARRIVAL)
            add(objs.imcando_hammer)
            add(objs.teak_plank, 6)
            add(objs.bronze_nails, 10)
            add(objs.rune_nails, 18)
            val startXp = player.statMap.getFineXP(stats.construction)

            player.withProtectedAccess {
                deps.bridge.build(this, privateLoc(MausoleumBridgeLocs.mainlandBuild))
            }
            advance(MausoleumBridge.BUILD_TICKS)

            assertEquals(2, player.vars[MausoleumBridgeVarps.repairState])
            assertEquals(0, player.count(objs.teak_plank))
            assertEquals(0, player.count(objs.bronze_nails))
            assertEquals(0, player.count(objs.rune_nails))
            assertEquals(5500, player.statMap.getFineXP(stats.construction) - startXp)
            assertMessageSent(MausoleumBridge.FINAL_SIDE_MESSAGE)
            assertPrivateLoc(MausoleumBridge.ISLAND_SIDE, MausoleumBridgeLocs.islandRepaired)
            assertPrivateLoc(MausoleumBridge.MAINLAND_SIDE, MausoleumBridgeLocs.mainlandRepaired)
        }

    @Test
    fun GameTestState.`construction hammer and exact material gates are atomic`() =
        runBridgeTest { deps ->
            prepare(deps)
            add(objs.hammer)
            add(objs.teak_plank, 6)
            add(objs.steel_nails, 28)
            player.setCurrentLevel(stats.construction, 58)
            player.withProtectedAccess {
                deps.bridge.build(this, privateLoc(MausoleumBridgeLocs.islandBuild))
            }
            assertMessageSent("You need a Construction level of 59 to build this bridge.")
            assertUnchangedMaterials()

            client.clear()
            player.setCurrentLevel(stats.construction, 59)
            player.inv[player.inv.indexOfFirst { it?.id == objs.hammer.id }] = null
            player.withProtectedAccess {
                deps.bridge.build(this, privateLoc(MausoleumBridgeLocs.islandBuild))
            }
            assertMessageSent("You need a hammer to build this bridge.")
            assertUnchangedMaterials()

            client.clear()
            add(objs.hammer)
            player.inv[player.inv.indexOfFirst { it?.id == objs.steel_nails.id }] =
                org.rsmod.game.inv.InvObj(objs.steel_nails, 27)
            player.withProtectedAccess {
                deps.bridge.build(this, privateLoc(MausoleumBridgeLocs.islandBuild))
            }
            assertMessageSent(MausoleumBridge.MISSING_MATERIALS_MESSAGE)
            assertEquals(6, player.count(objs.teak_plank))
            assertEquals(27, player.count(objs.steel_nails))
            assertEquals(0, player.vars[MausoleumBridgeVarps.repairState])
        }

    @Test
    fun GameTestState.`repaired crossing needs current agility sixty nine and awards zero xp`() =
        runBridgeTest { deps ->
            prepare(deps, state = 2)
            player.teleport(MausoleumBridge.MAINLAND_ARRIVAL)
            val crossing = privateLoc(MausoleumBridgeLocs.mainlandRepaired)
            player.setCurrentLevel(stats.agility, 68)
            player.withProtectedAccess { deps.bridge.cross(this, crossing) }
            assertMessageSent("You need an Agility level of 69 to use this shortcut.")
            assertEquals(MausoleumBridge.MAINLAND_ARRIVAL, player.coords)
            assertNull(player.pendingExactMove)

            client.clear()
            player.setCurrentLevel(stats.agility, 69)
            val startXp = player.statMap.getFineXP(stats.agility)
            player.withProtectedAccess { deps.bridge.cross(this, crossing) }
            assertEquals(MausoleumBridge.ISLAND_ARRIVAL, player.coords)
            assertEquals(seqs.human_longjump.id, player.pendingSequence.id)
            assertNotNull(player.pendingExactMove)
            assertEquals(startXp, player.statMap.getFineXP(stats.agility))
            advance(MausoleumBridge.JUMP_TICKS)
            assertEquals(startXp, player.statMap.getFineXP(stats.agility))
        }

    @Test
    fun GameTestState.`two distinct unrepaired surface attempts trigger mausoleum cinematic`() =
        runBridgeTest { deps ->
            prepare(deps)
            val first = surface(CoordGrid(3503, 3556), deps)
            val second = surface(CoordGrid(3503, 3557), deps)

            player.withProtectedAccess { deps.bridge.cross(this, first) }
            assertEquals(0, client.filterIsInstance<CamMoveTo>().size)
            player.withProtectedAccess { deps.bridge.cross(this, second) }
            assertEquals(1, client.filterIsInstance<CamMoveTo>().size)
            assertEquals(1, client.filterIsInstance<CamLookAt>().size)
            advance(6)
        }

    private fun GameTestScope.prepare(
        deps: Dependencies,
        caveReached: Boolean = true,
        state: Int = 0,
    ) {
        player.members = true
        player.setCurrentLevel(stats.construction, 59)
        player.setCurrentLevel(stats.agility, 69)
        player.setVarBit(varbits.fenk_unlocked_cavern, if (caveReached) 1 else 0)
        player.setVarp(MausoleumBridgeVarps.repairState, state)
        player.teleport(MausoleumBridge.ISLAND_ARRIVAL)
        allocZoneCollision(MausoleumBridge.MAINLAND_ARRIVAL)
        allocZoneCollision(MausoleumBridge.ISLAND_ARRIVAL)
        deps.bridge.sync(player)
        client.clear()
    }

    private fun GameTestScope.privateLoc(type: LocType): BoundLocInfo {
        val coords =
            when (type) {
                MausoleumBridgeLocs.islandBuild,
                MausoleumBridgeLocs.islandUnrepaired,
                MausoleumBridgeLocs.islandRepaired -> MausoleumBridge.ISLAND_SIDE
                else -> MausoleumBridge.MAINLAND_SIDE
            }
        val info = requireNotNull(player.privateLocs.findExactType(coords, type.id))
        return BoundLocInfo(info, locTypes[type])
    }

    private fun GameTestScope.surface(coords: CoordGrid, deps: Dependencies): BoundLocInfo {
        val entity =
            LocEntity(MausoleumBridgeLocs.surface.id, LocShape.GroundDecor.id, LocAngle.East.id)
        val info = LocInfo(LocLayerConstants.of(LocShape.GroundDecor.id), coords, entity)
        return BoundLocInfo(info, deps.locTypes[MausoleumBridgeLocs.surface])
    }

    private fun GameTestScope.assertPrivateLoc(coords: CoordGrid, type: LocType) {
        assertNotNull(player.privateLocs.findExactType(coords, type.id))
    }

    private fun GameTestScope.assertUnchangedMaterials() {
        assertEquals(6, player.count(objs.teak_plank))
        assertEquals(28, player.count(objs.steel_nails))
        assertEquals(0, player.vars[MausoleumBridgeVarps.repairState])
    }

    private fun GameTestScope.add(type: ObjType, count: Int = 1) {
        if (objTypes[type].isStackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(type, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(type)
        }
    }

    private fun GameTestState.runBridgeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            MausoleumBridgeScript::class,
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val bridge: MausoleumBridge,
        val locTypes: LocTypeList,
        val objTypes: ObjTypeList,
        val varps: VarpTypeList,
    )
}
