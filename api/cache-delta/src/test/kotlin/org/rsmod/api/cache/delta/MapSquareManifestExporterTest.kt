package org.rsmod.api.cache.delta

import java.io.ByteArrayOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.square.MapSquareKey

class MapSquareManifestExporterTest {
    @Test
    fun `terrain and location entries retain collision-sensitive contracts`() {
        val terrainBytes =
            ByteArrayOutputStream()
                .apply {
                    write(byteArrayOf(0, 50))
                    repeat(4 * 64 * 64) { write(byteArrayOf(0, 0)) }
                }
                .toByteArray()
        val square = MapSquareKey(x = 50, z = 50)

        val terrain = MapSquareManifestExporter.terrainEntry(square, terrainBytes)
        assertEquals("map-terrain", terrain.domain)
        assertEquals("12850", terrain.key)
        assertEquals("16384", terrain.attributes["tiles"])
        assertEquals("1", terrain.attributes["blockedTiles"])
        assertEquals("0", terrain.attributes["linkBelowTiles"])
        assertEquals(listOf("client-contract-risk", "map-collision-risk"), terrain.riskTags)

        val locations =
            MapSquareManifestExporter.locationEntry(
                square,
                byteArrayOf(0),
                MapSquareManifestExporter.XteaRecord(
                    archive = 5,
                    group = 123,
                    name = "l50_50",
                    mapsquare = square.id,
                    key = listOf(1, 2, 3, 4),
                ),
            )
        assertEquals("map-locations", locations.domain)
        assertEquals("123", locations.attributes["archiveGroupId"])
        assertEquals("0", locations.attributes["placements"])
        assertEquals("[0, 0, 0, 0]", locations.attributes["placementsByLevel"])
        assertEquals("true", locations.attributes["xteaProtected"])
        assertTrue("map-collision-risk" in locations.riskTags)
    }
}
