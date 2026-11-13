package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.openrs2.cache.Cache
import org.rsmod.api.cache.Js5Archives
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CacheArchiveIndexManifestExporterIntegrationTest {
    @Test
    fun `exports every cache archive group index contract`() {
        val manifest =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        assertEquals(116_016, manifest.entries.size)
        assertEquals(
            manifest.entries.size,
            manifest.entries.map { it.domain to it.key }.toSet().size,
        )
        assertTrue(manifest.entries.all { it.domain == "cache-archive-index" })
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })
        assertTrue(manifest.entries.all { it.symbol == null })
        assertTrue(manifest.entries.all { it.attributes["fileCount"]!!.toInt() > 0 })
        assertTrue(manifest.entries.all { it.riskTags == it.riskTags.sorted() })

        val archives =
            manifest.entries.groupingBy { it.attributes.getValue("archiveId") }.eachCount()
        assertEquals(
            mapOf(
                "0" to 9_596,
                "1" to 2_464,
                "2" to 37,
                "3" to 925,
                "4" to 10_749,
                "5" to 14_437,
                "6" to 830,
                "7" to 57_380,
                "8" to 6_415,
                "9" to 1,
                "10" to 4,
                "11" to 311,
                "12" to 8_093,
                "13" to 21,
                "14" to 501,
                "15" to 155,
                "17" to 1,
                "18" to 1_666,
                "19" to 49,
                "20" to 1_666,
                "21" to 72,
                "22" to 629,
                "24" to 14,
            ),
            archives,
        )
        assertEquals(
            640_720,
            manifest.entries.sumOf { it.attributes.getValue("fileCount").toInt() },
        )

        val lumbridge = manifest.entries.single { it.key == "5:12850" }
        assertEquals("2906976476", lumbridge.attributes["groupChecksum"])
        assertEquals("-838284128", lumbridge.attributes["groupNameHash"])
        assertEquals(
            "sha256:065e70a11be0fa72db530999794ec7d0b2d0ebb42c3b065f64a9c1b6354b0602",
            lumbridge.fingerprint,
        )
    }

    @Test
    fun `rsmod enrichment changes only expected archive groups`() {
        val vanilla =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                revision = "233.4",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )
        val enriched =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                revision = "233.4-rsmod-enriched",
                archive = 2317,
                capturedAt = "2025-10-01T10:45:07.114859Z",
            )

        val delta = CacheDeltaEngine.diff(vanilla, enriched)
        val maps = Js5Archives.MAPS
        fun mapIds(directory:String)=Cache.open(Path.of(directory)).use {cache -> cache.list(maps).asSequence().mapTo(mutableSetOf()){it.id} }
        val addedMaps = mapIds(".data/cache/game") - mapIds(".data/cache/vanilla")
        val addedConfigs = setOf("2:200","2:201","2:202","2:204","2:205")
        assertEquals(addedConfigs.size+addedMaps.size, delta.summary.added)
        assertEquals(0, delta.summary.removed)
        // Motherlode Mine replaces the depleted default veins in its vanilla landscape.
        assertEquals(13, delta.summary.changed)
        assertEquals(116_003, delta.summary.unchanged)

        val additions =
            delta.changes.filter { it.kind == CacheDeltaKind.Added }.map { it.key }.toSet()
        assertEquals(addedConfigs+addedMaps.map {"$maps:$it"},additions)

        val changes =
            delta.changes.filter { it.kind == CacheDeltaKind.Changed }.map { it.key }.toSet()
        assertEquals(
            setOf(
                "2:2",
                "2:5",
                "2:6",
                "2:8",
                "2:9",
                "2:10",
                "2:11",
                "2:14",
                "2:16",
                "2:18",
                "2:38",
                "2:39",
                "5:4453",
            ),
            changes,
        )
    }
}
