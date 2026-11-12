package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapSquareManifestExporterIntegrationTest {
    @Test
    fun `exports every cache 233 terrain and location map`() {
        val manifest =
            MapSquareManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                xteaFile = Path.of(".data/cache/xteas.json"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(
            mapOf("map-locations" to 2266, "map-terrain" to 2266),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(4532, manifest.entries.size)
        val terrainKeys = manifest.entries.filter { it.domain == "map-terrain" }.map { it.key }
        val locationKeys = manifest.entries.filter { it.domain == "map-locations" }.map { it.key }
        assertEquals(terrainKeys, locationKeys)
        assertEquals(2266, terrainKeys.distinct().size)
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })

        val terrain = manifest.entry("map-terrain", "12850")
        assertEquals("m50_50", terrain.attributes["archiveGroup"])
        assertEquals("16384", terrain.attributes["tiles"])
        assertEquals("543", terrain.attributes["blockedTiles"])
        assertEquals("40", terrain.attributes["linkBelowTiles"])
        assertEquals("1", terrain.attributes["trailingBytes"])
        val locations = manifest.entry("map-locations", "12850")
        assertEquals("l50_50", locations.attributes["archiveGroup"])
        assertEquals("true", locations.attributes["xteaProtected"])
        assertEquals("4728", locations.attributes["placements"])
        assertEquals("[3169, 1040, 422, 97]", locations.attributes["placementsByLevel"])

        val extendedTerrain = manifest.entry("map-terrain", "16457")
        assertEquals("m64_73", extendedTerrain.attributes["archiveGroup"])
        assertEquals("9564", extendedTerrain.attributes["trailingBytes"])
    }

    private fun CacheManifest.entry(domain: String, key: String): CacheManifestEntry =
        entries.single { it.domain == domain && it.key == key }
}
