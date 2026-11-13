package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.interact.LocUDefaultEvents
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.bosses.sporadic.SporadicBossController
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HesporiPlayTest {
    data class Dependencies
    @Inject
    constructor(
        val patches: FarmingPatchRepository,
        val codec: FarmingPatchCodec,
        val crops: FarmingCropCatalogue,
        val regions: RegionRegistry,
        val npcs: NpcRepository,
        val bosses: SporadicBossController,
    )

    @Test
    fun GameTestState
        .`cultivate Hespori enter a private fight and harvest the defeated plot once`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class, HesporiCave::class) {
            deps ->
            fun obj(name: String) = objTypes.values.single { it.internalName == name }
            fun loc(name: String) = locTypes.values.single { it.internalName == name }
            player.members = true
            player.setCurrentLevel(stats.farming, 65)
            val cave =
                placeMapLoc(CoordGrid(1231, 3728), loc("kebos_farming_guild_hespori_entrance"))
            player.teleport(CoordGrid(1234, 3727))
            player.opLoc1(cave)
            advance(4)
            assertEquals(CoordGrid(1244, 10083), player.coords)
            val base = loc("hespori_patch")
            val plot = placeMapLoc(CoordGrid(1246, 10086), base)
            placeMapLoc(CoordGrid(1242, 10081), loc("hespori_cave_exit"), LocShape[9], LocAngle[1])
            player.teleport(plot.coords.translateX(-1))
            player.inv[0] = InvObj(obj("rake"))
            repeat(3) {
                player.opLoc1(plot)
                advance(3)
            }
            val seed = obj("hespori_seed")
            player.inv[3] = InvObj(seed)
            player.inv[4] = InvObj(obj("dibber"))
            player.withProtectedAccess {
                eventBus.publish(this, LocUDefaultEvents.OpType(plot, plot, base, seed, 3))
            }
            val state = requireNotNull(deps.patches.find(player, deps.codec.patchKey(plot, base)))
            assertEquals("hespori", state.cropKey)
            deps.patches.advance(player, state, state.nextGrowthAt + 2 * 640 * 60_000L)
            val crop = requireNotNull(deps.crops["hespori"])
            deps.codec.sync(player, base, state, crop)
            assertEquals(
                "hespori_plant_fullygrown",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            player.inv[3] = InvObj(obj("spade"))
            player.opLoc1(plot)
            advance(4)
            val region = requireNotNull(deps.regions[player.coords])
            val privateTile = region.normal[plot.coords]
            val boss = deps.npcs.findAll(privateTile).single { it.type.internalName == "hespori" }
            assertEquals(4, deps.bosses.state(boss).activeMinions.size)
            assertNull(findLoc(privateTile, base))
            val exitType = loc("hespori_cave_exit_instance")
            val exit = requireNotNull(findLoc(region.normal[CoordGrid(1242, 10081)], exitType))
            player.teleport(exit.coords.translate(1, 1))
            player.opLoc2(BoundLocInfo(exit, exitType))
            advance(4)
            assertEquals(CoordGrid(1234, 3727), player.coords)
            assertEquals(FarmingCropStatus.Harvestable, state.status)
            assertTrue(deps.npcs.findAll(privateTile).none { it.id == boss.id })
            player.teleport(plot.coords.translateX(-1))
            player.opLoc1(plot)
            advance(4)
            val retry = requireNotNull(deps.regions[player.coords])
            val current = retry.normal[plot.coords]
            val second = deps.npcs.findAll(current).single { it.id == boss.id }
            eventBus.publish(NpcKillEvents.Credited(player, second))
            assertEquals(FarmingCropStatus.Dead, state.status)
            assertEquals(
                "hespori_plant_dead",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            val dead = requireNotNull(findLoc(current, base))
            val bound = BoundLocInfo(dead, base)
            player.teleport(current.translateX(-1))
            val xp = player.statMap.getFineXP(stats.farming)
            player.opLoc1(bound)
            advance(3)
            assertEquals(126000, player.statMap.getFineXP(stats.farming) - xp)
            assertNull(state.cropKey)
            assertTrue(
                listOf("attas_seed", "kronos_seed", "iasor_seed").sumOf {
                    player.inv.count(obj(it))
                } in 1..2
            )
            player.opLoc1(bound)
            advance(3)
            assertEquals(126000, player.statMap.getFineXP(stats.farming) - xp)
        }

    @Test
    fun GameTestState.`Anima seed has eight growth cycles and a final withered stage`() =
        runInjectedGameTest(Dependencies::class, null, FarmingScript::class) { deps ->
            player.members = true
            player.setCurrentLevel(stats.farming, 76)
            val base = locTypes.values.single { it.internalName == "farming_anima_patch_1" }
            val plot = placeMapLoc(CoordGrid(1231, 3722), base)
            player.teleport(plot.coords.translateX(-1))
            val state =
                deps.patches.getOrCreate(
                    player,
                    deps.codec.patchKey(plot, base),
                    FarmingPatchType.Anima,
                )
            state.weeds = 0
            val seed = objTypes.values.single { it.internalName == "attas_seed" }
            player.inv[0] = InvObj(seed)
            player.inv[1] = InvObj(objTypes.values.single { it.internalName == "dibber" })
            player.withProtectedAccess {
                eventBus.publish(this, LocUDefaultEvents.OpType(plot, plot, base, seed, 0))
            }
            val crop = requireNotNull(deps.crops["attas"])
            assertEquals(
                "anima_attas_seedling",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            val first = state.nextGrowthAt
            deps.patches.advance(player, state, first + 6 * 640 * 60_000L)
            assertEquals(
                "anima_attas_fullygrown_diseased",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
            deps.patches.advance(player, state, first + 7 * 640 * 60_000L)
            assertEquals(
                "anima_attas_fullygrown_dead",
                deps.codec.visualType(base, state, crop)?.internalName,
            )
        }
}
