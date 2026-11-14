package org.rsmod.api.cache.delta

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClientContractArchiveManifestExporterIntegrationTest {
    @Test
    fun `exports every selected client contract archive file`() {
        val manifest =
            ClientContractArchiveManifestExporter.export(
                cacheDirectory = Path.of(".data/cache/vanilla"),
                revision = "238-20260603",
                archive = 2565,
                capturedAt = "2026-06-03T13:45:06.049779Z",
            )

        val counts = manifest.entries.groupingBy(CacheManifestEntry::domain).eachCount()
        assertEquals(
            mapOf(
                "animation-bases" to 2_631,
                "animation-frames" to 203_223,
                "client-scripts" to 9_534,
                "interface-components" to 26_306,
                "jingles" to 314,
                "keyframe-sets" to 789,
                "midi-patches" to 179,
                "songs" to 861,
                "sound-effects" to 11_595,
                "vorbis-samples" to 566,
            ),
            counts,
        )
        assertEquals(
            manifest.entries.size,
            manifest.entries.map { it.domain to it.key }.toSet().size,
        )
        assertTrue(manifest.entries.all { it.fingerprint.matches(Regex("sha256:[0-9a-f]{64}")) })
        assertTrue(manifest.entries.all { it.symbol == null })
        assertTrue(manifest.entries.all { it.attributes["fileLength"]!!.toInt() >= 0 })
        assertTrue(manifest.entries.all { it.riskTags == it.riskTags.sorted() })

        val firstBase =
            manifest.entries.single { it.domain == "animation-bases" && it.key == "0:0" }
        assertEquals("1", firstBase.attributes["archiveId"])
        assertEquals("4154", firstBase.attributes["fileLength"])
        assertEquals(
            "sha256:0897c261e2367edfc680b1bef17cd3fc39c2029f2de1e1da59dec4b0247abdbb",
            firstBase.fingerprint,
        )
    }
}
