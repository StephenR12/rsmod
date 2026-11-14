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
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        assertEquals(114_198, manifest.entries.size)
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
                "0" to 10_676,
                "1" to 2_631,
                "2" to 40,
                "3" to 957,
                "4" to 11_533,
                "5" to 2_871,
                "6" to 861,
                "7" to 60_484,
                "8" to 8_357,
                "9" to 1,
                "10" to 4,
                "11" to 314,
                "12" to 9_534,
                "13" to 21,
                "14" to 566,
                "15" to 179,
                "17" to 2,
                "18" to 2_077,
                "19" to 54,
                "20" to 2_077,
                "21" to 156,
                "22" to 789,
                "24" to 14,
            ),
            archives,
        )
        assertEquals(
            712_944,
            manifest.entries.sumOf { it.attributes.getValue("fileCount").toInt() },
        )

        val lumbridge = manifest.entries.single { it.key == "5:12850" }
        assertEquals("2723771615", lumbridge.attributes["groupChecksum"])
        assertEquals("-1", lumbridge.attributes["groupNameHash"])
        assertEquals(
            "sha256:d26991146735b7e2fad3367227a3baab86b703e46cfe178c51b47d1e314040c3",
            lumbridge.fingerprint,
        )
    }

    @Test
    fun `rsmod enrichment changes only expected archive groups`() {
        val vanilla =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )
        val enriched =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/game"),
                revision = "238-20260603-rsmod-enriched",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
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
        assertEquals(114_185, delta.summary.unchanged)

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
                "5:14936",
            ),
            changes,
        )
    }
}
