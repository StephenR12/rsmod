package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreConfigManifestExporterIntegrationTest {
    @Test
    fun `cache 238 core definitions export with typed symbols and interaction contracts`() {
        val manifest =
            CoreConfigManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        assertEquals(
            mapOf("locations" to 60_710, "npcs" to 15_712, "objects" to 33_372),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(109_794, manifest.entries.size)
        assertEquals(
            manifest.entries.size,
            manifest.entries.map { it.domain to it.key }.toSet().size,
        )

        val coins = manifest.entry("objects", "995")
        assertEquals("coins", coins.symbol)
        assertEquals("Coins", coins.attributes["name"])
        assertTrue(coins.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")))

        val rogue = manifest.entry("npcs", "526")
        assertEquals("rogue", rogue.symbol)
        assertTrue(rogue.attributes.values.any { it == "Pickpocket" })

        val chaosAltar = manifest.entry("locations", "411")
        assertEquals("chaosaltar", chaosAltar.symbol)
        assertTrue(chaosAltar.attributes.values.any { it.startsWith("Pray") })
        assertTrue("map-collision-risk" in chaosAltar.riskTags)
    }

    private fun CacheManifest.entry(domain: String, key: String): CacheManifestEntry =
        entries.single { it.domain == domain && it.key == key }
}
