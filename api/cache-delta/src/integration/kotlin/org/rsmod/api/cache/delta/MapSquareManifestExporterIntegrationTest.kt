package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapSquareManifestExporterIntegrationTest {
    @Test
    fun `exports every cache 238 terrain and location map`() {
        val manifest =
            MapSquareManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                xteaFile = Path.of(".data/cache/xteas.json"),
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        assertEquals(
            mapOf("map-locations" to 2870, "map-terrain" to 2870),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(5740, manifest.entries.size)
        val terrainKeys = manifest.entries.filter { it.domain == "map-terrain" }.map { it.key }
        val locationKeys = manifest.entries.filter { it.domain == "map-locations" }.map { it.key }
        assertEquals(terrainKeys, locationKeys)
        assertEquals(2870, terrainKeys.distinct().size)
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })

        val terrain = manifest.entry("map-terrain", "12850")
        assertEquals("12850/0", terrain.attributes["archiveGroup"])
        assertEquals("16384", terrain.attributes["tiles"])
        assertEquals("641", terrain.attributes["blockedTiles"])
        assertEquals("40", terrain.attributes["linkBelowTiles"])
        assertEquals("1", terrain.attributes["trailingBytes"])
        val locations = manifest.entry("map-locations", "12850")
        assertEquals("12850/1", locations.attributes["archiveGroup"])
        assertEquals("false", locations.attributes["xteaProtected"])
        assertEquals("4726", locations.attributes["placements"])
        assertEquals("[3167, 1040, 422, 97]", locations.attributes["placementsByLevel"])

        val extendedTerrain = manifest.entry("map-terrain", "16457")
        assertEquals("16457/0", extendedTerrain.attributes["archiveGroup"])
        assertEquals("9564", extendedTerrain.attributes["trailingBytes"])
    }

    private fun CacheManifest.entry(domain: String, key: String): CacheManifestEntry =
        entries.single { it.domain == domain && it.key == key }
}
