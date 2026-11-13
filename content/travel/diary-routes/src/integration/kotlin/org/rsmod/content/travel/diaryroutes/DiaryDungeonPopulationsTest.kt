package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.rsmod.api.cache.map.npc.MapNpcDefinition
import org.rsmod.api.cache.map.npc.MapNpcListDecoder
import org.rsmod.api.cache.util.readOrNull
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DiaryDungeonPopulationsTest {
    private class OtherCellar @Inject constructor(private val types: LocTypeList) : PluginScript() {
        override fun ScriptContext.startup() {
            onOpLoc1(types.getValue(17384)) { telejump(it.loc.coords.translateX(2)) }
        }
    }

    @Test
    fun GameTestState
        .`a tile override keeps the ordinary type handler for other copies of the cellar ladder`() =
        runGameTest(DiaryDungeonPopulations::class, OtherCellar::class) {
            val cellar = placeMapLoc(CoordGrid(2892, 3507), locTypes.getValue(17384))
            val elsewhere = placeMapLoc(CoordGrid(3234, 3209), locTypes.getValue(17384))
            allocZoneCollision(CoordGrid(2893, 9907))
            player.teleport(cellar.coords.translateX(1))
            player.opLoc1(cellar)
            advance(3)
            assertEquals(CoordGrid(2893, 9907), player.coords)
            player.teleport(elsewhere.coords.translateX(1))
            player.opLoc1(elsewhere)
            advance(3)
            assertEquals(elsewhere.coords.translateX(2), player.coords)
        }

    @Test
    fun GameTestState
        .`packed populations fall inside the diary combat regions on the correct floor`() =
        runGameTest {
            data class Population(val id: Int, val count: Int, val xs: IntRange, val zs: IntRange)
            val targets =
                listOf(
                    Population(3449, 10, 2680..2760, 5044..5120),
                    Population(265, 1, 2896..2920, 9894..9920),
                    Population(2094, 23, 2816..2879, 9472..9535),
                    Population(509, 8, 2784..2824, 9280..9323),
                    Population(270, 3, 2620..2753, 9400..9508),
                    Population(272, 13, 2620..2753, 9400..9508),
                    Population(274, 4, 2620..2753, 9400..9508),
                    Population(679, 1, 2535..2535, 3431..3431),
                )
            Cache.open(Path.of(".data/cache/game")).use { cache ->
                for (target in targets) {
                    val spawns = mutableListOf<CoordGrid>()
                    for (mx in (target.xs.first / 64)..(target.xs.last / 64)) for (mz in
                        (target.zs.first / 64)..(target.zs.last / 64)) {
                        val group = cache.readOrNull(Js5Archives.MAPS, "n${mx}_${mz}") ?: continue
                        group.use {
                            for (packed in
                                MapNpcListDecoder.decode(it.toInlineBuf()).packedSpawns) {
                                val npc = MapNpcDefinition(packed)
                                val tile =
                                    CoordGrid(mx * 64 + npc.localX, mz * 64 + npc.localZ, npc.level)
                                if (
                                    npc.id == target.id &&
                                        tile.level == 0 &&
                                        tile.x in target.xs &&
                                        tile.z in target.zs
                                )
                                    spawns.add(tile)
                            }
                        }
                    }
                    assertEquals(
                        target.count,
                        spawns.distinct().size,
                        "native NPC ${target.id} at $spawns",
                    )
                    assertEquals(spawns.size, spawns.distinct().size, "duplicate $spawns")
                }
            }
        }

    @Test
    fun GameTestState
        .`Rasolo reissues the earned ring and the visible ladder works in both directions`() =
        runGameTest(DiaryDungeonPopulations::class) {
            player.members = true
            val trader = CoordGrid(2535, 3431)
            val surface = CoordGrid(2547, 3421)
            val dungeon = CoordGrid(2629, 5072)
            val npc = spawnNpc(trader, npcTypes.getValue(679))
            val down = placeMapLoc(surface, locTypes.getValue(6560))
            val up = placeMapLoc(dungeon, locTypes.getValue(6450))
            player.teleport(trader.translateX(1))
            player.opNpc1(npc)
            advance(3)
            assertEquals(0, player.count(objTypes.getValue(4657)))
            player.setVarBit(cacheTypes.varbits.getValue(383), 3)
            player.opNpc1(npc)
            advance(3)
            assertEquals(1, player.count(objTypes.getValue(4657)))
            player.withProtectedAccess { opHeld2(player.inv.indexOfFirst { it?.id == 4657 }) }
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(15152)])
            player.teleport(surface.translateX(1))
            player.opLoc1(down)
            advance(3)
            assertEquals(dungeon.translateX(1), player.coords)
            player.opLoc1(up)
            advance(3)
            assertEquals(surface.translateX(1), player.coords)
        }

    @Test
    fun GameTestState
        .`Heroes admission requires the completed quest and leads down to and through the dragon cage`() =
        runGameTest(DiaryDungeonPopulations::class) {
            player.members = true
            val door =
                placeMapLoc(
                    CoordGrid(2902, 3510),
                    locTypes.getValue(2624),
                    LocShape.WallStraight,
                    LocAngle.West,
                )
            val down =
                placeMapLoc(CoordGrid(2892, 3507), locTypes.getValue(17384), angle = LocAngle.South)
            val up =
                placeMapLoc(CoordGrid(2892, 9907), locTypes.getValue(17387), angle = LocAngle.North)
            val gate =
                placeMapLoc(
                    CoordGrid(2910, 9910),
                    locTypes.getValue(1568),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.teleport(door.coords)
            player.setVarp(cacheTypes.varps.getValue(188), 14)
            player.opLoc1(door)
            advance(3)
            assertEquals(door.coords, player.coords)
            player.setVarp(cacheTypes.varps.getValue(188), 15)
            player.opLoc1(door)
            advance(3)
            assertEquals(door.coords.translateX(-1), player.coords)
            player.teleport(down.coords.translateX(1))
            player.opLoc1(down)
            advance(3)
            assertEquals(up.coords.translateX(1), player.coords)
            player.teleport(gate.coords)
            player.opLoc1(gate)
            advance(3)
            assertEquals(gate.coords.translateZ(-1), player.coords)
            player.opLoc1(gate)
            advance(3)
            assertEquals(gate.coords, player.coords)
            player.teleport(up.coords.translateX(1))
            player.opLoc1(up)
            advance(3)
            assertEquals(down.coords.translateX(1), player.coords)
        }

    @Test
    fun GameTestState
        .`Pothole Search offers a descent to the Jogres and the handholds return to the beach`() =
        runGameTest(DiaryDungeonPopulations::class) {
            player.members = true
            val entrance = placeMapLoc(CoordGrid(2824, 3118), locTypes.getValue(2584))
            val exit =
                placeMapLoc(
                    CoordGrid(2830, 9522),
                    locTypes.getValue(2585),
                    LocShape.WallDecorStraightNoOffset,
                    LocAngle.West,
                )
            player.teleport(CoordGrid(2825, 3117))
            player.opLoc2(entrance)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance(2)
            assertEquals(CoordGrid(2831, 9522), player.coords)
            player.opLoc1(exit)
            advance(3)
            assertEquals(CoordGrid(2825, 3117), player.coords)
        }

    @Test
    fun GameTestState
        .`death wing access uses the cave bookcase lockpick mining boulder and strength gate`() =
        runGameTest(ShamanCaves::class) {
            val cave = placeMapLoc(CoordGrid(2782, 2935), locTypes.getValue(2902))
            val book =
                placeMapLoc(CoordGrid(2795, 9339), locTypes.getValue(2911), angle = LocAngle.North)
            val lock =
                placeMapLoc(
                    CoordGrid(2809, 9332),
                    locTypes.getValue(2913),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            val boulder = placeMapLoc(CoordGrid(2809, 9327), locTypes.getValue(2919))
            val strength =
                placeMapLoc(
                    CoordGrid(2809, 9314),
                    locTypes.getValue(2923),
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            for (tile in listOf(CoordGrid(2773, 9341), CoordGrid(2800, 9341))) allocZoneCollision(
                tile
            )
            player.members = true
            player.setCurrentLevel(stats.agility, 50)
            player.teleport(cave.coords.translateZ(-1))
            player.opLoc1(cave)
            advance(3)
            assertEquals(cave.coords.translateZ(-1), player.coords)
            player.setVarp(cacheTypes.varps.getValue(139), 1)
            player.opLoc1(cave)
            advance(3)
            assertEquals(CoordGrid(2773, 9341), player.coords)
            player.teleport(book.coords.translateX(-1))
            player.opLoc1(book)
            advance(3)
            assertEquals(CoordGrid(2800, 9341), player.coords)
            player.teleport(lock.coords)
            player.setCurrentLevel(stats.thieving, 50)
            player.opLoc1(lock)
            advance(3)
            assertEquals(lock.coords, player.coords)
            player.inv[0] = InvObj(objTypes.getValue(1523))
            player.opLoc1(lock)
            advance(3)
            assertEquals(lock.coords.translateZ(-1), player.coords)
            player.teleport(boulder.coords.translateZ(2))
            player.setCurrentLevel(stats.mining, 52)
            player.inv[1] = InvObj(objs.bronze_pickaxe)
            player.opLoc1(boulder)
            advance(7)
            assertEquals(boulder.coords.translateZ(-1), player.coords)
            assertEquals(1, player.count(objTypes.getValue(968)))
            player.teleport(strength.coords)
            player.opLoc1(strength)
            advance(3)
            assertEquals(strength.coords, player.coords)
            player.setCurrentLevel(stats.strength, 50)
            player.opLoc1(strength)
            advance(3)
            assertEquals(strength.coords.translateZ(-1), player.coords)
        }
}
