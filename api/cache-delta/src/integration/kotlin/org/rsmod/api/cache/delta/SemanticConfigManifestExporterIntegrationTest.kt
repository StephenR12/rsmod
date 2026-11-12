package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticConfigManifestExporterIntegrationTest {
    @Test
    fun `cache 233 semantic configs export every cached definition`() {
        val cachePath = Path.of(".data/cache/vanilla")
        val manifest =
            SemanticConfigManifestExporter.export(
                cacheDirectory = cachePath,
                symbolDirectory = Path.of(".data/symbols"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(
            mapOf(
                "database-rows" to 6_655,
                "database-tables" to 117,
                "enums" to 5_625,
                "inventories" to 936,
                "locations" to 57_724,
                "npcs" to 14_799,
                "objects" to 31_172,
                "parameters" to 2_318,
                "sequences" to 12_521,
                "spot-animations" to 3_456,
                "structs" to 5_879,
                "varbits" to 18_031,
                "varclients" to 1_261,
                "varobjects" to 107,
                "varps" to 4_876,
            ),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(165_477, manifest.entries.size)
        assertEquals(
            manifest.entries.size,
            manifest.entries.map { it.domain to it.key }.toSet().size,
        )
        assertTrue(manifest.entries.none { it.domain == "projectiles" })
        assertEquals(16, SemanticConfigSymbols.load(Path.of(".data/symbols")).projectiles.size)

        val runEnergy = manifest.entry("varps", "173")
        assertEquals("option_run", runEnergy.symbol)
        assertTrue("client-contract-risk" in runEnergy.riskTags)

        val fairyRingScroll = manifest.entry("varclients", "0")
        assertEquals("fairyrings_scrollpos", fairyRingScroll.symbol)
        assertTrue("persistent" in fairyRingScroll.attributes)
        assertEquals(
            494,
            manifest.entries.count {
                it.domain == "varclients" && it.attributes["persistent"] == "true"
            },
        )
        assertEquals(
            767,
            manifest.entries.count {
                it.domain == "varclients" && it.attributes["persistent"] == "false"
            },
        )

        assertEquals(107, manifest.entries.count { it.domain == "varobjects" })
        assertTrue(manifest.entries.filter { it.domain == "varobjects" }.all { it.symbol == null })

        val equipment = manifest.entry("inventories", "94")
        assertEquals("worn", equipment.symbol)
        assertEquals("14", equipment.attributes["size"])

        val teleport = manifest.entry("sequences", "714")
        assertEquals("human_castteleport", teleport.symbol)
        assertTrue(teleport.attributes.getValue("delays").isNotBlank())
        assertTrue("animation-risk" in teleport.riskTags)

        val emptyEnumSentinel = manifest.entry("enums", "0")
        assertEquals("false", emptyEnumSentinel.attributes["decoded"])
        assertEquals("missing-key-or-value-literal", emptyEnumSentinel.attributes["reason"])
        assertTrue(emptyEnumSentinel.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")))
    }

    private fun CacheManifest.entry(domain: String, key: String): CacheManifestEntry =
        entries.single { it.domain == domain && it.key == key }
}
