package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnrichedVariableManifestExporterIntegrationTest {
    @Test
    fun `exports every rsmod enriched variable and controller definition`() {
        val manifest =
            EnrichedVariableManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "233.4-rsmod-enriched",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(
            mapOf(
                "server-controllers" to 1,
                "server-varcons" to 3,
                "server-varns" to 6,
                "server-varobjects" to 12,
            ),
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap(),
        )
        assertEquals(22, manifest.entries.size)
        assertTrue(manifest.entries.all { it.symbol != null })
        assertEquals(22, manifest.entries.map { it.domain to it.key }.toSet().size)
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })

        val bloodEssence = manifest.entry("server-varobjects", "65523")
        assertEquals("blood_essence_charges", bloodEssence.symbol)
        assertEquals("0", bloodEssence.attributes["startBit"])
        assertEquals("9", bloodEssence.attributes["endBit"])
        assertEquals("10", bloodEssence.attributes["width"])
        assertEquals("rsmod-build-definition", bloodEssence.attributes["source"])

        val firelighter = manifest.entry("server-varobjects", "65527")
        assertEquals("gnomish_firelighter_charges", firelighter.symbol)
        assertEquals("0", firelighter.attributes["startBit"])
        assertEquals("9", firelighter.attributes["endBit"])
        assertEquals("10", firelighter.attributes["width"])
        assertEquals("rsmod-build-definition", firelighter.attributes["source"])

        val treeTicks = manifest.entry("server-varcons", "0")
        assertEquals("woodcutting_tree_cut_ticks", treeTicks.symbol)

        val controller = manifest.entry("server-controllers", "0")
        assertEquals("woodcutting_tree_duration", controller.symbol)

        val aggressive = manifest.entry("server-varns", "0")
        assertEquals("aggressive_player", aggressive.symbol)
        assertEquals("true", aggressive.attributes["bitProtect"])
    }

    private fun CacheManifest.entry(domain: String, key: String): CacheManifestEntry =
        entries.single { it.domain == domain && it.key == key }
}
