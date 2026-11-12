package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnrichedNpcSpawnManifestExporterIntegrationTest {
    @Test
    fun `exports every enriched cache 233 NPC placement group`() {
        val manifest =
            EnrichedNpcSpawnManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                xteaFile = Path.of(".data/cache/xteas.json"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "233.4-rsmod-enriched",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        val counts =
            manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount().toSortedMap()
        assertEquals(mapOf("map-npc-groups" to 6, "npc-spawns" to 585), counts)
        assertEquals(591, manifest.entries.size)
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })
        val spawns = manifest.entries.filter { it.domain == "npc-spawns" }
        assertEquals(136, spawns.map { it.attributes.getValue("npcId") }.distinct().size)
        assertEquals(460, spawns.count { it.attributes.getValue("attackable") == "true" })
        assertEquals(
            49,
            spawns
                .filter { it.attributes.getValue("attackable") == "true" }
                .map { it.attributes.getValue("npcId") }
                .distinct()
                .size,
        )
        assertTrue(spawns.all { it.symbol != null })
        assertEquals(
            mapOf(
                "12593" to "60",
                "12594" to "67",
                "12595" to "135",
                "12849" to "60",
                "12850" to "166",
                "12851" to "97",
            ),
            manifest.entries
                .filter { it.domain == "map-npc-groups" }
                .associate { it.key to it.attributes.getValue("placements") },
        )
    }
}
