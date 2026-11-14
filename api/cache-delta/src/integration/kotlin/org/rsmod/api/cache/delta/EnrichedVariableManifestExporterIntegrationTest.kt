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
                revision = "238-20260603-rsmod-enriched",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        val symbols = EnrichedVariableSymbols.load(Path.of(".data/symbols"))
        val expected = mapOf(
            "server-controllers" to symbols.controllers,
            "server-varcons" to symbols.varCons,
            "server-varns" to symbols.varns,
            "server-varobjects" to symbols.varObjects,
        )
        assertEquals(expected, manifest.entries.groupBy {it.domain}.mapValues { (_,entries) ->
            entries.associate {it.key.toInt() to it.symbol}
        })
        assertEquals(expected.values.sumOf {it.size},manifest.entries.size)
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
