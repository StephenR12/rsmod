package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnrichedNpcSpawnManifestExporterIntegrationTest {
    @Test
    fun `exports every enriched cache 238 NPC placement group`() {
        val manifest =
            EnrichedNpcSpawnManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                xteaFile = Path.of(".data/cache/xteas.json"),
                symbolDirectory = Path.of(".data/symbols"),
                revision = "238-20260603-rsmod-enriched",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        val spawns = manifest.entries.filter { it.domain == "npc-spawns" }
        val groups = manifest.entries.filter { it.domain == "map-npc-groups" }
        // The original six maps remain populated as new skill and diary maps are packed.
        val original = mapOf("12593" to 60,"12594" to 67,"12595" to 135,"12849" to 60,"12850" to 166,"12851" to 97)
        val counts = spawns.groupingBy {it.attributes.getValue("mapSquareId")}.eachCount()
        for((square,minimum) in original)assertTrue(counts.getValue(square)>=minimum)
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })
        assertTrue(spawns.all { it.symbol != null })
        assertTrue(spawns.filter {it.attributes["attackable"]=="true"}.all {"combat-risk" in it.riskTags})
        // Read the packed map headers independently of the exporter, including new regions.
        val archive = Js5Archives.MAPS
        Cache.open(Path.of(".data/cache/game")).use { cache ->
            val expected = buildMap<String, Int> {
                for (group in cache.list(archive)) {
                    if (group.id < 65536 || !cache.exists(archive, group.id, 0)) continue
                    val data = cache.read(archive, group.id, 0)
                    try {
                        put((group.id - 65536).toString(), data.readUnsignedShort())
                    } finally {
                        data.release()
                    }
                }
            }
            assertEquals(expected,counts)
            assertEquals(expected,groups.associate {it.key to it.attributes.getValue("placements").toInt()})
            assertEquals(expected.size+expected.values.sum(),manifest.entries.size)
        }
    }
}
