package org.rsmod.api.cache.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.cache.map.tile.MapTileSimpleDefinition

class MapBridgeLevelTest {
    @Test
    fun `ground flags on the Prifddinas bank ladder keep its ground level`() {
        val tiles = MapTileSimpleDefinition()
        tiles[54, 29, 0] = MapTileSimpleDefinition.LINK_BELOW
        assertEquals(listOf(0, 1, 2, 3), (0..3).map { tiles.sceneLevel(54, 29, it) })
    }

    @Test
    fun `a plane-one bridge lowers every floor in the same column only`() {
        val tiles = MapTileSimpleDefinition()
        tiles[12, 25, 1] = MapTileSimpleDefinition.LINK_BELOW
        assertEquals(listOf(-1, 0, 1, 2), (0..3).map { tiles.sceneLevel(12, 25, it) })
        assertEquals(2, tiles.sceneLevel(13, 25, 2))
    }
}
