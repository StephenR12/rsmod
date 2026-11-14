package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SemanticConfigManifestExporterIntegrationTest {
    @Test
    fun `cache 238 semantic configs export every cached definition`() {
        val cachePath = Path.of(".data/cache/vanilla")
        val manifest =
            SemanticConfigManifestExporter.export(
                cacheDirectory = cachePath,
                symbolDirectory = Path.of(".data/symbols"),
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        assertEquals(
            mapOf(
                "database-rows" to 16_211,
                "database-tables" to 233,
                "enums" to 5_864,
                "inventories" to 1_022,
                "locations" to 60_710,
                "npcs" to 15_712,
                "objects" to 33_372,
                "parameters" to 2_569,
                "sequences" to 13_902,
                "spot-animations" to 3_792,
                "structs" to 3_974,
                "varbits" to 18_714,
                "varclients" to 1_377,
                "varobjects" to 122,
                "varps" to 5_588,
            ),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(183_162, manifest.entries.size)
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
            530,
            manifest.entries.count {
                it.domain == "varclients" && it.attributes["persistent"] == "true"
            },
        )
        assertEquals(
            847,
            manifest.entries.count {
                it.domain == "varclients" && it.attributes["persistent"] == "false"
            },
        )

        assertEquals(122, manifest.entries.count { it.domain == "varobjects" })
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
