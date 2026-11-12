package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoreConfigManifestExporterIntegrationTest {
    @Test
    fun `cache 233 core definitions export with typed symbols and interaction contracts`() {
        val manifest =
            CoreConfigManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(
            mapOf("locations" to 57_724, "npcs" to 14_799, "objects" to 31_172),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(103_695, manifest.entries.size)
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
