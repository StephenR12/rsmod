package org.rsmod.api.cache.map

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openrs2.buffer.use
import org.openrs2.cache.Cache
import org.rsmod.api.cache.map.loc.MapLocDefinition
import org.rsmod.api.cache.map.loc.MapLocListDecoder
import org.rsmod.api.cache.map.tile.MapTileDecoder
import org.rsmod.api.cache.util.toInlineBuf
import org.rsmod.map.square.MapSquareKey

class NativeMapArchiveTest {
    @Test
    fun `loads native revision 238 Lumbridge terrain and populated zones`() {
        Cache.open(Path.of(".data/cache/vanilla")).use { cache ->
            val archive = MapArchive(cache)
            val square = MapSquareKey(50, 50)
            assertTrue(archive.numeric)
            val squares = archive.nativeSquares()
            assertEquals(2870, squares.size)
            assertTrue(square in squares)
            assertFalse(MapSquareKey(25287) in squares)
            archive.read(square, MapFile.Terrain)!!.use {
                assertEquals(60239, it.readableBytes())
                val terrain = MapTileDecoder.decode(it)
                assertTrue(terrain.rules.isNotEmpty())
                assertEquals(1, it.readableBytes()) // Native trailing environment byte.
            }
            archive.read(square, MapFile.Locations)!!.use {
                assertEquals(11157, it.readableBytes())
                val locs = MapLocListDecoder.decode(it.toInlineBuf()).spawns.map { packed -> MapLocDefinition(packed) }
                assertEquals(4726, locs.size)
                assertEquals(
                    64,
                    locs.map { (it.localX shr 3) to (it.localZ shr 3) }.distinct().size,
                )
            }
        }
    }
}
