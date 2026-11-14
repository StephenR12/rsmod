package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.api.cache.map.MapArchive
import org.rsmod.api.cache.map.MapFile
import org.rsmod.api.cache.map.GameMapDecoder
import org.rsmod.api.cache.map.area.MapAreaDecoder
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.area.AreaIndex
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

class RailwayTest {
    data class Deps @Inject constructor(val bits: VarBitTypeList, val areas: AreaIndex)

    @Test
    fun GameTestState
        .`walking into and remaining on the train carries the passenger in both directions`() =
        runInjectedGameTest(Deps::class, null, Railway::class) { deps ->
            // Game tests supply an empty index. Load the same four carriage polygons as packCache.
            Cache.open(Path.of(".data/cache/game")).use { cache ->
                val square = MapSquareKey(38, 86)
                val carriages =
                    MapArchive(cache).read(square, MapFile.Areas)!!.use {
                        MapAreaDecoder.decode(it.toInlineBuf())
                    }
                GameMapDecoder.putAreas(deps.areas, square, carriages)
            }
            player.members = true
            val door = placeMapLoc(CoordGrid(2695, 5277, 1), locTypes.getValue(23052))
            val platform = CoordGrid(2483, 5538)
            val carriage = CoordGrid(2480, 5538)
            val destination = CoordGrid(2440, 5522)
            for (tile in
                listOf(
                    platform,
                    carriage,
                    destination,
                    CoordGrid(2443, 5522),
                    CoordGrid(2488, 5536),
                )) allocZoneCollision(tile)
            player.teleport(CoordGrid(2696, 5277, 1))
            player.opLoc1(door)
            advance(2)
            assertEquals(CoordGrid(2696, 5277, 1), player.coords)
            player.setVarBit(deps.bits[3550]!!, 11)
            player.opLoc1(door)
            advance(2)
            assertEquals(CoordGrid(2488, 5536), player.coords)
            player.teleport(platform)
            advance(2)
            player.moveGameClick(carriage)
            advance(5)
            assertEquals(carriage, player.coords)
            player.moveGameClick(platform)
            advance(12)
            assertEquals(platform, player.coords)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.RideDorgeshTrain))
            player.moveGameClick(carriage)
            advance(15)
            assertEquals(destination, player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RideDorgeshTrain))
            player.moveGameClick(CoordGrid(2443, 5522))
            advance(15)
            assertEquals(platform, player.coords)
        }
}
